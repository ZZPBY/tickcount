package io.github.zzpby.tickcount.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.ThemePreset

/**
 * The palettes, taken from Tailwind CSS rather than mixed here.
 *
 * Tailwind's ramp is the most widely seen set of colours on the web, and its
 * structure is the part worth copying: a tinted page, white cards on top of it, and
 * one saturated accent. Surfaces derived from a hue by arithmetic come out pale and
 * samey — every level a slightly different tint of the same near-white — which is
 * what made the first attempt look washed out.
 *
 * The page is tinted with the theme's own hue rather than being grey in every
 * palette: a blue theme with a grey page does not read as a blue theme. The grey
 * ramp still supplies the text, the outlines and the accents that have to stay
 * legible whatever the background is.
 */
private val Gray50 = Color(0xFFF9FAFB)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray600 = Color(0xFF4B5563)
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)
private val Gray900 = Color(0xFF111827)

private val White = Color(0xFFFFFFFF)

private val Red600 = Color(0xFFDC2626)

/** The three background levels a palette uses, and the ink that goes on them. */
private class Surfaces(
    /** The page itself, behind everything. */
    val page: Color,
    /** Panels: the calendar, the drawer, the settings list. */
    val panel: Color,
    /** Cards: opaque, and the lightest step in a light palette so they lift. */
    val card: Color,
    val onPage: Color,
    val onVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

// The page, the panel and the card. One set serves every light palette: the accent
// is what a theme changes, and tinting the page as well made the whole screen the
// accent's colour, which read as heavy rather than as themed.
private val NeutralSurfaces = Surfaces(
    page = Gray100, panel = Gray200, card = White,
    onPage = Gray900, onVariant = Gray600, outline = Gray300, outlineVariant = Gray200,
)

private val DarkSurfaces = Surfaces(
    page = Gray900, panel = Gray800, card = Gray700,
    onPage = Gray50, onVariant = Gray400, outline = Gray500, outlineVariant = Gray700,
)

// ----------------------------------------------------------------- the palettes

private fun lightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    surfaces: Surfaces,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = Gray600,
    onSecondary = White,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = White,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Red600,
    onError = White,
    errorContainer = Color(0xFFFEE2E2), // red-100
    onErrorContainer = Color(0xFF7F1D1D), // red-900
    background = surfaces.page,
    onBackground = surfaces.onPage,
    surface = surfaces.page,
    onSurface = surfaces.onPage,
    surfaceVariant = surfaces.panel,
    onSurfaceVariant = surfaces.onVariant,
    surfaceContainer = surfaces.panel,
    surfaceContainerHigh = surfaces.card,
    outline = surfaces.outline,
    outlineVariant = surfaces.outlineVariant,
)

private val WhiteScheme = lightScheme(
    primary = Gray900,
    onPrimary = White,
    // A step darker than the page, because this is what a settings card is filled
    // with: the neutral palette has no hue to tint one with, so it has to be a grey
    // that is visibly not the page rather than the grey that *is* the page.
    primaryContainer = Gray200,
    onPrimaryContainer = Gray900,
    secondaryContainer = Gray200,
    onSecondaryContainer = Gray900,
    tertiary = Gray700,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Gray900,
    surfaces = NeutralSurfaces,
)

private val BlueScheme = lightScheme(
    primary = Color(0xFF2563EB), // blue-600
    onPrimary = White,
    primaryContainer = Color(0xFFDBEAFE), // blue-100
    onPrimaryContainer = Color(0xFF1E3A8A), // blue-900
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF4F46E5), // indigo-600
    tertiaryContainer = Color(0xFFE0E7FF), // indigo-100
    onTertiaryContainer = Color(0xFF312E81), // indigo-900
    surfaces = NeutralSurfaces,
)

private val GreenScheme = lightScheme(
    primary = Color(0xFF16A34A), // green-600
    onPrimary = White,
    primaryContainer = Color(0xFFDCFCE7), // green-100
    onPrimaryContainer = Color(0xFF14532D), // green-900
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF14532D),
    tertiary = Color(0xFF0D9488), // teal-600
    tertiaryContainer = Color(0xFFCCFBF1), // teal-100
    onTertiaryContainer = Color(0xFF134E4A), // teal-900
    surfaces = NeutralSurfaces,
)

