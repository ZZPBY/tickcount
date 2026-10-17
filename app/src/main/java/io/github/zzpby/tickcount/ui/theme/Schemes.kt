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
 * The palettes, built from a hue rather than from a fixed ramp.
 *
 * The first palette was Tailwind's cool greys, and it made every screen read as a document:
 * cool grey is the colour of paper that has already been printed on. These are warm instead,
 * and the warmth follows the theme: choosing a colour tints the page, the panels and the
 * hairlines as well as the accent, so the whole screen changes together. A page that stayed
 * grey while the buttons changed colour was a theme applied to the controls rather than to
 * the app.
 *
 * White is the exception on purpose: it is the plain one, a white page with near-black ink.
 * Black is the same idea in the other direction.
 */
private class Surfaces(
    val page: Color,
    val panel: Color,
    val card: Color,
    val onPage: Color,
    val onVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

/**
 * The surfaces for one hue: a page tinted clearly enough to see from across the room, cards
 * that stay near-white so they still lift off it, and ink with the same hue in it so nothing
 * looks like a grey that wandered in.
 */
private fun tintedSurfaces(hue: Float): Surfaces {
    fun shade(chroma: Float, lightness: Float) = Color.hsl(hue, chroma, lightness)
    return Surfaces(
        page = shade(0.34f, 0.940f),
        panel = shade(0.28f, 0.895f),
        // Only a hint of the hue, so a card still separates from the page without turning
        // into a second wash of colour.
        card = shade(0.08f, 0.988f),
        onPage = shade(0.22f, 0.125f),
        onVariant = shade(0.14f, 0.370f),
        outline = shade(0.22f, 0.835f),
        outlineVariant = shade(0.18f, 0.900f),
    )
}

/** White: a white page, near-black ink, and no hue anywhere. */
private val WhiteSurfaces = Surfaces(
    page = Color(0xFFFFFFFF),
    panel = Color(0xFFF4F4F2),
    card = Color(0xFFFFFFFF),
    onPage = Color(0xFF17171A),
    onVariant = Color(0xFF6B6B70),
    outline = Color(0xFFE5E5E3),
    outlineVariant = Color(0xFFEFEFF0),
)

/**
 * The warm paper the emerald theme has always sat on.
 *
 * Left as it was rather than derived from the hue: this is the palette the rest of the app
 * was designed against, and a page tinted to match the accent made the accent itself read as
 * quieter than it does on paper.
 */
private val PaperSurfaces = Surfaces(
    page = Color(0xFFF7F5F0),
    panel = Color(0xFFEFEBE3),
    card = Color(0xFFFFFFFF),
    onPage = Color(0xFF1C1B18),
    onVariant = Color(0xFF6F6A60),
    outline = Color(0xFFE3DED3),
    outlineVariant = Color(0xFFEFEBE2),
)

/** Black: warm near-black, so the dark theme is the same family as the light ones. */
private val DarkSurfaces = Surfaces(
    page = Color(0xFF141311),
    panel = Color(0xFF242320),
    card = Color(0xFF1D1C19),
    onPage = Color(0xFFF3F1EA),
    onVariant = Color(0xFFA8A296),
    outline = Color(0xFF352F28),
    outlineVariant = Color(0xFF2A2622),
)

// ---------------------------------------------------------------- the schemes

private fun light(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    surfaces: Surfaces,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    // The accent is the second and third colour too: three hues on one screen was one too
    // many, and the secondary/tertiary slots are only read by a few Material parts.
    secondary = primary,
    onSecondary = onPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = onPrimaryContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = primaryContainer,
    onTertiaryContainer = onPrimaryContainer,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFCE7E5),
    onErrorContainer = Color(0xFF7A1D16),
    background = surfaces.page,
    onBackground = surfaces.onPage,
    surface = surfaces.page,
    onSurface = surfaces.onPage,
    surfaceVariant = surfaces.panel,
    onSurfaceVariant = surfaces.onVariant,
    surfaceContainerLowest = surfaces.card,
    surfaceContainerLow = surfaces.card,
    surfaceContainer = surfaces.panel,
    surfaceContainerHigh = surfaces.card,
    surfaceContainerHighest = surfaces.panel,
    outline = surfaces.outline,
    outlineVariant = surfaces.outlineVariant,
    inverseSurface = surfaces.onPage,
    inverseOnSurface = surfaces.page,
    scrim = Color.Black,
)

