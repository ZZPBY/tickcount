# TickCount

[![Build](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml/badge.svg)](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A minimal, fully offline countdown calendar for Android. Tap a day, give it a name, and
see how long is left — or how long it has been.

[Chinese](README.md)

**About 2 MB · No permissions · Fully offline · No ads · No account · Open source**

<p align="center">
  <img src="docs/screenshot-home.png" width="200" alt="Home: days left for each countdown, the past below" />
  <img src="docs/screenshot-calendar.png" width="200" alt="The calendar and that day's countdowns" />
  <img src="docs/screenshot-widgets.png" width="240" alt="Both home-screen widgets" />
</p>

## Features

- **Drawer** — the menu at the top left opens a half-screen drawer: Home and Calendar,
  Appearance and Backup & data, then Changelog and About this project. Language sits on its
  own at the foot of the drawer.
- **Countdown list** — one card each: name, tags and date — with the time after it where there
  is one — and the days left on the right. What has gone by sits at the foot of the list,
  under a **Past** heading.
- **Pinning and sorting** — a pinned countdown leads the list however it is sorted. The sort
  button offers **By date · nearest first**, **By date · furthest first** and **Date added ·
  newest first**, and remembers the choice. It governs the part still ahead.
- **Search** — a box above the list, with a control on the right for searching **Name** (tags
  included), **Date** or **Time**.
- **Exact minute** — a countdown is either all day, or set to an exact hour and minute.
- **Tags and colours** — as many tags as you like, and a colour of its own mixed on a hue
  strip in the editor. Unset, it takes the theme's colour.
- **Countdown screen** — tap a card to open it: the full years, months, days, hours, minutes
  and seconds breakdown, plus the pin switch, the date, the time and the tags.
- **Calendar** — a hand-drawn month grid, with up to three dots on a day that holds several
  countdowns. Tap a day and that day's countdowns are listed underneath, with the same search
  and sort.
- **Fast month switching** — tap the month title for a year-and-month picker with This month,
  and buttons that step the year by 1 and by 10.
- **Long-press a card** — pin it, edit it or delete it without opening it first. A delete asks
  for confirmation before it happens.
- **Adding several at once** — while creating a countdown, switch on “Add several at once”,
  give a gap and a count, and a run of countdowns sharing one name, tags, colour and time is
  added together.
- **Home-screen widgets** — two of them. **Single countdown** puts one countdown on the home
  screen, ticking to the second. **Countdown list** shows what is coming next, one per row.
  Both adapt to their size, and neither asks for a permission.
- **Themes** — white, black, blue and green, or mix your own on a hue strip. The app does not
  follow the system dark setting and does not take colours from the wallpaper; a choice stays
  put. The colours come from Tailwind CSS.
- **Bilingual** — follows the system language, or you can pick Chinese or English in the
  drawer. Date formats are localised too.
- **Backup and restore** — every countdown, and the appearance, sorting and language settings,
  can be written to a file and read back. An export can be encrypted with AES-256-GCM and a
  password; an import asks for that password first, then asks **Overwrite** or **Merge**, and
  says how many countdowns it would add, overwrite or discard before it does any of them.
- **Changelog and About** — every release is listed in the drawer with the installed version
  marked, and the project's own introduction is readable in the app, with a repository address
  ready to copy or to open.
- **Large fonts** — the title bar, the drawer and the cards keep clear of the status bar and
  grow with the system font scale, so nothing is squeezed.
- **No permissions, no network, no analytics.** One module, no third-party runtime libraries,
  `minSdk` 27.

## Adding several at once

The name, the tags, the colour and the time of day are answered once. Switch on “Add several
at once” in the new-countdown screen and two numbers are all that is left:

- **Days between** — how far apart neighbouring entries are, from 1 to 3650 days.
- **How many** — how many more of the same countdown to add, from 1 to 100.

The date chosen above is **added as well**, and every entry after it is that many days later
than the one before. So creating “Raid night” on 14 October with a gap of 2 and a count of 3
leaves the 14th, the 16th, the 18th and the 20th in the list: four in all.

Whatever the time field says is carried by every entry in the run — all day stays all day,
and 09:30 stays 09:30. They count as one batch, so ordering by when they were added keeps
them together, reading forwards by date. Each is an ordinary countdown afterwards, to edit
or delete on its own.

## How the countdown reads

The target is **00:00 local time on the chosen date**, or the exact time of day it is set
to. Once that moment has passed, the same arithmetic runs backwards, and the label changes
from “Time left” to “Time since”; on the target day itself it reads “Elapsed today”.

Leading fields that have not started counting yet are filled with dashes, so you can see at
a glance how far off a date is:

| Time left | Shown as |
|---|---|
| 4 months, 11 days, 6 hours, 30 minutes and 15 seconds | `----y 04mo 11d 06h 30m 15s` |
| 2 days and 30 seconds | `----y --mo 02d 00h 00m 30s` |
| 3 years, 4 months, 11 days, 6 hours, 30 minutes and 15 seconds | `0003y 04mo 11d 06h 30m 15s` |

Years, months and days are counted on the calendar, while hours, minutes and seconds are
counted as elapsed time, so the day the clocks change for daylight saving still counts as a
whole day even if it is actually 23 or 25 hours long.

## Home-screen widget

Long-press an empty spot on the home screen → Widgets: there are two to choose from.

**TickCount · Single countdown** — pick a countdown as well; the picker shows the date and,
where it has one, the time, so two countdowns with the same name can be told apart before the
widget is placed. It starts at 4×2 and rearranges itself as you drag: made short and wide, it
becomes one row of name, days and clock; made larger, it adds the target date and splits the
days into “1 year 1 month 4 days”. Tapping it opens that countdown. A countdown that names a
time is counted to that time here too, and says so — on the date line, or after the days in
the layouts that have no date line — so the widget and the countdown screen agree. The
seconds are driven by the system itself, without waking the app.

**TickCount · Countdown list** — nothing to pick: it lists what is coming next on its own,
nearest first, one per row, and tapping a row opens that countdown. A countdown that names
a time says so after the days — “Today 09:30”. The default 4×2 holds three rows; a taller
widget holds up to four, and the rows share the height between them; with nothing coming up
it says so.

**Neither asks for a permission.**

## Download

APKs are published in [Releases](../../releases/latest), and any successful build can
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

On Windows, use `gradlew.bat`.

## Built with

| | |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2026.09.00) |
| Build | AGP 9.4.1, Gradle 9.6.0, with all versions in `gradle/libs.versions.toml` |
| SDK | compileSdk 37, targetSdk 36, minSdk 27 |
| Storage | a small JSON document in `SharedPreferences` |
| Dependencies | at runtime, AndroidX and Compose only |

