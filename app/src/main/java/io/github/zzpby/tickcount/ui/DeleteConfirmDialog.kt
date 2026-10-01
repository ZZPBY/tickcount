package io.github.zzpby.tickcount.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzpby.tickcount.R

/**
 * Stands between a delete *button* and the deletion itself.
 *
 * Two different buttons lead here — the one under the countdown and the one in
 * the editor dialog — so the confirmation lives in one place instead of being
 * written twice at the call sites.
 *
 * Emptying the name field is deliberately not routed through this: that is a
 * visible act of editing rather than a stray tap, and confirming it would add a
 * step to every rename. See the strings file.
 *
 * The destructive option is the confirm button, and it is tinted as an error, so
 * that the path of least resistance for a mis-tap is "keep it".
 */
@Composable
fun DeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_title)) },
        text = { Text(stringResource(R.string.dialog_delete_message, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
