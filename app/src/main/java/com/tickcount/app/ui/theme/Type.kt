package com.tickcount.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tabular figures, so a ticking clock does not make the digits jitter as the
 * glyph widths change. Applied to every numeric style in the app.
 */
private const val TABULAR_FIGURES = "tnum"

val TickCountTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
        headlineSmall = base.headlineSmall.copy(fontFeatureSettings = TABULAR_FIGURES),
        titleMedium = base.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
        labelLarge = base.labelLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
    )
}

/** The oversized day count at the top of the screen. */
val DayCountTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Light,
    fontSize = 92.sp,
    lineHeight = 96.sp,
    letterSpacing = (-4).sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

/** The live `HH:MM:SS` line. */
val ClockTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = 1.sp,
    fontFeatureSettings = TABULAR_FIGURES,
)
