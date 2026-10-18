package io.github.zzpby.tickcount.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.ui.theme.CardShape
import io.github.zzpby.tickcount.ui.theme.hairline

/**
 * The two switches that change how the app behaves rather than how it looks.
 *
 * Given their own entry in the drawer, above Appearance: they were at the bottom of a screen
 * about colours, where nobody would look for a notification setting. Appearance is now only
 * about the palette.
 */
@Composable
fun TogglesScreen(
    showSeconds: Boolean,
    remind: Boolean,
    onShowSeconds: (Boolean) -> Unit,
    onRemind: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // The permission request needs an activity result, so it lives on this screen. A refusal
    // turns the switch back off, so the setting never claims a reminder that cannot arrive.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onRemind(granted) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ToggleCard(
            title = stringResource(R.string.appearance_seconds),
            hint = stringResource(R.string.appearance_seconds_hint),
            checked = showSeconds,
            onChange = onShowSeconds,
        )

        Spacer(Modifier.height(10.dp))

        ToggleCard(
            title = stringResource(R.string.appearance_remind),
            hint = stringResource(R.string.appearance_remind_hint),
            checked = remind,
            onChange = { want ->
                when {
                    !want -> onRemind(false)
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> onRemind(true)
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> onRemind(true)
                    else -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}

/** One switch with its explanation, in the card shape the rest of the app uses. */
@Composable
private fun ToggleCard(
    title: String,
    hint: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .hairline(CardShape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
