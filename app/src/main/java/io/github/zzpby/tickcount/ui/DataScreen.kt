package io.github.zzpby.tickcount.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.Backup
import io.github.zzpby.tickcount.data.BackupCrypto
import io.github.zzpby.tickcount.data.ImportMode
import io.github.zzpby.tickcount.data.ImportPreview
import io.github.zzpby.tickcount.data.ReadOutcome
import io.github.zzpby.tickcount.ui.components.EyeIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate

/**
 * Where the import has got to.
 *
 * The order is the point. The file is opened, and any password on it accepted, before the
 * screen asks what to do with it: a password reported wrong after that answer would cost
 * the user the answer as well as the attempt.
 */
private enum class ImportStage { CLOSED, PASSWORD, MODE }

/**
 * Something to tell the user, held as a resource rather than as resolved text.
 *
 * The callbacks that produce these run outside composition — a file picker hands its
 * result back long after the screen was drawn — so the string cannot be looked up where
 * the message is decided. Keeping the id and resolving it during composition is what
 * lets it follow the app's language and avoids reading resources off a context.
 */
private data class Notice(val id: Int, val count: Int? = null)

/**
 * Backing the data up, and putting it back.
 *
 * The export goes through the system's own file picker rather than to a fixed place, so
 * the copy ends up wherever the user keeps things — a cloud folder, an SD card, a
 * desktop over a cable — without the app asking for permission to write anywhere, and
 * without it knowing about any of those places.
 */
