package io.github.zzpby.tickcount.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.zzpby.tickcount.R

/**
 * The asset this dialog reads. The `copyChangelog` Gradle task puts the
 * repository-root changelog into the APK under exactly this name, so the dialog
 * and the file cannot disagree. Written with \u escapes to keep the literal out
 * of reach of any source-encoding mishap: a mis-decoded name would not fail
 * loudly, it would just look like a missing changelog.
 */
private const val CHANGELOG_ASSET = "\u66F4\u65B0\u65E5\u5FD7.txt"

/**
 * Share of the height the dialog hands its text area. A fraction of that box
 * rather than of the screen: it stays right in landscape, in split screen, and
 * under any inset, where a screen measurement would not.
 */
private const val TEXT_HEIGHT_FRACTION = 0.6f

/**
 * The changelog, read from the file that ships inside the APK.
 *
 * Deliberately taller than [AboutDialog] and fixed at that height: a changelog
 * only grows, so once the text outruns the box it scrolls rather than pushing the
 * dialog further down the screen.
 */
@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val changelog = remember { readChangelog(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_changelog)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(TEXT_HEIGHT_FRACTION)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = changelog.ifBlank { stringResource(R.string.changelog_missing) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * Reads the changelog out of the APK's assets.
 *
 * Unreadable is not worth crashing over — the dialog says so and carries on.
 */
private fun readChangelog(context: Context): String =
    runCatching {
        context.assets.open(CHANGELOG_ASSET).bufferedReader().use { it.readText() }
    }.getOrNull()
        // The file carries a UTF-8 BOM for the benefit of Windows text tools, and
        // it would otherwise render as a stray glyph ahead of the first heading.
        ?.removePrefix("\uFEFF")
        .orEmpty()
