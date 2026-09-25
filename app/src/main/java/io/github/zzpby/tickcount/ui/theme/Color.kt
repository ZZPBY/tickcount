package io.github.zzpby.tickcount.ui.theme

import androidx.compose.ui.graphics.Color

// Light scheme — an indigo seed, cool neutrals.
internal val LightPrimary = Color(0xFF4C56C0)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFDFE0FF)
internal val LightOnPrimaryContainer = Color(0xFF00105C)
internal val LightSecondary = Color(0xFF5B5D72)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFE0E1F9)
internal val LightOnSecondaryContainer = Color(0xFF181A2C)
internal val LightTertiary = Color(0xFF77536D)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFFFD7F1)
internal val LightOnTertiaryContainer = Color(0xFF2D1228)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
internal val LightBackground = Color(0xFFFBF8FF)
internal val LightOnBackground = Color(0xFF1B1B21)
internal val LightSurface = Color(0xFFFBF8FF)
internal val LightOnSurface = Color(0xFF1B1B21)
internal val LightSurfaceVariant = Color(0xFFE3E1EC)
internal val LightOnSurfaceVariant = Color(0xFF46464F)
internal val LightSurfaceContainer = Color(0xFFF1EFF7)
internal val LightSurfaceContainerHigh = Color(0xFFEBE8F1)
internal val LightOutline = Color(0xFF767680)
internal val LightOutlineVariant = Color(0xFFC7C5D0)

// Dark scheme.
internal val DarkPrimary = Color(0xFFBBC3FF)
internal val DarkOnPrimary = Color(0xFF1B2478)
internal val DarkPrimaryContainer = Color(0xFF333C9E)
internal val DarkOnPrimaryContainer = Color(0xFFDFE0FF)
internal val DarkSecondary = Color(0xFFC4C5DD)
internal val DarkOnSecondary = Color(0xFF2D2F42)
internal val DarkSecondaryContainer = Color(0xFF434559)
internal val DarkOnSecondaryContainer = Color(0xFFE0E1F9)
internal val DarkTertiary = Color(0xFFE6BAD7)
internal val DarkOnTertiary = Color(0xFF44263E)
internal val DarkTertiaryContainer = Color(0xFF5D3C55)
internal val DarkOnTertiaryContainer = Color(0xFFFFD7F1)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkBackground = Color(0xFF121318)
internal val DarkOnBackground = Color(0xFFE4E1E9)
internal val DarkSurface = Color(0xFF121318)
internal val DarkOnSurface = Color(0xFFE4E1E9)
internal val DarkSurfaceVariant = Color(0xFF46464F)
internal val DarkOnSurfaceVariant = Color(0xFFC7C5D0)
internal val DarkSurfaceContainer = Color(0xFF1E1F25)
internal val DarkSurfaceContainerHigh = Color(0xFF292A30)
internal val DarkOutline = Color(0xFF90909A)
internal val DarkOutlineVariant = Color(0xFF46464F)

/**
 * Accent colours a countdown can be tagged with. They are used for the calendar
 * dots and the header pill, and are picked to stay legible on both the light and
 * the dark surface, so one palette serves both themes.
 */
val EventAccents = listOf(
    Color(0xFF5B67D6), // indigo
    Color(0xFFD1495B), // rose
    Color(0xFF2E9E5B), // green
    Color(0xFFD98324), // amber
    Color(0xFF2A8FBD), // sky
    Color(0xFF9A5CD0), // violet
)

/** Returns the accent for [index], wrapping safely for any input. */
fun eventAccent(index: Int): Color =
    EventAccents[((index % EventAccents.size) + EventAccents.size) % EventAccents.size]
