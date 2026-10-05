package io.github.zzpby.tickcount.data

import io.github.zzpby.tickcount.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import javax.crypto.AEADBadTagException

/**
 * The backup file: what it says, and what it hides.
 *
 * The encryption is the part that has to be tested rather than read. GCM authenticates
 * as it decrypts, so the assertions that matter are the ones about a *wrong* password
 * failing loudly — an implementation that quietly returned rubbish would satisfy a
 * round-trip test and no user.
 */
class BackupTest {

    private val events = listOf(
        CountdownEvent(
            id = "a",
            date = LocalDate.of(2026, 10, 16),
            title = "评审",
            time = LocalTime.of(9, 30),
            tags = listOf("工作", "重要"),
            colorHue = 210f,
            createdAt = 1000L,
            pinned = true,
        ),
        CountdownEvent(id = "b", date = LocalDate.of(2027, 1, 1), title = "新年"),
    )

    private val settings = AppSettings(
        preset = ThemePreset.GREEN,
        customHue = 88f,
        sortOrder = SortOrder.ADDED,
        language = AppLanguage.ENGLISH,
    )

    private fun backup() = Backup(events, settings)

    // ------------------------------------------------------------------ the file

    @Test
    fun `a backup survives a round trip`() {
        val restored = BackupCodec.decode(BackupCodec.encode(backup(), exportedAt = 1L))

        assertEquals(events, restored.events)
    }

    @Test
    fun `the settings travel with it`() {
        val restored = BackupCodec.decode(BackupCodec.encode(backup(), exportedAt = 1L))

        assertEquals(settings, restored.settings)
    }

    @Test
    fun `encoding is repeatable when the timestamp is given`() {
        assertEquals(
            BackupCodec.encode(backup(), exportedAt = 7L),
            BackupCodec.encode(backup(), exportedAt = 7L),
        )
    }

    @Test
    fun `something that is not a backup is rejected`() {
        listOf("{}", """{"app":"something-else"}""", "[1,2,3]", "").forEach { text ->
            val failed = runCatching { BackupCodec.decode(text) }.isFailure
            assertTrue("accepted: $text", failed)
        }
    }

    @Test
    fun `a backup without settings takes the defaults`() {
        val bare = """{"app":"tickcount","format":1,"events":[]}"""

        assertEquals(AppSettings(), BackupCodec.decode(bare).settings)
    }

    @Test
    fun `a setting from a newer build falls back rather than failing`() {
        val future = """
            {"app":"tickcount","format":1,"events":[],
             "settings":{"theme":"MAGENTA","sort":"SIDEWAYS","language":"KLINGON"}}
        """.trimIndent()

        val restored = BackupCodec.decode(future)

        assertEquals(AppSettings().preset, restored.settings.preset)
        assertEquals(AppSettings().sortOrder, restored.settings.sortOrder)
        assertEquals(AppSettings().language, restored.settings.language)
    }

    @Test
    fun `records written before this version still load`() {
        // The oldest shape: a date, a name and a palette index, and nothing else.
        val legacy = """{"app":"tickcount","format":1,"events":[{"d":20338,"t":"旧数据","c":2}]}"""

        val event = BackupCodec.decode(legacy).events.single()

        assertEquals("旧数据", event.title)
        assertEquals(LocalDate.ofEpochDay(20338), event.date)
        assertEquals(144f, event.colorHue)
    }

    // -------------------------------------------------------------- the password

    @Test
    fun `an encrypted backup survives a round trip`() {
        val password = "correct horse battery staple".toCharArray()
        val sealed = BackupCrypto.encrypt(BackupCodec.encode(backup(), 1L), password)

        val restored = BackupCodec.decode(BackupCrypto.decrypt(sealed, password))

        assertEquals(events, restored.events)
        assertEquals(settings, restored.settings)
    }

    @Test
    fun `an encrypted file says so, and a plain one does not`() {
        val sealed = BackupCrypto.encrypt("{}", "pw".toCharArray())

        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertFalse(BackupCrypto.isEncrypted("""{"app":"tickcount"}"""))
    }

    @Test
    fun `the wrong password fails rather than returning rubbish`() {
        val sealed = BackupCrypto.encrypt("""{"secret":"数据"}""", "right".toCharArray())

        val failed = runCatching { BackupCrypto.decrypt(sealed, "wrong".toCharArray()) }.isFailure

        assertTrue("a wrong password was accepted", failed)
    }

    @Test
    fun `a wrong password fails with the exception the caller handles`() {
        val sealed = BackupCrypto.encrypt("{}", "right".toCharArray())

        val error = runCatching { BackupCrypto.decrypt(sealed, "wrong".toCharArray()) }
            .exceptionOrNull()

        assertTrue(
            "expected an authentication failure, got $error",
            error is AEADBadTagException,
        )
    }

    @Test
    fun `a tampered file is rejected`() {
        val sealed = BackupCrypto.encrypt("""{"a":1}""", "pw".toCharArray())
        val lines = sealed.trim().lines()
        val blob = lines[1].toCharArray()
        // Flip one character of the payload; GCM's tag has to notice.
        blob[blob.size / 2] = if (blob[blob.size / 2] == 'A') 'B' else 'A'
        val tampered = lines[0] + "\n" + String(blob) + "\n"

        assertTrue(
            "a tampered file was accepted",
            runCatching { BackupCrypto.decrypt(tampered, "pw".toCharArray()) }.isFailure,
        )
    }

    @Test
    fun `the same backup encrypted twice gives two different files`() {
        val password = "pw".toCharArray()

        val first = BackupCrypto.encrypt("""{"a":1}""", password)
        val second = BackupCrypto.encrypt("""{"a":1}""", password)

        // A fresh salt and nonce each time, so the files cannot be compared.
        assertNotEquals(first, second)
        assertEquals(
            BackupCrypto.decrypt(first, password),
            BackupCrypto.decrypt(second, password),
        )
    }

    @Test
    fun `the password is not in the file`() {
        val password = "hunter2".toCharArray()

        val sealed = BackupCrypto.encrypt("""{"title":"评审"}""", password)

        assertFalse(sealed.contains("hunter2"))
        assertFalse(sealed.contains("评审"))
    }

    // -------------------------------------------------------------- what it would do

    @Test
    fun `a file of new countdowns would add all of them`() {
        val preview = previewOf(backup(), existing = listOf(event("c")))

        assertEquals(2, preview.added)
        assertEquals(0, preview.overwritten)
        assertEquals(1, preview.discarded)
    }

    @Test
    fun `a countdown the device already holds counts as overwritten, not added`() {
        val preview = previewOf(backup(), existing = listOf(event("a"), event("c")))

        assertEquals(1, preview.added)
        assertEquals(1, preview.overwritten)
        assertEquals(2, preview.discarded)
    }

    @Test
    fun `importing into an empty app discards nothing and adds everything`() {
        val preview = previewOf(backup(), existing = emptyList())

        assertEquals(2, preview.added)
        assertEquals(0, preview.overwritten)
        assertEquals(0, preview.discarded)
    }

    @Test
    fun `a file of what is already here would only overwrite`() {
        val preview = previewOf(backup(), existing = listOf(event("a"), event("b")))

        assertEquals(0, preview.added)
        assertEquals(2, preview.overwritten)
        assertEquals(2, preview.discarded)
    }

    private fun event(id: String) =
        CountdownEvent(id = id, date = LocalDate.of(2026, 5, 1), title = "别的")
}
