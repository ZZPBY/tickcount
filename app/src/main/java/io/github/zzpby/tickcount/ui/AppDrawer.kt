package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppLanguage

/**
 * The destinations the app has, all of them offered by the drawer.
 *
 * There is no Settings screen any more: with two entries under it, it was a level of
 * nesting that cost a tap and explained nothing.
 *
 * A single countdown's own screen is not a destination at all but a thing opened from
 * a list, so it is not in here.
 */
enum class AppScreen(val labelRes: Int) {
    HOME(R.string.drawer_home),
    CALENDAR(R.string.drawer_calendar),
    APPEARANCE(R.string.settings_appearance),
    CHANGELOG(R.string.drawer_changelog),
    INTRO(R.string.settings_project_intro),
}

/**
 * The panel that slides in from the left.
 *
 * Hand-rolled rather than built on `ModalNavigationDrawer`, which clamps its sheet
 * to between 240dp and 360dp wide: on a phone that is nowhere near the half-screen
 * width this one is specified to take. Laying it out directly also keeps it a plain
 * child of the screen rather than a window of its own.
 *
 * The language sits between Appearance and the changelog rather than being a screen of its
 * own: it is one choice, and a screen for it would be a tap and a back press to change one
 * word.
 */
@Composable
fun AppDrawerContent(
    current: AppScreen?,
    language: AppLanguage,
    onSelect: (AppScreen) -> Unit,
    onLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            AppScreen.entries.forEach { screen ->
                // The language is not a screen of its own, so it is drawn between two
                // of them rather than being an entry in the list above.
                if (screen == AppScreen.CHANGELOG) {
                    DrawerRow(
                        label = stringResource(R.string.drawer_language),
                        trailing = stringResource(language.labelRes),
                        selected = false,
                        onClick = onLanguage,
                    )
                }
                DrawerRow(
                    label = stringResource(screen.labelRes),
                    selected = screen == current,
                    onClick = { onSelect(screen) },
                )
            }
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

@Composable
private fun DrawerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
