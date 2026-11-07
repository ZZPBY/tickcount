package io.github.zzpby.tickcount.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What a backup leaves out.
 *
 * The countdowns are the user's and travel; a widget's binding is the launcher's, keyed by
 * an id that only means something on the device that handed it out. Restoring one is at
 * best a pointer at a countdown that is not there, and at worst a value written by a
 * release that kept a date where an id is read now.
 *
 * The rules are matched loosely — attribute order and spacing are not this test's business
 * — so that reflowing the XML does not fail it for the wrong reason.
 */
class BackupRulesTest {

    private val widgetBinding = Regex("""<exclude[^>]*tickcount_widgets\.xml""")
    private val everything = Regex("""<include[^>]*path="\.""")

    private fun rules(name: String): String {
        val file = listOf(File("src/main/res/xml/$name"), File("app/src/main/res/xml/$name"))
            .firstOrNull { it.isFile }
            ?: error("$name not found from ${File(".").absolutePath}")
        return file.readText()
    }

    @Test
    fun `the countdowns are still backed up`() {
        assertTrue(
            "backup_rules.xml no longer includes the preferences",
            everything.containsMatchIn(rules("backup_rules.xml")),
        )
    }

    @Test
    fun `the widget bindings are left out of a backup`() {
        assertTrue(
            "backup_rules.xml does not exclude tickcount_widgets.xml",
            widgetBinding.containsMatchIn(rules("backup_rules.xml")),
        )
    }

    @Test
    fun `they are left out of both halves of a device transfer`() {
        // Both `<cloud-backup>` and `<device-transfer>` include the preferences, so both
        // have to exclude the bindings; excluding one of them is how half the file would
        // come back on a new phone without the other.
        assertEquals(
            2,
            widgetBinding.findAll(rules("data_extraction_rules.xml")).count(),
        )
    }
}