private val BlackScheme = darkColorScheme(
    primary = Gray50,
    onPrimary = Gray900,
    primaryContainer = Gray700,
    onPrimaryContainer = Gray100,
    secondary = Gray300,
    onSecondary = Gray800,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray100,
    tertiary = Gray200,
    onTertiary = Gray800,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Gray50,
    error = Color(0xFFF87171), // red-400
    onError = Color(0xFF450A0A), // red-950
    errorContainer = Color(0xFF991B1B), // red-800
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkSurfaces.page,
    onBackground = DarkSurfaces.onPage,
    surface = DarkSurfaces.page,
    onSurface = DarkSurfaces.onPage,
    surfaceVariant = DarkSurfaces.panel,
    onSurfaceVariant = DarkSurfaces.onVariant,
    surfaceContainer = DarkSurfaces.panel,
    surfaceContainerHigh = DarkSurfaces.card,
    outline = DarkSurfaces.outline,
    outlineVariant = DarkSurfaces.outlineVariant,
)

/** The palette [settings] asks for. */
fun schemeFor(settings: AppSettings): ColorScheme =
    schemeFor(settings.preset, settings.customHue)

fun schemeFor(preset: ThemePreset, customHue: Float): ColorScheme = when (preset) {
    ThemePreset.WHITE -> WhiteScheme
    ThemePreset.BLACK -> BlackScheme
    ThemePreset.BLUE -> BlueScheme
    ThemePreset.GREEN -> GreenScheme
    ThemePreset.CUSTOM -> customScheme(customHue)
}

/**
 * A palette built around one hue the user mixed: the same neutral page the named
 * palettes use, with the hue carried by the accent and its containers.
 */
private fun customScheme(hue: Float): ColorScheme {
    val degrees = ((hue % 360f) + 360f) % 360f

    fun shade(chroma: Float, lightness: Float, shift: Float = 0f): Color =
        Color.hsl(((degrees + shift) % 360f + 360f) % 360f, chroma, lightness)

    return lightScheme(
        primary = shade(0.72f, 0.45f),
        onPrimary = White,
        primaryContainer = shade(0.85f, 0.93f),
        onPrimaryContainer = shade(0.70f, 0.22f),
        secondaryContainer = shade(0.80f, 0.91f),
        onSecondaryContainer = shade(0.65f, 0.24f),
        tertiary = shade(0.60f, 0.45f, shift = 42f),
        tertiaryContainer = shade(0.85f, 0.93f, shift = 42f),
        onTertiaryContainer = shade(0.65f, 0.22f, shift = 42f),
        surfaces = NeutralSurfaces,
    )
}

/**
 * The colour a countdown paints itself with, given its own [hue].
 *
 * Null means the countdown has not been given a colour, and the caller falls back to
 * the theme's own text colours — which is what keeps the default "white" from
 * becoming invisible on a light surface.
 */
fun eventHueColor(hue: Float?, dark: Boolean): Color? = hue?.let {
    Color.hsl(it, if (dark) 0.55f else 0.72f, if (dark) 0.68f else 0.42f)
}

/** How much of an event's colour is laid over the card behind it. */
const val EVENT_WASH_ALPHA = 0.16f

/** How much of an event's colour is laid over the card while it is selected. */
const val EVENT_WASH_ALPHA_STRONG = 0.30f

/**
 * The colour a countdown paints itself with, falling back to the theme's own accent
 * when it has not been given one.
 *
 * For the marks that have to exist either way — the calendar dots, the heading on a
 * countdown's screen — the fallback is what keeps an uncoloured countdown visible
 * rather than leaving a hole where its dot should be.
 */
@Composable
fun eventAccent(hue: Float?): Color =
    eventHueColor(hue, isDarkScheme()) ?: MaterialTheme.colorScheme.primary
