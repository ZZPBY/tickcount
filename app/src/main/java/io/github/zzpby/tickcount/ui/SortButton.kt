package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.SortOrder
import io.github.zzpby.tickcount.ui.components.SortIcon
import io.github.zzpby.tickcount.ui.theme.PillShape
import io.github.zzpby.tickcount.ui.theme.hairline

/** The name each ordering is offered under. */
private val SortOrder.labelRes: Int
    get() = when (this) {
        SortOrder.DATE_ASC -> R.string.sort_date_asc
        SortOrder.DATE_DESC -> R.string.sort_date_desc
        SortOrder.ADDED -> R.string.sort_added
    }

/**
 * The button that chooses how a list is ordered.
 *
 * A pill beside the search box rather than an icon in the title bar: the order is a property
 * of the list under it, and putting it next to the box that filters that same list keeps the
 * two controls that shape the list together. Both lists that can be ordered share it.
 */
@Composable
fun SortButton(
    current: SortOrder,
    onPick: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.action_sort)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .hairline(PillShape)
                .clickable { open = true }
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            SortIcon(
                size = 19.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { option ->
                val selected = option == current
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onPick(option)
                        open = false
                    },
                    trailingIcon = {
                        // A tick rather than a highlight, so the current choice is legible
                        // without relying on a colour.
                        if (selected) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                )
            }
        }
    }
}
