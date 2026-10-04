package io.github.zzpby.tickcount.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Reading the changelog, on the repository's own file.
 *
 * The made-up cases pin the shape down; the last test runs the real file, because the
 * format is written by hand and an entry that starts looking like a heading would
 * otherwise be swallowed silently.
 */
class ChangelogTest {

    private val sample = """
        更新日志
        ========================================

        1.0.5
        ----------------------------------------

        本版变化

        ✨ 侧边栏导航：左上角的菜单按钮打开侧边栏
        ✨ 搜索：列表上方一个搜索框
        🔧 仍然不要任何权限

        📦 安装：下载 TickCount-1.0.5.apk

        1.0.4
        ----------------------------------------

        ✨ 桌面小组件
    """.trimIndent()

    @Test
    fun `the document's own title is not a release`() {
        assertEquals(listOf("1.0.5", "1.0.4"), parseChangelog(sample).map { it.version })
    }

    @Test
    fun `each version keeps the lines that follow it`() {
        val newest = parseChangelog(sample).first()

        assertEquals(
            listOf(
                "本版变化",
                "✨ 侧边栏导航：左上角的菜单按钮打开侧边栏",
                "✨ 搜索：列表上方一个搜索框",
                "🔧 仍然不要任何权限",
                "📦 安装：下载 TickCount-1.0.5.apk",
            ),
            newest.lines,
        )
    }

    @Test
    fun `entries on consecutive lines stay separate`() {
        // This is the whole reason the file is not read as Markdown: consecutive lines
        // there are one paragraph, and every release would render as a single block.
        val newest = parseChangelog(sample).first()

        assertTrue("entries were run together", newest.lines.size > 3)
        assertTrue(newest.lines.none { it.contains('\n') })
    }

    @Test
    fun `blank lines and rules are not entries`() {
        val newest = parseChangelog(sample).first()

        assertTrue(newest.lines.none { it.isBlank() })
        assertTrue(newest.lines.none { it.all { character -> character == '-' } })
    }

    @Test
    fun `a file with no versions yields nothing`() {
        assertEquals(emptyList<ChangelogRelease>(), parseChangelog(""))
        assertEquals(emptyList<ChangelogRelease>(), parseChangelog("更新日志\n====\n\n随笔。\n"))
    }

    @Test
    fun `the repository's own changelog parses`() {
        val file = listOf(File("更新日志.txt"), File("../更新日志.txt"))
            .firstOrNull { it.isFile }
            ?: error("更新日志.txt not found from ${File(".").absolutePath}")

        val releases = parseChangelog(file.readText())

        assertTrue("no releases came out", releases.size >= 4)
        assertEquals("1.0.5", releases.first().version)
        assertTrue("the newest release has no entries", releases.first().lines.size > 5)
    }
}
