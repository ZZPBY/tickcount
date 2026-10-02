package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.zzpby.tickcount.R

/**
 * The strip at the very top: the app's own name, centred, with the changelog at
 * the left and the About box at the right.
 *
 * The name is centred in a Box rather than placed in a three-slot Row, so it sits
 * in the middle of the *screen* instead of the middle of whatever space the two
 * buttons happen to leave over.
 */
@Composable
fun AppTitleBar(
    onChangelog: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onChangelog,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text(
                text = stringResource(R.string.action_changelog),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = onAbout,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Text(
                text = stringResource(R.string.action_about),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
