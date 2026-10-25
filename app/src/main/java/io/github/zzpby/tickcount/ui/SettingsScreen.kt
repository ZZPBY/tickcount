package io.github.zzpby.tickcount.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppLanguage
import io.github.zzpby.tickcount.ui.components.ArchiveIcon
import io.github.zzpby.tickcount.ui.components.BellIcon
import io.github.zzpby.tickcount.ui.components.ChevronIcon
import io.github.zzpby.tickcount.ui.components.DocIcon
import io.github.zzpby.tickcount.ui.components.GlobeIcon
import io.github.zzpby.tickcount.ui.components.GridIcon
import io.github.zzpby.tickcount.ui.components.InfoIcon
import io.github.zzpby.tickcount.ui.components.PaletteIcon
import io.github.zzpby.tickcount.ui.theme.CardShape
import io.github.zzpby.tickcount.ui.theme.Gutter
import io.github.zzpby.tickcount.ui.theme.hairline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What used to be the drawer, as a screen of its own.
 *
 * Every entry here opens something rather than changing a value in place, with two
 * exceptions: the language, which is one choice and so stays a choice at the call site, and
 * the two rows that are pages of text. All of them are drawn the same weight, because at this
 * depth they are all "one more thing about the app".
 *
 * The rows are grouped by what they are about rather than by how often they are used: what
 * the app does for you, how it behaves, and what it has to say about itself. The gaps between
 * the cards are the only thing saying so, so a flat list would leave the reader guessing.
 *
 * The repository link sits under the group that introduces the project rather than at the foot
 * of that page: the introduction is worth reading once, the address is what people come back
 * for, and this is the screen they come back to.
 */
@Composable
fun SettingsScreen(
    language: AppLanguage,
    onOpen: (AppScreen) -> Unit,
    onLanguage: () -> Unit,
    onOpenLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
        )

        SettingsGroup(label = stringResource(R.string.settings_group_more)) {
            SettingsRow(
                label = stringResource(R.string.settings_notifications),
                onClick = { onOpen(AppScreen.REMINDERS) },
            ) { tint -> BellIcon(color = tint) }
            SettingsDivider()
            SettingsRow(
                label = stringResource(R.string.settings_appearance),
                onClick = { onOpen(AppScreen.APPEARANCE) },
            ) { tint -> PaletteIcon(color = tint) }
            SettingsDivider()
            SettingsRow(
                label = stringResource(R.string.settings_widgets),
                onClick = { onOpen(AppScreen.WIDGETS) },
            ) { tint -> GridIcon(size = 20.dp, color = tint) }
        }

        SettingsGroup(label = stringResource(R.string.settings_group_general)) {
            SettingsRow(
                label = stringResource(R.string.settings_language),
                onClick = onLanguage,
                trailing = stringResource(language.labelRes),
            ) { tint -> GlobeIcon(color = tint) }
            SettingsDivider()
            SettingsRow(
                label = stringResource(R.string.settings_data),
                onClick = { onOpen(AppScreen.DATA) },
            ) { tint -> ArchiveIcon(color = tint) }
        }

        SettingsGroup(label = stringResource(R.string.settings_group_about)) {
            SettingsRow(
                label = stringResource(R.string.settings_changelog),
                onClick = { onOpen(AppScreen.CHANGELOG) },
            ) { tint -> DocIcon(color = tint) }
            SettingsDivider()
            SettingsRow(
                label = stringResource(R.string.settings_project_intro),
                onClick = { onOpen(AppScreen.INTRO) },
            ) { tint -> InfoIcon(color = tint) }
        }

        Spacer(Modifier.height(10.dp))
        ProjectLinkCard(
            onOpen = onOpenLink,
            modifier = Modifier.padding(horizontal = Gutter),
        )

        Spacer(Modifier.height(24.dp))
    }
}

/** One card of rows, with the name of what they have in common above it. */
@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Gutter + 4.dp, top = 18.dp, bottom = 8.dp),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Gutter)
            .hairline(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        // The rows are stacked here rather than handed to the Surface: its content slot
        // lays its children out on top of one another.
        Column { content() }
    }
}

/** The gap between two rows, held clear of the icons so the column of text stays unbroken. */
@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * One destination: a mark, its name, and — when the row is a value rather than a place — what
 * that value currently is.
 */
@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    icon: @Composable (Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // A floor rather than a fixed height: at a large system font the row has to grow
            // with its text instead of clipping it.
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            icon(scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        ChevronIcon(
            pointsLeft = false,
            size = 16.dp,
            color = scheme.onSurfaceVariant,
        )
    }
}

/**
 * Where the project lives: the mark, what it is, the address, and the way out.
 *
 * The address copies rather than opens, and the button beside it opens rather than copies —
 * the two things someone might want from a repository link, each given its own target
 * instead of one control guessing. The confirmation replaces the address in place for a
 * moment, because a snackbar would need a host this screen has none of, and the text it
 * replaces is the thing that was just copied anyway.
 *
 * It used to sit at the foot of the introduction, which meant scrolling through the whole
 * README to reach the one thing on that page that goes anywhere.
 */
@Composable
private fun ProjectLinkCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shaping = RoundedCornerShape(18.dp)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val url = stringResource(R.string.about_link_url)

    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Row(
        modifier = modifier
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
                        // The clipboard is written through a coroutine because the current
                        // API is suspending. The label is null because that is what the
                        // deprecated one-liner this replaced put on the clip — a label is
                        // only ever shown by clipboard inspectors, and changing it here
                        // would be a difference nobody asked for.
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, url)))
                        }
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

/** The name each language is shown under, in that language rather than the current one. */
internal val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.CHINESE -> R.string.language_chinese
        AppLanguage.ENGLISH -> R.string.language_english
    }
