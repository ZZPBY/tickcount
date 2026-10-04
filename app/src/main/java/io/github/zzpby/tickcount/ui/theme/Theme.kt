package io.github.zzpby.tickcount.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.SettingsStore

/**
 * Material 3 theme, built from whichever hue the user chose.
 *
 * Deliberately without `dynamicColor`: the point of the appearance setting is that
 * the palette is something the user picked, and colours taken from the wallpaper
 * are precisely the ones they cannot pick. The Android 12+ branch that used to
 * read them is gone, along with the hand-written indigo fallback it sat beside —
 * every scheme now comes out of [schemeFor].
 *
 * [settings] is nullable so that a screen which only wants the app's colours — the
 * widget picker, say — can write `TickCountTheme { }` and have the stored choice
 * read for it. A default argument cannot do that, because reading it needs
 * `remember`, and a default argument is not a composable scope.
 */
@Composable
fun TickCountTheme(
    settings: AppSettings? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolved = settings ?: remember(context) { SettingsStore(context).load() }
    val colorScheme = remember(resolved) { schemeFor(resolved) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TickCountTypography,
        content = content,
    )
}

/**
 * True when the scheme in force is a dark one, decided from the scheme rather than
 * from the settings.
 *
 * Callers that colour something by hand — an event's own hue, which needs a
 * different lightness on each side — then work the same whichever way the theme
 * was chosen, and without having to be handed the settings.
 */
@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
