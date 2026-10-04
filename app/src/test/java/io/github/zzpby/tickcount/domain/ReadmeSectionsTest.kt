package io.github.zzpby.tickcount.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pulling the introduction out of the README.
 *
 * The parts worth pinning down are the boundaries: a section runs to the next
 * level-two heading and no further, and a heading that is not there costs one
 * paragraph rather than the whole screen.
 */
class ReadmeSectionsTest {

    private val readme = """
        # TickCount

        Some preamble that is not part of any section.

        ## 功能

        - **倒数到任意一天** —— 点日历上的一天。
        - **桌面小组件** —— 把某一天放到桌面上。

        ## 倒计时的读法

        目标是**所选日期当地时间的 00:00**。

        | 剩余时间 | 显示 |
        |---|---|
        | 2 天 30 秒 | `----年--月02日` |

        ## 桌面小组件

        长按桌面空白处 → 添加小组件。

        ## 下载

        这一段不该出现。
    """.trimIndent()

    @Test
    fun `picks the requested sections and nothing else`() {
        val text = readmeSections(readme, listOf("功能", "桌面小组件"))

        assertTrue(text.contains("倒数到任意一天"))
        assertTrue(text.contains("长按桌面空白处"))
        assertFalse(text.contains("这一段不该出现"))
        assertFalse(text.contains("剩余时间"))
    }

    @Test
    fun `a section stops at the next level-two heading`() {
        val text = readmeSections(readme, listOf("倒计时的读法"))

        assertTrue(text.contains("剩余时间"))
        assertFalse(text.contains("长按桌面空白处"))
    }

    @Test
    fun `sections come out in the order asked for, not the order in the file`() {
        val text = readmeSections(readme, listOf("桌面小组件", "功能"))

        val first = text.indexOf("长按桌面空白处")
        val second = text.indexOf("倒数到任意一天")
        assertTrue("expected the widget section first", first in 1 until second)
    }

    @Test
    fun `each chosen section keeps its heading`() {
        val text = readmeSections(readme, listOf("功能"))

        assertTrue(text.startsWith("## 功能"))
    }

    @Test
    fun `a heading that is not there is skipped, not fatal`() {
        val text = readmeSections(readme, listOf("不存在的章节", "功能"))

        assertTrue(text.contains("倒数到任意一天"))
        assertFalse(text.contains("不存在的章节"))
    }

    @Test
    fun `no headings at all yields an empty string`() {
        assertEquals("", readmeSections(readme, emptyList()))
        assertEquals("", readmeSections("# 空文档\n", listOf("功能")))
    }

    @Test
    fun `a leading byte order mark does not hide the first heading`() {
        // A Windows editor will happily write one, and it would otherwise ride along
        // on the first line and stop `## 功能` from ever matching.
        val text = readmeSections("\uFEFF$readme", listOf("功能"))

        assertTrue(text.contains("倒数到任意一天"))
    }
}