private fun dark(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = primary,
    onSecondary = onPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = onPrimaryContainer,
    tertiary = Color(0xFFE7C36B),
    onTertiary = Color(0xFF2A1F05),
    tertiaryContainer = Color(0xFF3A2E10),
    onTertiaryContainer = Color(0xFFF0DBA4),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF8A2018),
    onErrorContainer = Color(0xFFFCE7E5),
    background = DarkSurfaces.page,
    onBackground = DarkSurfaces.onPage,
    surface = DarkSurfaces.page,
    onSurface = DarkSurfaces.onPage,
    surfaceVariant = DarkSurfaces.panel,
    onSurfaceVariant = DarkSurfaces.onVariant,
    surfaceContainerLowest = DarkSurfaces.card,
    surfaceContainerLow = DarkSurfaces.card,
    surfaceContainer = DarkSurfaces.panel,
    surfaceContainerHigh = DarkSurfaces.card,
    surfaceContainerHighest = DarkSurfaces.panel,
    outline = DarkSurfaces.outline,
    outlineVariant = DarkSurfaces.outlineVariant,
    inverseSurface = DarkSurfaces.onPage,
    inverseOnSurface = DarkSurfaces.page,
    scrim = Color.Black,
)

/**
 * The accent of a tinted palette: saturated enough to carry a 34sp number, dark enough to
 * read as text on the page, and not so dark that the tint above it looks like a mistake.
 */
private fun accentFor(hue: Float): Color = Color.hsl(hue, 0.52f, 0.36f)

private fun tintFor(hue: Float): Color = Color.hsl(hue, 0.55f, 0.92f)

private fun onTintFor(hue: Float): Color = Color.hsl(hue, 0.55f, 0.17f)

/** A light palette built from one hue: the page and the ink both carry it. */
private fun tinted(hue: Float): ColorScheme = light(
    primary = accentFor(hue),
    onPrimary = Color.White,
    primaryContainer = tintFor(hue),
    onPrimaryContainer = onTintFor(hue),
    surfaces = tintedSurfaces(hue),
)

private val WhiteScheme = light(
    primary = Color(0xFF17171A),
    onPrimary = Color.White,
    // A grey a step away from the panel it sits on, so a selected row and a tinted pill are
    // still visible in the one theme that has no colour to tell them apart with.
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF17171A),
    surfaces = WhiteSurfaces,
)

/**
 * The emerald the app's icon is built on, kept at the exact value it has always had: it is
 * the palette the rest of the app was designed against.
 */
private val TealScheme = light(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFEA),
    onPrimaryContainer = Color(0xFF083F3B),
    surfaces = PaperSurfaces,
)

private val MistScheme = tinted(214f)
private val AmberScheme = tinted(28f)
private val RoseScheme = tinted(345f)

private val NightScheme = dark(
    primary = Color(0xFF7FD6C9),
    onPrimary = Color(0xFF05201D),
    primaryContainer = Color(0xFF17332F),
    onPrimaryContainer = Color(0xFFC8EDE6),
)

/** The palette [settings] asks for. */
fun schemeFor(settings: AppSettings): ColorScheme =
    schemeFor(settings.preset, settings.customHue)

fun schemeFor(preset: ThemePreset, customHue: Float): ColorScheme = when (preset) {
    ThemePreset.WHITE -> WhiteScheme
    ThemePreset.BLACK -> NightScheme
    ThemePreset.TEAL -> TealScheme
    ThemePreset.MIST -> MistScheme
    ThemePreset.AMBER -> AmberScheme
    ThemePreset.ROSE -> RoseScheme
    ThemePreset.CUSTOM -> tinted(((customHue % 360f) + 360f) % 360f)
}

/**
 * The colour a countdown paints itself with, given its own [hue].
 *
 * Null means it was not given one, and the caller falls back to the theme's accent — which is
 * what keeps an uncoloured countdown visible rather than leaving a hole where its dot is.
 */
fun eventHueColor(hue: Float?, dark: Boolean): Color? = hue?.let {
    Color.hsl(it, if (dark) 0.52f else 0.62f, if (dark) 0.66f else 0.40f)
}

/** How much of a countdown's own colour is laid over the card behind it. */
const val EVENT_WASH_ALPHA = 0.10f

/** The colour a countdown paints itself with, falling back to the theme's accent. */
@Composable
fun eventAccent(hue: Float?): Color =
    eventHueColor(hue, isDarkScheme()) ?: MaterialTheme.colorScheme.primary
