package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.BuildConfig
import io.github.zzpby.tickcount.R

/**
 * What this app is, in one box.
 *
 * The version is read from [BuildConfig] rather than written into a string
 * resource, so it tracks `versionName` in the build script instead of going stale
 * the next time the app is released.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.about_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                ProjectLink()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * The project's home. Tapping it hands the URL to whatever app handles web links.
 *
 * TickCount still declares no permissions and still makes no request of its own —
 * the browser does the fetching — so the "no network" promise in the README
 * survives having a link here.
 *
 * The call is guarded because a device with no browser at all would otherwise
 * take the whole dialog down with an ActivityNotFoundException.
 */
@Composable
private fun ProjectLink() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.about_link_url)

    Text(
        text = stringResource(R.string.about_link_label),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        // Underlined as well as tinted: at bodySmall in a dialog, colour alone is
        // too easy to read as decoration rather than something tappable.
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { runCatching { uriHandler.openUri(url) } }
            .padding(vertical = 2.dp),
    )
}
