package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.WidgetPanel
import io.github.zzpby.tickcount.data.WidgetTextSize
import io.github.zzpby.tickcount.ui.components.ChoicePill
import io.github.zzpby.tickcount.ui.components.SectionLabel

/** The name each panel and text size is shown under. */
private val WidgetPanel.labelRes: Int
    get() = when (this) {
        WidgetPanel.LIGHT -> R.string.appearance_widget_light
        WidgetPanel.DARK -> R.string.appearance_widget_dark
        WidgetPanel.SYSTEM -> R.string.appearance_widget_system
    }

private val WidgetTextSize.labelRes: Int
    get() = when (this) {
        WidgetTextSize.SMALL -> R.string.appearance_widget_text_small
        WidgetTextSize.STANDARD -> R.string.appearance_widget_text_standard
        WidgetTextSize.LARGE -> R.string.appearance_widget_text_large
    }

/**
 * The home-screen widgets' own appearance: a panel and a text size.
 *
 * A destination of its own rather than a section of Appearance: the widgets are drawn by
 * the launcher on a wallpaper, outside this app's window and outside its theme, so a choice
 * made beside the theme read as one more thing that would repaint the app.
 *
 * The panel applies to both widgets. The text size only has room to work in the list, whose
 * rows are drawn at fixed sizes; the single widget fills its cell with the largest text
 * that fits, which is already as large as it gets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetSettingsScreen(
    settings: AppSettings,
    onPanel: (WidgetPanel) -> Unit,
    onTextSize: (WidgetTextSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.widget_settings_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.appearance_widget_panel))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WidgetPanel.entries.forEach { option ->
                ChoicePill(
                    label = stringResource(option.labelRes),
                    selected = option == settings.widgetPanel,
                    onClick = { onPanel(option) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.appearance_widget_text_size))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WidgetTextSize.entries.forEach { option ->
                ChoicePill(
                    label = stringResource(option.labelRes),
                    selected = option == settings.widgetTextSize,
                    onClick = { onTextSize(option) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.appearance_widget_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(36.dp))
    }
}
