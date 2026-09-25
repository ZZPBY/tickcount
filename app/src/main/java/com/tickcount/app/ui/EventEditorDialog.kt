package com.tickcount.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tickcount.app.R
import com.tickcount.app.data.CountdownEvent
import com.tickcount.app.ui.components.rememberDateFormatter
import java.time.LocalDate

/**
 * Names (or renames, or clears) the countdown attached to [date].
 *
 * Clearing the text field is treated as "remove this countdown", which keeps the
 * dialog down to a single action and matches what people expect from a rename box.
 */
@Composable
fun EventEditorDialog(
    date: LocalDate,
    initialTitle: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = initialTitle.isEmpty()
    var title by rememberSaveable(initialTitle) { mutableStateOf(initialTitle) }
    val focusRequester = remember { FocusRequester() }
    val dateFormatter = rememberDateFormatter(R.string.date_format_full)

    LaunchedEffect(Unit) {
        // The field is only focused for a brand new countdown; when renaming, the
        // user is usually correcting one word and does not want the keyboard.
        if (isNew) runCatching { focusRequester.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNew) R.string.dialog_new_title else R.string.dialog_edit_title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CountdownEvent.MAX_TITLE_LENGTH) },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.field_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                if (!isNew) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.dialog_clear_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            if (isNew) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            } else {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}
