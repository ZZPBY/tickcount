package io.github.zzpby.tickcount.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.ThemePreset
import io.github.zzpby.tickcount.ui.components.HueSlider
import io.github.zzpby.tickcount.ui.theme.FieldShape
import io.github.zzpby.tickcount.ui.theme.hairline
import io.github.zzpby.tickcount.ui.theme.schemeFor

/**
 * The name each preset is shown under.
 *
 * An extension rather than a field on the enum, so that the stored settings — which
 * the theme reads before any screen exists — do not have to know about resources.
 */
private val ThemePreset.labelRes: Int
    get() = when (this) {
        ThemePreset.WHITE -> R.string.theme_white
        ThemePreset.BLACK -> R.string.theme_black
        ThemePreset.TEAL -> R.string.theme_teal
        ThemePreset.MIST -> R.string.theme_mist
        ThemePreset.AMBER -> R.string.theme_amber
        ThemePreset.ROSE -> R.string.theme_rose
        ThemePreset.CUSTOM -> R.string.theme_custom
    }

/**
 * Picks the colour the whole app is painted in.
 *
 * The screen is its own preview: the theme is applied from the settings as they
 * change, so dragging the hue strip repaints this page — and everything behind it —
 * as the knob moves. That is why there is no separate swatch to compare against.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    settings: AppSettings,
    onPreset: (ThemePreset) -> Unit,
    onCustomHue: (Float) -> Unit,
    onShowSeconds: (Boolean) -> Unit,
    onRemind: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // The permission is asked for here because a permission request needs an activity
    // result, and this is the screen the switch lives on. A refusal still turns the switch
    // back off, so the setting never claims a reminder that cannot arrive.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onRemind(granted) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        SectionLabel(stringResource(R.string.appearance_presets))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemePreset.entries.forEach { preset ->
                PresetCard(
                    preset = preset,
                    customHue = settings.customHue,
                    selected = settings.preset == preset,
                    onClick = { onPreset(preset) },
                )
            }
        }

        if (settings.preset == ThemePreset.CUSTOM) {
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.appearance_custom_hue))
            Spacer(Modifier.height(10.dp))
            HueSlider(hue = settings.customHue, onHueChange = onCustomHue)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.appearance_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.appearance_surfaces))
        Spacer(Modifier.height(10.dp))
        SurfaceLadder()

        Spacer(Modifier.height(24.dp))
        // A switch rather than a section of its own: the countdown still reads the same,
        // it just stops at the minute, and a paragraph about that would be longer than the
        // change.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .hairline(FieldShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.appearance_seconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.appearance_seconds_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = settings.showSeconds, onCheckedChange = onShowSeconds)
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .hairline(FieldShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.appearance_remind),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.appearance_remind_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = settings.remind,
                onCheckedChange = { want ->
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

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * One theme, shown as the page it would paint.
 *
 * The card wears the palette's *background* with that background's own text colour,
 * so white reads as a white card with black writing and black as the reverse — which
 * is what someone picking a theme is actually choosing between. The accent, which is
 * the other half of a palette, is the dot beside the name; white, blue and green all
 * have a light page, so without the dot they would be three identical cards.
 */
@Composable
private fun PresetCard(
    preset: ThemePreset,
    customHue: Float,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = remember(preset, customHue) { schemeFor(preset, customHue) }
    val shaping = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(shaping)
            .background(scheme.background)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) scheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shaping,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(preset.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = scheme.onBackground,
            )
        }
    }
}

/**
 * The three surface levels the app actually uses, one on top of the other.
 *
 * Drawn so the choice can be checked at a glance: a theme whose card, panel and
 * background are too close together looks flat everywhere else in the app, and this
 * is the one place that shows it without hunting for an example.
 */
@Composable
private fun SurfaceLadder() {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        LadderStep(stringResource(R.string.appearance_background), scheme.surface, scheme.onSurface)
        Spacer(Modifier.height(8.dp))
        LadderStep(
            stringResource(R.string.appearance_panel),
            scheme.surfaceContainer,
            scheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        LadderStep(
            stringResource(R.string.appearance_card),
            scheme.surfaceContainerHigh,
            scheme.onSurface,
        )
    }
}

@Composable
private fun LadderStep(label: String, background: Color, foreground: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = foreground,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
