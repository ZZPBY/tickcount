package io.github.zzpby.tickcount.domain

/** How Markdown writes a level-two heading, which is what opens a section. */
private const val SECTION_MARKER = "## "

/** The README the introduction is read from, and the sections that make it up. */
data class IntroDocument(val asset: String, val sections: List<String>)

/**
 * The document to read for [languageTag], which is the *app's* language rather than a
 * language the file is written in.
 *
 * The screen and its headings travel together on purpose. Looking them up separately
 * would eventually pair one file with the other file's headings, which yields an empty
 * introduction and no error whatsoever — the failure mode this whole arrangement is
 * most likely to have.
 */
fun introDocumentFor(languageTag: String): IntroDocument = if (languageTag.startsWith("zh")) {
    IntroDocument(
        asset = "README.md",
        sections = listOf("功能", "倒计时的读法", "桌面小组件"),
    )
} else {
    IntroDocument(
        asset = "README.en.md",
        sections = listOf("Features", "How the countdown reads", "Home-screen widget"),
    )
}

/**
 * Pulls the named level-two sections out of [markdown], in the order given by
 * [titles].
 *
 * Used to build the project introduction out of the repository's own README, so the
 * app and the file cannot drift apart — the same reasoning that has the changelog
 * copied into the build rather than retyped. The order comes from [titles] rather
 * than from the file, so the introduction can read in the order that suits a
 * first-time reader while the README stays ordered the way a repository wants.
 *
 * A title that is not there is skipped rather than failing: a section renamed in
 * the README should cost one paragraph, not the whole screen.
 */
fun readmeSections(markdown: String, titles: List<String>): String =
    titles
        .mapNotNull { title -> section(markdown, title) }
        .joinToString("\n\n")

/** One section, heading included, or null when [markdown] has no such heading. */
private fun section(markdown: String, title: String): String? {
    val lines = markdown.removePrefix("\uFEFF").lines()
    val heading = lines.indexOfFirst { it.trim() == SECTION_MARKER + title }
    if (heading < 0) return null

    val end = (heading + 1 until lines.size)
        .firstOrNull { lines[it].startsWith(SECTION_MARKER) }
        ?: lines.size

    val body = lines.subList(heading + 1, end).joinToString("\n").trim()
    return if (body.isEmpty()) null else "$SECTION_MARKER$title\n\n$body"
}
