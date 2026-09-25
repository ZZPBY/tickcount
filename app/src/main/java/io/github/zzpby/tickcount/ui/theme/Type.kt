package io.github.zzpby.tickcount.ui.theme

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

/**
 * The single countdown line, e.g. `----年04月11日 06时30分15秒`.
 *
 * Monospace is deliberate and load-bearing: every slot occupies the same width
 * whether it currently holds digits or the dash placeholder, so the line neither
 * reflows nor slides sideways as the seconds tick.
 *
 * The size is chosen per screen by the caller; this only fixes face and weight.
 */
val CountdownLineTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp,
)
