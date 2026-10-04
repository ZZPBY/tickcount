# TickCount

[![Build](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml/badge.svg)](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A minimal, fully offline countdown calendar for Android. Tap a day, give it a name, and
see how long is left — or how long it has been.

[中文说明](README.md)

## Features

- **Drawer navigation** — the menu button at the top left opens a half-screen drawer whose
  entries come in three groups: Home and Calendar to browse, then Appearance, the language
  switch and Backup & data to set things up, and finally the changelog and About this
  project to read.
- **Countdown list** — one rounded card per countdown on the home screen: name, tags and
  date on the left, days remaining on the right.
- **Pin to the top** — switch it on in a countdown's own screen and it leads the list
  whatever the list is sorted by.
- **Sorting** — the button at the top right of a list orders it by **target date, soonest
  first**, **target date, furthest first**, or **date added, newest first**. The choice is
  remembered.
- **Search** — a box above the list, with a control inside it that switches between
  **name** (which includes tags), **date** and **time**.
- **To the minute** — a countdown can cover a whole day, or name an exact time.
- **Several tags** — as many tags per countdown as you like.
- **Your own colour** — each countdown can have one, mixed on a hue strip in the editor.
  Leave it unset and it takes the theme's colour.
- **Countdown screen** — tap a card to open it: the full years, months, days, hours,
  minutes and seconds breakdown, plus the date, the time and the tags.
- **Calendar** — a hand-drawn month grid, with up to three dots on a day that holds
  several countdowns. Tap a day and its countdowns are listed underneath, with the same
  search and sort; the + at the top right adds one on that day.
- **Fast month switching** — tap the month title for a year-and-month picker with ±1 and
  ±10 year buttons.
- **Delete asks first** — pressing Delete does not take effect until you confirm.
- **Home-screen widget** — put a day on the home screen, ticking to the second, adapting
  its layout to its size, and still with no permissions.
- **Themes** — white, black, blue and green, or mix your own on a hue strip. The app does
  not follow the system dark setting, and does not take colours from the wallpaper; a
  choice stays put. The colours come from Tailwind CSS.
- **Bilingual** — follows the system language, or pick Chinese or English in the drawer.
  Date formats are localised too.
- **Backup and restore** — in Backup & data, every countdown and setting can be written to a
  file and read back; an import either replaces what is here or merges into it. An export
  can be encrypted with a password, using AES-256-GCM, and a wrong password says so.
- **No permissions, no network, no analytics.**
- One module, no third-party libraries, `minSdk` 27 (Android 8.1).

## How the countdown reads

The target is **00:00 local time on the chosen date**. Once that moment has passed the
same arithmetic runs backwards, and the label changes from “time left” to “time since”.

Fields that have not started counting yet are stood in for by dashes, so how far off
something is can be read at a glance:

| Time left | Shown as |
|---|---|
| 4 months 11 days 6 h 30 min 15 s | `----y 04mo 11d 06h 30m 15s` |
| 2 days 30 s | `----y --mo 02d 00h 00m 30s` |
| 3 years 4 months 11 days 6 h 30 min 15 s | `0003y 04mo 11d 06h 30m 15s` |

Years, months and days are counted on the calendar while hours, minutes and seconds are
elapsed time, so the day a clock changes still counts as a whole day even if it is really
23 or 25 hours long.

## Home-screen widget

Long-press an empty spot on the home screen → Widgets → TickCount, then pick a countdown.
It starts at 4×2 and rearranges itself as you drag: made short and wide it becomes one
row of name, days and clock; made larger it adds the target date and splits the days into
“1y 1mo 4d”.

The seconds are driven by the system itself, without waking the app, and the widget
**still asks for no permissions**.

## Download

APKs are published under [Releases](../../releases/latest), and any successful build can
also be downloaded from the **Actions** tab. What changed in each version is in
[`更新日志.txt`](更新日志.txt).

## Building

You need:

- JDK 17
- Android SDK Platform 37 and Build Tools 36.0.0
- the SDK path in `local.properties`, or `ANDROID_HOME` exported

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleRelease     # APK -> app/build/outputs/apk/release/
```

On Windows use `gradlew.bat`.

## Built with

| | |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2026.09.00) |
| Build | AGP 9.4.1, Gradle 9.6.0, versions together in `gradle/libs.versions.toml` |
| SDK | compileSdk 37, targetSdk 36, minSdk 27 |
| Storage | a small JSON document in `SharedPreferences` |
| Dependencies | AndroidX and Compose only |

`minSdk` is 27. `java.time` arrived in API 26 and the theme's `windowLightNavigationBar`
in 27, so the project needs neither `coreLibraryDesugaring` nor any compatibility shim,
and the theme does not have to be split by version.

## Layout

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              edge-to-edge host + the app theme, bar icons included
├── data/                        the model and how it is stored
│   ├── CountdownEvent.kt        the model: id / date / time / tags / colour / pinned
│   ├── EventCodec.kt            reading and writing the records, migrations included
│   ├── EventStore.kt            keeping that document in SharedPreferences
│   ├── SettingsStore.kt         the stored theme, sort order and language
│   ├── Backup.kt                the backup file's shape, and how an import went
│   └── BackupCrypto.kt          password encryption for a backup (PBKDF2 + AES-GCM)
├── domain/                      pure logic, covered by unit tests
│   ├── Countdown.kt             the y/mo/d/h/m/s split and the dash placeholder
│   ├── CalendarMath.kt          building the month grid
│   ├── CountdownList.kt         ordering, day distance and the “already past” flag
│   ├── Search.kt                what the search box looks at
│   ├── Changelog.kt             splitting the changelog into releases
│   ├── Readme.kt                pulling the introduction's sections out of the README
│   └── WidgetCountdown.kt       the widget's day split and shape rules
└── ui/
    ├── MainViewModel.kt         the state, and the process's only clock
    ├── TickCountApp.kt          the root: drawer, screen switching, back, dialogs
    ├── AppDrawer.kt             the side drawer
    ├── AppTitleBar.kt           the title bar: menu or back, name, action slot
    ├── HomeScreen.kt            the countdown list
    ├── CalendarScreen.kt        the month, and the chosen day's countdowns
    ├── EventCard.kt             the countdown card, shared by both lists
    ├── SearchField.kt           the search box and its scope control
    ├── EventDetailScreen.kt     one countdown's own screen
    ├── EventEditorDialog.kt     new or edit: name, tags, date, time, colour
    ├── DeleteConfirmDialog.kt   the confirmation before a delete
    ├── AppearanceScreen.kt      appearance: four presets and a custom hue
    ├── DataScreen.kt            data: export, import, and the optional password
    ├── ChangelogScreen.kt       the changelog, taken from the repository's own file
    ├── ProjectIntroScreen.kt    the introduction, taken from this file
    ├── calendar/                the hand-drawn month grid and the month picker
    ├── components/              canvas icons, the hue strip, Markdown, localised formats
    ├── widget/                  the home-screen widget: drawing, receiver, picker
    └── theme/                   the Material 3 palettes (Tailwind values) and the type
```

The countdown arithmetic, list ordering, search matching, README section extraction and
the Markdown parser are all covered by the unit tests in `app/src/test/`, along with
month lengths, leap days, year boundaries, both clock changes, and the “today is not
past” boundary that is so easy to get a day wrong.

## App icon

![icon preview](.tools/图标生成/icon-preview.png)

Vector XML cannot draw Chinese characters, so the launcher's lettering is rasterised by
[`.tools/图标生成/IconGen.java`](.tools/图标生成/IconGen.java): it loads Microsoft YaHei,
scales the characters to land exactly inside the 66dp safe circle of an Android adaptive
icon, and writes a PNG into `res/drawable-xxxhdpi/`.

```bash
java .tools/图标生成/IconGen.java app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
```

## Privacy

TickCount stores one list of `(date, name, colour)` in its own private
`SharedPreferences` and nothing else. The colour belongs to each countdown and is used to
tell them apart in the calendar and the list. The home-screen widget keeps a second file,
`tickcount_widgets`, holding only which widget shows which countdown; the theme, sort
order and language live in `tickcount_settings`. All three sit in the app's private
directory, and none of them leaves the device.
The app declares no permissions, makes no network requests, and contains no analytics or
advertising. There is a link to this repository in the introduction; tapping it hands the
address to the browser, because TickCount has no network ability of its own and no
`INTERNET` permission. The list leaves the device only if Android's own cloud backup is
on, which is what `res/xml/backup_rules.xml` decides.

## Licence

[MIT](LICENSE).
