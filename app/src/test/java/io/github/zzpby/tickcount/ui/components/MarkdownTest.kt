package io.github.zzpby.tickcount.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Markdown subset the introduction is written in.
 *
 * The block boundaries are the part that matters: a table that swallows the
 * paragraph after it, or a wrapped Chinese paragraph that comes out with spaces
 * wedged between the characters, are both wrong in ways that are obvious on screen
 * and invisible in the source.
 */
class MarkdownTest {

    @Test
    fun `headings record their level`() {
        val blocks = parseMarkdown("## 功能\n\n### 小标题\n")

        assertEquals(
            listOf(
                MarkdownBlock.Heading("功能", 2),
                MarkdownBlock.Heading("小标题", 3),
            ),
            blocks,
        )
    }

    @Test
    fun `bullets become one block each`() {
        val blocks = parseMarkdown("- 第一项\n- 第二项\n")

        assertEquals(
            listOf(MarkdownBlock.Bullet("第一项"), MarkdownBlock.Bullet("第二项")),
            blocks,
        )
    }

    @Test
    fun `a wrapped bullet is one bullet, not a bullet and a paragraph`() {
        // Markdown wraps a long item wherever it runs out of column. Reading the
        // continuation as a new block splits one item into two with a gap between
        // them, which on screen looks like a stray line break.
        val blocks = parseMarkdown("- **排序** —— 可以按**升序或\n  降序**排列，选择会记住。\n")

        assertEquals(1, blocks.size)
        assertEquals(
            MarkdownBlock.Bullet("**排序** —— 可以按**升序或降序**排列，选择会记住。"),
            blocks.single(),
        )
    }

    @Test
    fun `two wrapped bullets stay two bullets`() {
        val blocks = parseMarkdown("- 第一条\n  接着写\n- 第二条\n  也接着写\n")

        assertEquals(
            listOf(MarkdownBlock.Bullet("第一条接着写"), MarkdownBlock.Bullet("第二条也接着写")),
            blocks,
        )
    }

    @Test
    fun `a wrapped bullet stops at the next heading`() {
        val blocks = parseMarkdown("- 一项\n  续行\n\n## 下一节\n\n正文。\n")

        assertEquals(3, blocks.size)
        assertEquals(MarkdownBlock.Bullet("一项续行"), blocks[0])
        assertEquals(MarkdownBlock.Heading("下一节", 2), blocks[1])
        assertEquals(MarkdownBlock.Paragraph("正文。"), blocks[2])
    }

    @Test
    fun `a wrapped Chinese paragraph is rejoined without spaces`() {
        val blocks = parseMarkdown("目标是**所选日期当地时间的 00:00**。这个时刻过去之后，\n标签从「还有」变成「已过去」。\n")

        assertEquals(1, blocks.size)
        val text = (blocks.single() as MarkdownBlock.Paragraph).text
        assertEquals("目标是**所选日期当地时间的 00:00**。这个时刻过去之后，标签从「还有」变成「已过去」。", text)
    }

    @Test
    fun `a wrapped English paragraph is rejoined with spaces`() {
        val blocks = parseMarkdown("The first half of a sentence\nand its second half.\n")

        assertEquals(
            MarkdownBlock.Paragraph("The first half of a sentence and its second half."),
            blocks.single(),
        )
    }

    @Test
    fun `a table keeps its header row and drops the rule beneath it`() {
        val blocks = parseMarkdown(
            """
            | 剩余时间 | 显示 |
            |---|---|
            | 2 天 30 秒 | 02日 |
            """.trimIndent()
        )

        val table = blocks.single() as MarkdownBlock.Table
        assertEquals(2, table.rows.size)
        assertEquals(listOf("剩余时间", "显示"), table.rows[0])
        assertEquals(listOf("2 天 30 秒", "02日"), table.rows[1])
    }

    @Test
    fun `a table does not swallow what follows it`() {
        val blocks = parseMarkdown(
            """
            | a | b |
            |---|---|
            | 1 | 2 |

            表格后面的一段话。
            """.trimIndent()
        )

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Table)
        assertEquals(MarkdownBlock.Paragraph("表格后面的一段话。"), blocks[1])
    }

    @Test
    fun `two tables in a row stay two tables`() {
        val blocks = parseMarkdown("| a |\n|---|\n| 1 |\n\n| b |\n|---|\n| 2 |\n")

        assertEquals(2, blocks.size)
        assertEquals(listOf(listOf("a"), listOf("1")), (blocks[0] as MarkdownBlock.Table).rows)
        assertEquals(listOf(listOf("b"), listOf("2")), (blocks[1] as MarkdownBlock.Table).rows)
    }

    @Test
    fun `a trailing blank line does not become a paragraph`() {
        assertEquals(emptyList<MarkdownBlock>(), parseMarkdown("\n\n"))
    }

    @Test
    fun `text with no markers is one paragraph`() {
        assertEquals(
            MarkdownBlock.Paragraph("普通一段话。"),
            parseMarkdown("普通一段话。").single(),
        )
    }
}
