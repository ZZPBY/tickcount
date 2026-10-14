package io.github.zzpby.tickcount.ui.components

import io.github.zzpby.tickcount.domain.introDocumentFor
import io.github.zzpby.tickcount.domain.readmeSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The project introduction, as the app will actually build it, in both languages.
 *
 * The other Markdown tests use made-up snippets, which cannot catch a document shaped in
 * a way the parser mishandles. These parse the repository's own READMEs through the same
 * two steps the app does, so a wrap or a stray blank line that only shows up on screen
 * fails here instead.
 *
 * The unit tests run with the module directory as the working directory, but the
 * repository root is accepted too so the test does not depend on which.
 */
class ReadmeRenderingTest {

    private val root = listOf(File("."), File(".."))
        .firstOrNull { File(it, "README.md").isFile }
        ?: error("no README.md found from ${File(".").absolutePath}")

    /** The languages the introduction can be shown in. */
    private val tags = listOf("zh", "en")

    /**
     * Where the countdown line's own section sits in `introDocumentFor`'s list.
     *
     * Named here because it is the only section either document gives a table to, and the
     * test below is about the table. A section inserted above it moves this number, which is
     * the one thing about this test that is coupled to the order in [introDocumentFor] — the
     * headings themselves differ by language and cannot be matched by name from here.
     */
    private val countdownSection = 2

    private fun document(tag: String) = introDocumentFor(tag)

    private fun file(tag: String) = File(root, document(tag).asset)

    private fun blocksFor(tag: String) =
        parseMarkdown(readmeSections(file(tag).readText(), document(tag).sections))

    // ------------------------------------------------------------------ the files

    @Test
    fun `each language reads a file that exists`() {
        tags.forEach { tag ->
            assertTrue("${document(tag).asset} is missing", file(tag).isFile)
        }
    }

    @Test
    fun `every section heading is in the file it is looked up in`() {
        // A heading that does not match costs the whole section and reports nothing at
        // all, which is the one failure this arrangement cannot notice by itself.
        tags.forEach { tag ->
            val text = file(tag).readText()
            document(tag).sections.forEach { section ->
                assertTrue(
                    "${document(tag).asset} has no '## $section'",
                    text.contains("## $section"),
                )
            }
        }
    }

    @Test
    fun `each document yields headings, bullets and a paragraph`() {
        tags.forEach { tag ->
            val blocks = blocksFor(tag)
            assertTrue("$tag: no heading came through", blocks.any { it is MarkdownBlock.Heading })
            assertTrue("$tag: no bullets came through", blocks.any { it is MarkdownBlock.Bullet })
            assertTrue(
                "$tag: no paragraph came through",
                blocks.any { it is MarkdownBlock.Paragraph },
            )
        }
    }

    @Test
    fun `the table comes through in both languages`() {
        tags.forEach { tag ->
            // Read from the countdown section by position rather than by heading: the
            // headings differ between the two languages, so a name that matched one of them
            // would silently select nothing in the other — which is how this test first
            // broke when a section was added above the table.
            val reading = readmeSections(
                file(tag).readText(),
                listOf(document(tag).sections[countdownSection]),
            )
            assertTrue(
                "$tag: the table is missing",
                parseMarkdown(reading).any { it is MarkdownBlock.Table },
            )
        }
    }

    // ---------------------------------------------------------------- the bullets

    @Test
    fun `every feature is one bullet, whatever the source wraps`() {
        tags.forEach { tag ->
            // The first section is the feature list, and it is bullets only.
            val section = readmeSections(file(tag).readText(), listOf(document(tag).sections[0]))
            val blocks = parseMarkdown(section)
            val sourceBullets = section.lines().count { it.startsWith("- ") }

            assertEquals(
                "$tag: every '- ' line should be exactly one bullet",
                sourceBullets,
                blocks.filterIsInstance<MarkdownBlock.Bullet>().size,
            )
            assertEquals(
                "$tag: the feature list should hold bullets under its heading and nothing else",
                emptyList<MarkdownBlock>(),
                blocks.filterNot { it is MarkdownBlock.Bullet || it is MarkdownBlock.Heading },
            )
        }
    }

    @Test
    fun `no bullet carries a line break or a doubled space`() {
        tags.forEach { tag ->
            blocksFor(tag).filterIsInstance<MarkdownBlock.Bullet>().forEach { block ->
                assertTrue("$tag: bullet has a newline: ${block.text}", !block.text.contains('\n'))
                assertTrue("$tag: doubled space: ${block.text}", !block.text.contains("  "))
                assertTrue("$tag: leading space: ${block.text}", block.text == block.text.trim())
            }
        }
    }
}
