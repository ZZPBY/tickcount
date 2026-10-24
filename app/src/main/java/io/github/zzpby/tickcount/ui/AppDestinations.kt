package io.github.zzpby.tickcount.ui

import io.github.zzpby.tickcount.R

/**
 * The three places the bottom bar switches between.
 *
 * Everything else the app can show hangs off one of these rather than being a destination
 * of its own: a countdown's own screen is opened from a list, and the settings screens are
 * opened from the settings list. That is what keeps the bar three wide however deep the app
 * is at the moment.
 */
enum class AppTab(val labelRes: Int) {
    HOME(R.string.tab_home),
    CALENDAR(R.string.tab_calendar),
    SETTINGS(R.string.tab_settings),
}

/**
 * Every screen the app can be showing.
 *
 * [SETTINGS] is the list of what used to be the drawer: it is a tab, and the rows under it
 * are screens reached from it. The language is not one of them — it is a single choice, and
 * a screen for it would be a tap and a back press to change one word.
 */
enum class AppScreen(val labelRes: Int) {
    HOME(R.string.tab_home),
    CALENDAR(R.string.tab_calendar),
    SETTINGS(R.string.tab_settings),
    REMINDERS(R.string.settings_notifications),
    APPEARANCE(R.string.settings_appearance),
    // The widgets have a page of their own rather than a section of Appearance: they are
    // drawn on the wallpaper, not in this window, and the choice is not this theme's.
    WIDGETS(R.string.settings_widgets),
    DATA(R.string.settings_data),
    CHANGELOG(R.string.settings_changelog),
    INTRO(R.string.settings_project_intro),
}

/** Which of the three the bar should light up while this screen is showing. */
val AppScreen.tab: AppTab
    get() = when (this) {
        AppScreen.HOME -> AppTab.HOME
        AppScreen.CALENDAR -> AppTab.CALENDAR
        else -> AppTab.SETTINGS
    }

/**
 * True for the screens reached from the settings list, which are the ones the title bar
 * offers a way back from rather than only the bar underneath.
 */
val AppScreen.isUnderSettings: Boolean
    get() = this != AppScreen.SETTINGS && tab == AppTab.SETTINGS
