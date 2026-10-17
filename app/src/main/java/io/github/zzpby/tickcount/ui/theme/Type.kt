package io.github.zzpby.tickcount.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tabular figures, so a ticking clock does not make the digits jitter as the glyph widths
 * change. Applied to every numeric style in the app.
 */
private const val TABULAR_FIGURES = "tnum"

/**
 * The type scale: deliberately wider than Material's, because most of this app is one number
 * and the word next to it.
 *
 * The gap between a screen's title and its body is large on purpose. A countdown is read in
 * a glance, and a glance needs one thing much bigger than everything around it — which is
 * what the display styles are for. Body sizes stay at Material's defaults, since prose is
 * read rather than glanced at.
 */
val TickCountTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontSize = 52.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        displayMedium = base.displayMedium.copy(
            fontSize = 42.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        displaySmall = base.displaySmall.copy(
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = base.titleLarge.copy(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = base.titleMedium.copy(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        titleSmall = base.titleSmall.copy(
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = base.labelLarge.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        labelSmall = base.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        ),
    )
}

/**
 * The single countdown line, e.g. `----年04月11日 06时30分15秒`.
 *
 * Monospace is deliberate and load-bearing: every slot occupies the same width whether it
 * currently holds digits or the dash placeholder, so the line neither reflows nor slides
 * sideways as the seconds tick.
 *
 * The size is chosen per screen by the caller; this only fixes face, weight and spacing.
 */
val CountdownLineTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 21.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.5.sp,
)
