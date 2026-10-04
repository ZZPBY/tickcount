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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.BackupCrypto
import io.github.zzpby.tickcount.data.ImportMode
import io.github.zzpby.tickcount.data.ImportOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate

/** Where the import has got to, since it can need the password and the mode in turn. */
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
    onImport: (text: String, password: String?, mode: ImportMode) -> ImportOutcome,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<Notice?>(null) }
    var exportOpen by remember { mutableStateOf(false) }
    var importStage by remember { mutableStateOf(ImportStage.CLOSED) }
    var pendingText by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf<String?>(null) }

    val suggestedName = remember {
        "tickcount-backup-${LocalDate.now()}.json"
    }

    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val text = onExport(pendingPassword)
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
        pendingPassword = null
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
            importStage = if (BackupCrypto.isEncrypted(text)) {
                ImportStage.PASSWORD
            } else {
                ImportStage.MODE
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
                pendingPassword = password
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
            onDismiss = { importStage = ImportStage.CLOSED },
            onConfirm = { password ->
                pendingPassword = password
                importStage = ImportStage.MODE
            },
        )
    }

    if (importStage == ImportStage.MODE) {
        ModeDialog(
            onDismiss = { importStage = ImportStage.CLOSED },
            onPick = { mode ->
                importStage = ImportStage.CLOSED
                message = when (val outcome = onImport(pendingText, pendingPassword, mode)) {
                    is ImportOutcome.Applied ->
                        Notice(R.plurals.data_applied, outcome.count)

                    ImportOutcome.WrongPassword -> Notice(R.string.data_wrong_password)
                    ImportOutcome.NotABackup -> Notice(R.string.data_not_a_backup)
                    ImportOutcome.Unreadable -> Notice(R.string.data_unreadable)
                }
                pendingPassword = null
                pendingText = ""
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
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.data_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
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
                enabled = !encrypted || password.isNotEmpty(),
                onClick = { onConfirm(password.takeIf { encrypted }) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    body: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.data_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
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
private fun ModeDialog(onPick: (ImportMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_mode_title)) },
        text = {
            Column {
                ModeRow(
                    label = stringResource(R.string.data_mode_replace),
                    body = stringResource(R.string.data_mode_replace_body),
                    onClick = { onPick(ImportMode.REPLACE) },
                )
                Spacer(Modifier.height(14.dp))
                ModeRow(
                    label = stringResource(R.string.data_mode_merge),
                    body = stringResource(R.string.data_mode_merge_body),
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
private fun ModeRow(label: String, body: String, onClick: () -> Unit) {
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
    }
}
