package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.BuildConfig
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.introDocumentFor
import io.github.zzpby.tickcount.domain.readmeSections
import io.github.zzpby.tickcount.ui.components.AppIcon
import io.github.zzpby.tickcount.ui.components.MarkdownText
import io.github.zzpby.tickcount.ui.components.currentLocale
import io.github.zzpby.tickcount.ui.components.parseMarkdown
import java.io.FileNotFoundException

/**
 * What this app is, in the order it should be read: the icon, the name, then the
 * repository's own description of what it does.
 *
 * The body is the README rather than a copy of it, so a feature added there shows up here
 * without anyone remembering to write it twice.
 *
 * The repository link used to sit at the foot of this page. It is one address that does not
 * need the introduction around it, so it waits on the settings page under the row that opens
 * this one — reachable without reading the whole README first.
 */
@Composable
fun ProjectIntroScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = currentLocale()
    val intro = remember(locale) { introDocumentFor(locale.language) }
    val blocks = remember(context, intro) {
        runCatching {
            val markdown = context.assets.open(intro.asset)
                .bufferedReader()
                .use { it.readText() }
            parseMarkdown(readmeSections(markdown, intro.sections))
        }.getOrElse { error ->
            // A missing asset is a build problem, not a user problem: the screen still
            // has its heading and simply has no body.
            if (error !is FileNotFoundException) throw error
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        AppIcon(size = 104.dp)

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))
        MarkdownText(blocks = blocks)

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }
}
