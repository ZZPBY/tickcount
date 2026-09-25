# TickCount

A tiny, offline Android app that answers one question: **how many days until…?**

Pick any day on the calendar, give it a name, and the top of the screen shows how
far away it is — as a big day count plus a live `HH:MM:SS` clock. It also counts
*up* for dates that have already passed, so it works for anniversaries and
"days since" as well as countdowns.

*Read this in [中文](README.zh-CN.md).*

---

## Features

- **Countdown to any date** — tap a day, name it, done.
- **As many countdowns as you like** — every named day gets a coloured dot on the
  calendar, so a month at a glance shows what is coming.
- **Live `HH:MM:SS`** that ticks every second and rolls over exactly when the day
  count changes.
- **Counts up for the past** — "12 days ago" and counting.
- **Material 3**, light and dark, with wallpaper-based dynamic colour on Android 12+.
- **Chinese and English**, following the system language. Dates are formatted with
  locale-aware patterns (`2027年2月6日 星期六` / `Saturday, February 6, 2027`).
- **No permissions, no network, no analytics.** The app cannot phone home because
  it never asks for the ability to.
- Small: a single module, no third-party UI or calendar libraries, minSdk 29.

## How the two numbers work

The header deliberately shows **two different quantities**, because they answer two
different questions:

| | Meaning |
|---|---|
| **The big number** | *Calendar* days between today and the target date. Pick a date two days out and it says `2`, even at 23:00 when only 25 hours remain. This is what "how many days until…" means to a person. |
| **The clock** | Time left in the *current local day* (or time elapsed, for a past date). It rolls over to a new day at midnight — the same instant the day count changes — so the two numbers can never contradict each other. |

The alternative — one duration split into `D 天 HH:MM:SS` — would show `0 天` for
most of the day before a big event, which reads as a bug. See
[`Countdown.kt`](app/src/main/java/com/tickcount/app/domain/Countdown.kt) for the
full reasoning.

## Download

Grab the APK from the [latest release](../../releases/latest), or from the
artifacts of any green CI run under **Actions**.

Install it by opening the file on your phone (you will need to allow installing
from unknown sources for your browser or file manager).

> **Signing note.** Releases published by CI are signed with the standard Android
> *debug* key, because the repository has no private signing key. The APK is
> minified and not debuggable, and installs fine, but it is not suitable for
> publishing to Google Play. If you fork this and want real releases, create a
> `keystore.properties` in the project root (it is `.gitignore`d):
>
> ```properties
> storeFile=release.jks
> storePassword=…
> keyAlias=…
> keyPassword=…
> ```
>
> `assembleRelease` will then use your key automatically. Note that switching keys
> means users must uninstall the debug-signed build first.

## Building

Requirements:

- JDK 17
- Android SDK Platform 37 (`android-37`) and Build Tools 36.0.0
- The Android SDK location in `local.properties`, or `ANDROID_HOME` exported

```bash
# unit tests
./gradlew testDebugUnitTest

# installable APK -> app/build/outputs/apk/release/
./gradlew assembleRelease
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose, Material 3 (Compose BOM 2026.09.00) |
| Build | AGP 9.4.1, Gradle 9.6.0, version catalog in `gradle/libs.versions.toml` |
| SDK | compileSdk 37, targetSdk 36, minSdk 29 |
| Storage | `SharedPreferences` holding one small JSON document |
| Dependencies | AndroidX + Compose only — no third-party libraries at all |

Because `minSdk` is 29, `java.time` is available from the platform: no
`coreLibraryDesugaring`, no compatibility shims.

### Project layout

```
app/src/main/java/com/tickcount/app/
├── MainActivity.kt              edge-to-edge host
├── data/                        CountdownEvent model + JSON persistence
├── domain/                      pure, unit-tested logic
│   ├── Countdown.kt             day maths, daylight-saving aware
│   └── CalendarMath.kt          month grid construction
└── ui/
    ├── MainViewModel.kt         state + the one clock in the process
    ├── TickCountScreen.kt       the single screen
    ├── calendar/                hand-rolled month grid
    ├── countdown/               the header
    └── theme/                   Material 3 colour and type
```

## Privacy

TickCount stores a list of `(date, name)` pairs in its own private
`SharedPreferences`, and nothing else. It declares **no permissions**. It makes no
network requests, contains no analytics and no ads. The only thing that leaves the
device is the list itself, if you have Android's own cloud backup enabled — and
`res/xml/backup_rules.xml` decides that.

## License

[MIT](LICENSE). Do whatever you like with it.
