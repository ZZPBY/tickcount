package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.BuildConfig
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.introDocumentFor
import io.github.zzpby.tickcount.domain.readmeSections
import io.github.zzpby.tickcount.ui.components.AppIcon
import io.github.zzpby.tickcount.ui.components.MarkdownText
import io.github.zzpby.tickcount.ui.components.currentLocale
import io.github.zzpby.tickcount.ui.components.parseMarkdown
import kotlinx.coroutines.delay
import java.io.FileNotFoundException

/**
 * What this app is, in the order it should be read: the icon, the name, then the
 * repository's own description of what it does, and the source at the end as the one
 * thing on the page that goes anywhere.
 *
 * The body is the README rather than a copy of it, so a feature added there shows up here
 * without anyone remembering to write it twice.
 */
@Composable
fun ProjectIntroScreen(onOpenLink: () -> Unit, modifier: Modifier = Modifier) {
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
            // has its heading and its link, and simply has no body.
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

        Spacer(Modifier.height(16.dp))
        GithubCard(onOpen = onOpenLink)

        Spacer(Modifier.height(36.dp))
    }
}

/**
 * Where the project lives: the mark, what it is, the address, and the way out.
 *
 * The address copies rather than opens, and the button beside it opens rather than
 * copies — the two things someone might want from a repository link, each given its
 * own target instead of one control guessing. The confirmation replaces the address
 * in place for a moment, because a snackbar would need a host this screen has none
 * of, and the text it replaces is the thing that was just copied anyway.
 */
@Composable
private fun GithubCard(onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shaping = RoundedCornerShape(18.dp)
    val clipboard = LocalClipboardManager.current
    val url = stringResource(R.string.about_link_url)

    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shaping)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant, shaping)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            tint = scheme.onSurface,
            modifier = Modifier.size(34.dp),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.intro_github_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (copied) {
                    stringResource(R.string.intro_copied)
                } else {
                    url
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (copied) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(url))
                        copied = true
                    }
                    .padding(vertical = 3.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Button(
            onClick = onOpen,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.intro_go))
        }
    }
}
