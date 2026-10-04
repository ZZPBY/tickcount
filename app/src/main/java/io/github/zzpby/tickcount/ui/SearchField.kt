package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.SearchScope
import io.github.zzpby.tickcount.ui.components.ChevronIcon

/** The name each scope is shown under. */
private val SearchScope.labelRes: Int
    get() = when (this) {
        SearchScope.NAME -> R.string.search_scope_name
        SearchScope.DATE -> R.string.search_scope_date
        SearchScope.TIME -> R.string.search_scope_time
    }

/**
 * The search box, shared by the home screen and the calendar's day list.
 *
 * What it looks at is chosen by the small control inside it — a name, a date and a
 * time are three different things to be looking for, and a box that guessed would be
 * wrong two times in three.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    scope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(50),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    ClearButton(onClick = { onQueryChange("") })
                }
                ScopePicker(scope = scope, onScopeChange = onScopeChange)
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The control inside the box that says what is being searched for.
 *
 * Its own shape and colour, so it reads as a control rather than as part of the
 * query text sitting next to it.
 */
@Composable
private fun ScopePicker(scope: SearchScope, onScopeChange: (SearchScope) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)

    Box {
        Row(
            modifier = Modifier
                .padding(end = 6.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { open = true }
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(scope.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(3.dp))
            ChevronIcon(
                pointsLeft = false,
                size = 14.dp,
                strokeWidth = 1.8.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SearchScope.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onScopeChange(option)
                        open = false
                    },
                )
            }
        }
    }
}