`minSdk` is 27. `java.time` arrived in API 26 and the theme's `windowLightNavigationBar`
in 27, so the project needs neither `coreLibraryDesugaring` nor any compatibility shim,
and the theme does not have to be split in two by version.

## Project layout

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              edge-to-edge host + the app theme, system-bar icon colours included
├── data/                        the model and how it is stored
│   ├── CountdownEvent.kt        the model: id / date / time / tags / colour / pinned
│   ├── EventCodec.kt            reading and writing the records as JSON, migrations included
│   ├── EventStore.kt            keeping that document in SharedPreferences
│   ├── SettingsStore.kt         the stored theme, sort order and language
│   ├── Backup.kt                the backup file's shape, and the outcome of an import
│   └── BackupCrypto.kt          password encryption for a backup (PBKDF2 + AES-GCM)
├── domain/                      pure logic, covered by unit tests
│   ├── Countdown.kt             the y/mo/d/h/m/s split and the dash placeholder
│   ├── BatchAdd.kt              adding several at once: one countdown spread over a gap
│   ├── CalendarMath.kt          building the month grid
│   ├── CountdownList.kt         ordering, sections and day distance
│   ├── Search.kt                what the search box looks at, and what counts as a match
│   ├── Changelog.kt             splitting the changelog into versions
│   ├── Readme.kt                pulling the introduction's sections out of the README
│   └── WidgetCountdown.kt       the widget's day split, shape rules and list picking
└── ui/
    ├── MainViewModel.kt         the state, and the process's only clock
    ├── TickCountApp.kt          the root: drawer, screen switching, back, dialogs
    ├── AppDrawer.kt             the side drawer
    ├── AppTitleBar.kt           the title bar: menu or back, name, action slot
    ├── HomeScreen.kt            the countdown list
    ├── CalendarScreen.kt        the month grid, and the chosen day's countdowns
    ├── EventCard.kt             the countdown card, shared by both lists
    ├── SearchField.kt           the search box and its scope control
    ├── EventDetailScreen.kt     one countdown's own screen
    ├── EventEditorScreen.kt     new or edit (a whole screen): name, tags, date, time,
    │                            colour, and adding several at once
    ├── DeleteConfirmDialog.kt   the confirmation before a delete
    ├── AppearanceScreen.kt      appearance: four theme presets and a custom hue
    ├── DataScreen.kt            data management: export, import, and the optional encryption
    ├── ChangelogScreen.kt       the changelog, taken from `更新日志.txt` in the repository
    ├── ProjectIntroScreen.kt    the introduction, taken from this file
    ├── calendar/                the hand-drawn month grid and the month picker
    ├── components/              Canvas icons, the hue strip, the Markdown renderer, localised formats
    ├── widget/                  the two home-screen widgets: drawing, receivers, config screen
    └── theme/                   the Material 3 palettes (Tailwind colour values) and the typography
```

The countdown arithmetic, list ordering, search matching, the widget's day split and what
its list picks, changelog parsing, backup encoding and decoding, and encryption, README
section extraction and the Markdown parser are all covered by the unit tests in
`app/src/test/`, along with month lengths, leap days, year boundaries, both daylight-saving
changes, and the “today is not past” boundary that is so easy to get wrong by a day.

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

TickCount stores one list of countdowns — date, name, time, tags, colour, pin and when it
was added — in its own private `SharedPreferences` file, with nothing else in it. The
colour belongs to each countdown and is used to tell them apart in the calendar and the
list. The home-screen widget keeps a second file, `tickcount_widgets`, recording only which
widget shows which countdown; the theme, sort order and language live in
`tickcount_settings`. All three sit in the app's private directory, and none of them leaves
the device on its own.

A backup exported from Backup & data goes wherever you choose to put it, encrypted or not,
as you decide; the app has no way to send it anywhere.
The app declares no permissions, makes no network requests, and contains no analytics or
advertising. There is a link to this repository in the introduction; tapping it opens the
address in the browser, because TickCount has no network ability of its own and no
`INTERNET` permission. The stored data leaves the device only if Android's own cloud backup
is on, which is what `res/xml/backup_rules.xml` decides.

## Licence

[MIT](LICENSE).
