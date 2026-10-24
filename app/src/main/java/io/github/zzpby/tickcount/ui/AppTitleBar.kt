package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.ui.components.ChevronIcon

/** What the button on the left of the bar does. */
enum class TitleBarNavigation { NONE, BACK }

/**
 * The bar every screen carries: a button on the left when there is somewhere to go back to,
 * the screen's name in the middle, and whatever that screen offers on the right.
 *
 * There is no menu button any more: the three destinations are on the bar at the foot of the
 * window, so a button here would only repeat them. What the left slot still carries is a way
 * up one level — a countdown's own page, the editor, and the screens reached from Settings.
 * The box around it is the same either way, so the bar does not jump as you move between
 * levels.
 *
 * The name is centred by the layout rather than by padding, so it stays in the
 * middle of the *screen* whether or not the right-hand slot has anything in it.
 *
 * The bar is hand-drawn rather than Material's `TopAppBar`, so it has to claim its insets
 * for itself: without that the whole bar is laid out from the top of the window, and its
 * button, name and actions are drawn underneath the clock and the system icons. Top and
 * sides are taken — the sides matter in landscape, where a cutout sits beside the button —
 * but not the bottom, which belongs to the screen below. The inset is taken outside the
 * 56dp, so the bar is as tall as it looks plus the strip the system keeps, and the screen
 * underneath starts where the bar really ends.
 */
@Composable
fun AppTitleBar(
    /**
     * The screen's name, or null to leave it out.
     *
     * The home screen passes null: its name is set at display size inside the screen, and a
     * bar repeating it in small type just above the large one says the same thing twice.
     */
    title: String?,
    navigation: TitleBarNavigation,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .fillMaxWidth()
            // A floor, not a fixed height: at a large system font the name has to be able
            // to push the bar taller rather than be clipped by it.
            .heightIn(min = 56.dp),
    ) {
        LeadingButton(
            navigation = navigation,
            onClick = onNavigate,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    // Kept clear of the two slots beside it, so a long name is
                    // ellipsised rather than drawn underneath them.
                    .padding(horizontal = 56.dp),
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
private fun LeadingButton(
    navigation: TitleBarNavigation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing to go back to, and the destinations are on the bar below: the slot is left
    // empty rather than filled with a button that would repeat them.
    if (navigation == TitleBarNavigation.NONE) return

    val description = stringResource(R.string.action_back)
    val corner = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(corner)
            .border(width = 1.5.dp, color = MaterialTheme.colorScheme.onSurface, shape = corner)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(
            pointsLeft = true,
            size = 22.dp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
