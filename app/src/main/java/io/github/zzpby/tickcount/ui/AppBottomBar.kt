package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.ui.components.CalendarIcon
import io.github.zzpby.tickcount.ui.components.GridIcon
import io.github.zzpby.tickcount.ui.components.SlidersIcon
import io.github.zzpby.tickcount.ui.theme.Gutter

/**
 * The bar that switches between the three screens.
 *
 * Drawn as one floating card rather than an edge-to-edge strip: the page keeps its own colour
 * behind and below it, the way the content does on the screen this was modelled on. The
 * selected destination is a filled tile inside that card, so which one is current is legible
 * from the shape before the colour is read.
 *
 * It claims the bottom inset itself — the strip the system gesture bar keeps — because it is
 * handed none of it by the Scaffold it sits in.
 */
@Composable
fun AppBottomBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
            )
            .padding(horizontal = Gutter, vertical = 10.dp),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainer,
        // A real shadow rather than tonal elevation: the tonal version tints towards the
        // accent, which on the white theme would grey the one thing that has to look white.
        shadowElevation = 8.dp,
    ) {
        Row(modifier = Modifier.padding(6.dp)) {
            AppTab.entries.forEach { tab ->
                TabTile(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabTile(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TabIcon(tab = tab, color = tint)
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun TabIcon(tab: AppTab, color: Color) {
    when (tab) {
        AppTab.HOME -> GridIcon(modifier = Modifier.size(20.dp), size = 20.dp, color = color)
        AppTab.CALENDAR -> CalendarIcon(modifier = Modifier.size(20.dp), size = 20.dp, color = color)
        AppTab.SETTINGS -> SlidersIcon(modifier = Modifier.size(20.dp), size = 20.dp, color = color)
    }
}