@Composable
fun DataScreen(
    onExport: (password: String?) -> String,
    onRead: (text: String, password: String?) -> ReadOutcome,
    onApply: (backup: Backup, mode: ImportMode) -> Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<Notice?>(null) }

    var exportOpen by remember { mutableStateOf(false) }

    // The password chosen for an export, held from the dialog until the file picker — which
    // is a different activity and answers much later — says where the file goes. Reading
    // needs no equivalent: once a password is accepted there, the backup is already in hand.
    var exportPassword by remember { mutableStateOf<String?>(null) }

    // The reading side: the file waiting on a password, and what came out of it.
    var importStage by remember { mutableStateOf(ImportStage.CLOSED) }
    var pendingText by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<Backup?>(null) }
    var pendingPreview by remember { mutableStateOf<ImportPreview?>(null) }
    var passwordRejected by remember { mutableStateOf(false) }

    /** Forgets the file the flow was working on, however it ended. */
    fun clearPending() {
        importStage = ImportStage.CLOSED
        pendingText = ""
        pendingBackup = null
        pendingPreview = null
        passwordRejected = false
    }

    /**
     * Opens what was picked, and moves on only once it has been read.
     *
     * A password that does not open the file comes back to the dialog rather than ending
     * the flow, so a typo costs one attempt instead of the whole file selection.
     */
    fun readPending(password: String?) {
        when (val outcome = onRead(pendingText, password)) {
            is ReadOutcome.Read -> {
                pendingBackup = outcome.backup
                pendingPreview = outcome.preview
                passwordRejected = false
                importStage = ImportStage.MODE
            }

            ReadOutcome.WrongPassword -> {
                passwordRejected = true
                importStage = ImportStage.PASSWORD
            }

            ReadOutcome.NotABackup -> {
                clearPending()
                message = Notice(R.string.data_not_a_backup)
            }

            ReadOutcome.Unreadable -> {
                clearPending()
                message = Notice(R.string.data_unreadable)
            }
        }
    }

    val suggestedName = remember {
        "tickcount-backup-${LocalDate.now()}.json"
    }

    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val text = onExport(exportPassword)
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(text.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                message = Notice(
                    if (ok) R.string.data_exported else R.string.data_write_failed
                )
            }
        }
        exportPassword = null
    }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                message = Notice(R.string.data_unreadable)
                return@launch
            }
            pendingText = text
            // An encrypted file has to ask for its password before it can even say
            // whether it is one of ours.
            if (BackupCrypto.isEncrypted(text)) {
                passwordRejected = false
                importStage = ImportStage.PASSWORD
            } else {
                readPending(password = null)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        ActionCard(
            title = stringResource(R.string.data_export),
            body = stringResource(R.string.data_export_body),
            onClick = { exportOpen = true },
        )
        Spacer(Modifier.height(10.dp))
        ActionCard(
            title = stringResource(R.string.data_import),
            body = stringResource(R.string.data_import_body),
            onClick = { openFile.launch(arrayOf("*/*")) },
        )

        message?.let { notice ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (notice.count == null) {
                    stringResource(notice.id)
                } else {
                    pluralStringResource(notice.id, notice.count, notice.count)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (exportOpen) {
        ExportDialog(
            onDismiss = { exportOpen = false },
            onConfirm = { password ->
                exportOpen = false
                exportPassword = password
                // The name only reaches the picker; where the file lands is the user's
                // choice from here on.
                createFile.launch(suggestedName)
            },
        )
    }

    if (importStage == ImportStage.PASSWORD) {
        PasswordDialog(
            title = stringResource(R.string.data_need_password),
            body = stringResource(R.string.data_need_password_body),
            rejected = passwordRejected,
            onEdit = { passwordRejected = false },
            onDismiss = { clearPending() },
            onConfirm = { password -> readPending(password) },
        )
    }

    if (importStage == ImportStage.MODE) {
        ModeDialog(
            preview = pendingPreview,
            onDismiss = { clearPending() },
            onPick = { mode ->
                // The file has already been read and accepted, so the only thing left to
                // decide is what to do with it.
                val count = pendingBackup?.let { onApply(it, mode) }
                clearPending()
                if (count != null) {
                    message = Notice(R.plurals.data_applied, count)
                }
            },
        )
    }
}

@Composable
private fun ActionCard(title: String, body: String, onClick: () -> Unit) {
    val shaping = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shaping)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shaping)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExportDialog(onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var encrypted by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }

    // A password that is one character out cannot be discovered later from anywhere, so
    // the second field is the only chance to catch it.
    val mismatch = encrypted && repeated.isNotEmpty() && repeated != password

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_export)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.data_encrypt),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = encrypted, onCheckedChange = { encrypted = it })
                }
                if (encrypted) {
                    Spacer(Modifier.height(10.dp))
                    PasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.data_password),
                        revealed = revealed,
                        onToggleReveal = { revealed = !revealed },
                    )
                    Spacer(Modifier.height(8.dp))
                    PasswordField(
                        value = repeated,
                        onValueChange = { repeated = it },
                        label = stringResource(R.string.data_password_repeat),
                        revealed = revealed,
                        onToggleReveal = { revealed = !revealed },
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text(stringResource(R.string.data_password_mismatch)) }
                        } else {
                            null
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.data_password_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !encrypted || (password.isNotEmpty() && password == repeated),
                onClick = { onConfirm(password.takeIf { encrypted }) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * A password field with the eye that shows what was typed.
 *
 * Shared by both dialogs that ask for one. The password on an export cannot be recovered
 * from anywhere, so being able to look at it before committing is the difference between
 * catching a typo and losing the file.
 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val description = stringResource(
        if (revealed) R.string.action_hide_password else R.string.action_show_password
    )

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            onEdit()
        },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (revealed) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                onClick = onToggleReveal,
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                EyeIcon(revealed = revealed)
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    body: String,
    rejected: Boolean,
    onEdit: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.data_password),
                    revealed = revealed,
                    onToggleReveal = { revealed = !revealed },
                    // The complaint belongs to the attempt that failed, not to the next
                    // one, so it goes as soon as the field is touched again.
                    onEdit = onEdit,
                    isError = rejected,
                )
                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.data_wrong_password),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty(),
                onClick = { onConfirm(password) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ModeDialog(
    preview: ImportPreview?,
    onPick: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
) {
    // The count each option would leave behind, said before it is chosen rather than
    // after. A line only appears when there is something to count.
    val discarded = preview?.discarded?.takeIf { it > 0 }?.let {
        stringResource(R.string.data_preview_discarded, it)
    }
    val added = preview?.added?.takeIf { it > 0 }?.let {
        stringResource(R.string.data_preview_added, it)
    }
    val overwritten = preview?.overwritten?.takeIf { it > 0 }?.let {
        stringResource(R.string.data_preview_overwritten, it)
    }
    val mergeNote = listOfNotNull(added, overwritten).joinToString(" · ").ifEmpty { null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_mode_title)) },
        text = {
            Column {
                ModeRow(
                    label = stringResource(R.string.data_mode_replace),
                    body = stringResource(R.string.data_mode_replace_body),
                    note = discarded,
                    onClick = { onPick(ImportMode.REPLACE) },
                )
                Spacer(Modifier.height(14.dp))
                ModeRow(
                    label = stringResource(R.string.data_mode_merge),
                    body = stringResource(R.string.data_mode_merge_body),
                    note = mergeNote,
                    onClick = { onPick(ImportMode.MERGE) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ModeRow(label: String, body: String, note: String?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
