package io.github.zzpby.tickcount.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds a [DateTimeFormatter] from a *pattern stored in string resources*, so
 * the date layout itself is localised rather than hard-coded.
 *
 * `zh` uses `yyyy年M月d日` and `en` uses `MMMM d, yyyy`; keeping the pattern in
 * `strings.xml` means a new language only needs a translation, not a code change.
 */
@Composable
fun rememberDateFormatter(@StringRes patternRes: Int): DateTimeFormatter {
    val pattern = stringResource(patternRes)
    val locale = currentLocale()
    return remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
}

/** The first locale in the user's configuration, falling back to the JVM default. */
@Composable
fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    return remember(configuration, locale) {
        if (locale == null || locale.language.isEmpty()) Locale.getDefault() else locale
    }
}
