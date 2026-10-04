package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.github.zzpby.tickcount.ui.components.MenuIcon

/** What the button on the left of the bar does. */
enum class TitleBarNavigation { MENU, BACK }

/**
 * The bar every screen carries: a button on the left, the screen's name in the
 * middle, and whatever that screen offers on the right.
 *
 * The left button opens the drawer from a screen the drawer lists, and goes up one
 * level from a screen it does not — a countdown's own page, or the introduction
 * under Settings. The box around it is the same either way, so the bar does not
 * jump as you move between levels.
 *
 * The name is centred by the layout rather than by padding, so it stays in the
 * middle of the *screen* whether or not the right-hand slot has anything in it.
 */
@Composable
fun AppTitleBar(
    title: String,
    navigation: TitleBarNavigation,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        LeadingButton(
            navigation = navigation,
            onClick = onNavigate,
            modifier = Modifier.align(Alignment.CenterStart),
        )

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
    val description = stringResource(
        when (navigation) {
            TitleBarNavigation.MENU -> R.string.action_open_menu
            TitleBarNavigation.BACK -> R.string.action_back
        }
    )
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
        when (navigation) {
            TitleBarNavigation.MENU -> MenuIcon(color = MaterialTheme.colorScheme.onSurface)
            TitleBarNavigation.BACK -> ChevronIcon(
                pointsLeft = true,
                size = 22.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
