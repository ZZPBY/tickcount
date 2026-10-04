package io.github.zzpby.tickcount.domain

/** One release in the changelog: its version, and the lines that belong to it. */
data class ChangelogRelease(val version: String, val lines: List<String>)

/**
 * Splits the changelog into releases.
 *
 * The file is very nearly Markdown — `更新日志` over `===` and each version over `---` is
 * exactly a setext heading — but its entries sit on consecutive lines with no blank line
 * between them, and a Markdown parser joins consecutive lines into one paragraph, because
 * that is what makes the README's hard-wrapped prose read correctly. Rendering it as
 * Markdown would therefore run a whole version's notes together into a single block.
 *
 * So the rules are read here instead: a line underlined with dashes opens a version, a
 * line underlined with equals is the document's own title and is dropped, and everything
 * else belongs to the version above it.
 */
fun parseChangelog(text: String): List<ChangelogRelease> {
    val lines = text.removePrefix("\uFEFF").lines().map { it.trimEnd() }
    val releases = mutableListOf<ChangelogRelease>()
    var version: String? = null
    var body = mutableListOf<String>()

    fun closeRelease() {
        val current = version ?: return
        releases += ChangelogRelease(current, body.toList())
        body = mutableListOf()
    }

    lines.forEachIndexed { index, raw ->
        val line = raw.trim()
        val underline = lines.getOrNull(index + 1)?.trim().orEmpty()

        when {
            line.isEmpty() -> Unit

            // The rule under a heading, consumed by the heading above it.
            underline.isNotEmpty() && underline.all { it == '-' } -> {
                closeRelease()
                version = line
            }

            underline.isNotEmpty() && underline.all { it == '=' } -> Unit

            // The rule under a heading, consumed by the heading above it. Checked after
            // the two above, because a rule's own underline is whatever follows it.
            line.all { it == '-' || it == '=' } -> Unit

            else -> if (version != null) body += line
        }
    }
    closeRelease()

    return releases
}
