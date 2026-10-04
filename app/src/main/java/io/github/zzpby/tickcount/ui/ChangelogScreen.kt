package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.BuildConfig
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.ChangelogRelease
import io.github.zzpby.tickcount.domain.parseChangelog
import java.io.FileNotFoundException

/** The changelog as shipped in the APK's assets. */
private const val CHANGELOG_ASSET = "\u66F4\u65B0\u65E5\u5FD7.txt"

/**
 * What changed in each version, newest first.
 *
 * The text is the repository's own `更新日志.txt`, so the screen and the release notes on
 * the project page cannot disagree. The version matching the one installed is marked,
 * because a changelog read from inside the app is nearly always read to find out what the
 * current build brought.
 */
@Composable
fun ChangelogScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val releases = remember(context) {
        runCatching {
            val text = context.assets.open(CHANGELOG_ASSET)
                .bufferedReader()
                .use { it.readText() }
            parseChangelog(text)
        }.getOrElse { error ->
            // A missing asset is a build problem, not a user problem.
            if (error !is FileNotFoundException) throw error
            emptyList()
        }
    }

    if (releases.isEmpty()) {
        CentredNotice(stringResource(R.string.changelog_missing))
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        releases.forEachIndexed { index, release ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Release(release)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Release(release: ChangelogRelease) {
    val installed = release.version == BuildConfig.VERSION_NAME

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = release.version,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (installed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (installed) {
            Text(
                text = stringResource(R.string.changelog_installed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(10.dp))

        // One line per entry, with no joining: the file puts each note on its own line
        // and they are separate things, not a wrapped paragraph.
        release.lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}
