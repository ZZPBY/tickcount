package io.github.ginko.tickcount.ui.theme

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

/** The number inside one countdown cell — "4 months", "11 days", … */
val CountNumberTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 32.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

/** The placeholder shown when the selected day has no countdown. */
val EmptyValueTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Light,
    fontSize = 64.sp,
    lineHeight = 68.sp,
    letterSpacing = 2.sp,
)
