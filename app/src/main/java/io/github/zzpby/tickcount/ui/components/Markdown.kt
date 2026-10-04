package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** One block of a parsed Markdown document. */
sealed interface MarkdownBlock {
    data class Heading(val text: String, val level: Int) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
}

/**
 * Splits Markdown into the handful of blocks the README actually uses.
 *
 * A deliberately small subset — headings, bullets, paragraphs, tables — because
 * this exists to render one specific document, and a full Markdown engine is a
 * dependency and a page of edge cases for features that document does not contain.
 * Anything unrecognised falls through to a paragraph rather than being dropped.
 */
fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val bullet = mutableListOf<String>()
    val table = mutableListOf<List<String>>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(joinWrapped(paragraph))
            paragraph.clear()
        }
    }

    fun flushBullet() {
        if (bullet.isNotEmpty()) {
            blocks += MarkdownBlock.Bullet(joinWrapped(bullet))
            bullet.clear()
        }
    }

    fun flushTable() {
        if (table.isNotEmpty()) {
            blocks += MarkdownBlock.Table(table.toList())
            table.clear()
        }
    }

    fun flushAll() {
        flushParagraph()
        flushBullet()
        flushTable()
    }

    source.removePrefix("\uFEFF").lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> flushAll()

            line.startsWith("|") -> {
                flushParagraph()
                flushBullet()
                if (!isTableRule(line)) table += tableCells(line)
            }

            line.startsWith("### ") -> {
                flushAll()
                blocks += MarkdownBlock.Heading(line.removePrefix("### ").trim(), level = 3)
            }

            line.startsWith("## ") -> {
                flushAll()
                blocks += MarkdownBlock.Heading(line.removePrefix("## ").trim(), level = 2)
            }

            line.startsWith("- ") -> {
                flushParagraph()
                flushBullet()
                flushTable()
                bullet += line.removePrefix("- ").trim()
            }

            // A line that is neither blank nor a marker continues whatever block is
            // open. For a bullet that is the bullet: Markdown wraps long items across
            // lines, and treating the wrap as a new paragraph splits one item into
            // two blocks with a gap between them.
            bullet.isNotEmpty() -> bullet += line.trim()

            else -> {
                flushTable()
                paragraph += line.trim()
            }
        }
    }
    flushAll()

    return blocks
}

/** True for the `|---|---|` row that separates a table's header from its body. */
private fun isTableRule(line: String) =
    line.trim().matches(Regex("""\|[\s:|-]+\|"""))

private fun tableCells(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

/**
 * Joins the source's hard-wrapped lines back into one.
 *
 * Markdown wraps a paragraph wherever it runs out of column, which is mid-sentence.
 * A space is right between two Latin words and wrong between two Chinese ones, so
 * the seam is only given a space when neither side is CJK.
 */
private fun joinWrapped(lines: List<String>): String =
    lines.fold("") { joined, line ->
        when {
            joined.isEmpty() -> line
            endsCjk(joined) || startsCjk(line) -> joined + line
            else -> "$joined $line"
        }
    }

private fun endsCjk(text: String) = text.isNotEmpty() && isCjk(text.last())

private fun startsCjk(text: String) = text.isNotEmpty() && isCjk(text.first())

private fun isCjk(character: Char): Boolean {
    val code = character.code
    return code in 0x3000..0x303F || // CJK punctuation
        code in 0x4E00..0x9FFF || // unified ideographs
        code in 0xFF00..0xFFEF // fullwidth forms
}

/** Renders what [parseMarkdown] produced. */
@Composable
fun MarkdownText(blocks: List<MarkdownBlock>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Spacer(Modifier.height(if (block.level == 2) 18.dp else 12.dp))
                    Text(
                        text = inline(block.text),
                        style = if (block.level == 2) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                }

                is MarkdownBlock.Paragraph -> {
                    Text(text = inline(block.text), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                is MarkdownBlock.Bullet -> {
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(text = "·", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(text = inline(block.text), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is MarkdownBlock.Table -> {
                    MarkdownTable(block.rows)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerHigh),
    ) {
        rows.forEachIndexed { index, cells ->
            if (index > 0) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cells.forEach { cell ->
                    Text(
                        text = inline(cell),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * The inline half of the subset: bold, code and links.
 *
 * A link keeps its label and loses its target. Nothing inside the introduction is
 * meant to be tapped — the repository link at the foot of the screen is the one
 * thing that leaves the app, and it is a row of its own.
 */
@Composable
private fun inline(text: String): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    val codeBackground = scheme.surfaceContainerHigh
    val linkColor = scheme.primary

    return remember(text, codeBackground, linkColor) {
        buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                when {
                    text.startsWith("**", index) -> {
                        val end = text.indexOf("**", index + 2)
                        if (end < 0) {
                            append(text[index]); index++
                        } else {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(index + 2, end))
                            }
                            index = end + 2
                        }
                    }

                    text[index] == '`' -> {
                        val end = text.indexOf('`', index + 1)
                        if (end < 0) {
                            append(text[index]); index++
                        } else {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBackground,
                                )
                            ) { append(text.substring(index + 1, end)) }
                            index = end + 1
                        }
                    }

                    text[index] == '[' -> {
                        val labelEnd = text.indexOf(']', index)
                        val targetEnd = if (labelEnd >= 0 && labelEnd + 1 < text.length &&
                            text[labelEnd + 1] == '('
                        ) {
                            text.indexOf(')', labelEnd)
                        } else {
                            -1
                        }
                        if (labelEnd < 0 || targetEnd < 0) {
                            append(text[index]); index++
                        } else {
                            withStyle(SpanStyle(color = linkColor)) {
                                append(text.substring(index + 1, labelEnd))
                            }
                            index = targetEnd + 1
                        }
                    }

                    else -> {
                        append(text[index]); index++
                    }
                }
            }
        }
    }
}
