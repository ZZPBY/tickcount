package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    REMINDERS(R.string.settings_notifications),
    APPEARANCE(R.string.settings_appearance),
    DATA(R.string.drawer_data),
    CHANGELOG(R.string.drawer_changelog),
    INTRO(R.string.settings_project_intro),
}

/**
 * Whether this destination opens a group, and so is drawn with a gap above it.
 *
 * Which entries start a group is a fact about [AppScreen] rather than about the drawer, so
 * it is stated here and the drawing code just asks. Appearance opens the settings, after
 * the two screens you browse; the changelog opens the two you read, after the one that
 * writes.
 */
private val AppScreen.startsGroup: Boolean
    get() = this == AppScreen.REMINDERS || this == AppScreen.CHANGELOG

/**
 * The gap between two groups.
 *
 * Sized against the 4dp between two rows inside a group 鈥?four times that, and equal to the
 * 12dp the column already leaves at its top and bottom, so the ends and the seams read as
 * the same kind of pause.
 */
private val GroupSpacing = 12.dp

/**
 * The panel that slides in from the left.
 *
 * Hand-rolled rather than built on `ModalNavigationDrawer`, which clamps its sheet
 * to between 240dp and 360dp wide: on a phone that is nowhere near the half-screen
 * width this one is specified to take. Laying it out directly also keeps it a plain
 * child of the screen rather than a window of its own.
 *
 * The language is not a screen of its own: it is one choice, and a screen for it would be a
 * tap and a back press to change one word. It is drawn at the foot of the panel rather than
 * among the rows, because everything above it is a place to go and it is not.
 *
 * The destinations come in three groups 鈥?what you browse, what you set up, what you read 鈥? * and the gaps between them are the only thing saying so. Every row is otherwise the same
 * weight, and a flat list would leave the reader to guess where one kind ends and the next
 * begins.
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
        // The card colour, which is pure white in the white theme. What separates the panel
        // from the page there is the shadow below and the rounded edge, not a different tone:
        // a grey drawer on a white page reads as a stray piece of the old palette.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        // A real shadow rather than a tonal elevation: tonal elevation tints the surface
        // towards the primary colour, which on a white theme is a grey 鈥?the one thing this
        // panel should not be.
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                // The panel is drawn edge to edge, and as a sibling of the Scaffold rather
                // than a child of it, it is handed none of the insets the Scaffold applies
                // to its own content. Without this the top row is drawn twelve dp below the
                // top of the window and sits under the status bar.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 12.dp),
        ) {
            Column(
                // Only the destinations scroll. Six rows and two gaps are taller than a
                // window in landscape or in split screen, and a Column that cannot scroll
                // would not clip them either: it would measure the last ones to nothing and
                // they would simply vanish. The language is outside this, so it is never one
                // of the rows that can vanish.
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                AppScreen.entries.forEach { screen ->
                    if (screen.startsGroup) Spacer(Modifier.height(GroupSpacing))
                    DrawerRow(
                        label = stringResource(screen.labelRes),
                        selected = screen == current,
                        onClick = { onSelect(screen) },
                    )
                }
            }

            DrawerRow(
                label = stringResource(R.string.drawer_language),
                trailing = stringResource(language.labelRes),
                selected = false,
                onClick = onLanguage,
            )
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
                // Transparent rather than a card colour, so an ordinary row is the drawer's
                // own panel showing through and only the current destination is lifted.
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            // A floor rather than a fixed height: at a large system font the row has to
            // grow with its text instead of clipping it.
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
