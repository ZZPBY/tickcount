package io.github.zzpby.tickcount

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zzpby.tickcount.data.AppLanguage
import io.github.zzpby.tickcount.data.SettingsStore
import io.github.zzpby.tickcount.ui.MainViewModel
import io.github.zzpby.tickcount.ui.TickCountApp
import io.github.zzpby.tickcount.ui.theme.TickCountTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * Countdowns something outside the app asked to open.
     *
     * A widget names the countdown it is showing, so tapping it lands on that countdown
     * rather than merely in the app. A flow rather than a value, because the same widget
     * can be tapped twice and the second tap has to be a new request rather than the same
     * one seen again.
     *
     * `replay` rather than a buffer: the request is made in `onCreate`, before there is
     * anything collecting it, and a shared flow with no subscribers drops what it is
     * given. The flow belongs to the activity, so a fresh launch starts it empty and the
     * replay cannot resurrect an old tap.
     */
    private val openRequests = MutableSharedFlow<String>(replay = 1)

    /**
     * The language the resources were built with, kept so a later change can be noticed.
     *
     * [attachBaseContext] runs before anything else and is the only point at which an
     * activity's resources can still be replaced, so the choice has to be read there
     * rather than in `onCreate`.
     */
    private var appliedLanguage: AppLanguage? = null

    override fun attachBaseContext(newBase: Context) {
        val language = SettingsStore(newBase).load().language
        appliedLanguage = language
        super.attachBaseContext(newBase.withLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestOpen(intent)
        setContent {
            // The theme is applied here, above the screen, so that changing it in the
            // appearance settings repaints the whole window — including the system bar
            // areas — rather than only the part the screen owns.
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Strings come from resources, and resources cannot be swapped under a
            // running activity. Starting over is the whole of the cost, and it happens
            // once, when the language is deliberately changed.
            LaunchedEffect(settings.language) {
                if (settings.language != appliedLanguage) recreate()
            }

            // `enableEdgeToEdge` picks the system bar icon colours from the *system's*
            // dark setting, which is no longer what the app follows: choosing the black
            // theme on a light device would draw dark icons on a dark bar. This overrides
            // it to follow the app instead. It is the one API for the job that behaves
            // the same from API 21 up, so no version check is needed.
            val dark = settings.preset.dark
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            TickCountTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TickCountApp(viewModel, openRequests = openRequests)
                }
            }
        }
    }

    /**
     * An already-running app gets the request here rather than in [onCreate]; the intent
     * is taken over as well, so a later restoration reads the same one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestOpen(intent)
    }

    private fun requestOpen(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_OPEN_EVENT) ?: return
        openRequests.tryEmit(id)
    }
}

/**
 * The countdown a launch should open, named by whatever asked for it.
 *
 * A widget is the only thing that sets it today; the constant lives here because the
 * activity is the side that reads it.
 */
const val EXTRA_OPEN_EVENT = "io.github.zzpby.tickcount.extra.OPEN_EVENT"

/**
 * This context, restricted to one language.
 *
 * A configuration context rather than a per-app locale: `LocaleManager` only exists from
 * API 33, and this app runs from 27.
 */
private fun Context.withLanguage(language: AppLanguage): Context {
    val tag = language.tag ?: return this
    val configuration = Configuration(resources.configuration).apply {
        setLocales(LocaleList.forLanguageTags(Locale.forLanguageTag(tag).toLanguageTag()))
    }
    return createConfigurationContext(configuration)
}
