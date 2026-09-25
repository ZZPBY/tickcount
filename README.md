# TickCount

[![Build](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml/badge.svg)](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A minimal, offline Android countdown calendar. Tap a day, give it a name, and
TickCount shows how long until — or since — that date.

*Read this in [中文](README.zh-CN.md).*

## Features

- **Countdown to any date** — tap a day in the calendar and name it.
- **One fixed-width line** — `yyyy年MM月dd日 HH时mm分ss秒`, ticking every second.
- **Counts up for past dates** — the same line under a `Time since` label.
- **Multiple countdowns** — each named day is marked with a coloured dot, so a
  month at a glance shows what is coming.
- **Fast month navigation** — the month title opens a year/month picker with
  one-year and ten-year steps.
- **Material 3** — light and dark, with wallpaper-based dynamic colour on
  Android 12+.
- **Chinese and English**, following the system language, with locale-aware date
  patterns.
- **No permissions, no network, no analytics.**
- Single module, no third-party libraries, `minSdk` 29.

## How the countdown reads

The target is **00:00 local time on the chosen date**. After that instant the same
arithmetic runs in reverse and the label changes from `Time left` to `Time since`.

A slot that is still *leading* — zero, with every larger unit also zero — is
blanked with dashes the width of its pattern letter. A zero sitting between
counting units stays a real number, so nothing flickers mid-count:

| Countdown | Display |
|---|---|
| 4 months, 11 days, 6h 30m 15s | `----年04月11日 06时30分15秒` |
| 2 days, 30s | `----年--月02日 00时00分30秒` |
| 3 years, 4 months | `0003年04月11日 06时30分15秒` |
| no countdown set | `----年--月--日 --时--分--秒` |

The line is set in monospace so digits and dashes occupy identical widths and it
never shifts as the seconds tick. Its size is measured at runtime and scaled to
the available width, keeping it on one line across devices and font scales.

Years, months and days are calendar arithmetic, while hours, minutes and seconds
are elapsed time. A daylight-saving day is therefore still reported as one day
even though it is 23 or 25 hours long. See
[`Countdown.kt`](app/src/main/java/io/github/zzpby/tickcount/domain/Countdown.kt).

## Download

APKs are published under [Releases](../../releases/latest) and as artifacts of
each green CI run on the **Actions** tab.

APKs built by CI are signed with the Android debug key, since no private signing
key is stored in the repository. They install normally but are not suitable for
store publication. See [Signing](#signing) to build with a real key.

## Building

Requirements:

- JDK 17
- Android SDK Platform 37 and Build Tools 36.0.0
- The SDK location in `local.properties`, or `ANDROID_HOME` exported

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleRelease     # APK -> app/build/outputs/apk/release/
```

On Windows, use `gradlew.bat`.

## Signing

`assembleRelease` uses a real key when a `keystore.properties` file exists in the
project root, and otherwise falls back to the Android debug key so that a fresh
clone still produces an installable APK.

Create a keystore:

```bash
keytool -genkeypair -v -keystore release.jks -alias tickcount \
        -keyalg RSA -keysize 4096 -validity 10000 -storetype PKCS12 \
        -dname "CN=Your Name"
```

Only `CN` is meaningful; `O`/`OU`/`L`/`ST`/`C` are metadata that Android never
reads. `validity 10000` is roughly 27 years, comfortably past the certificate
horizon Google Play requires.

Then point `keystore.properties` at it:

```properties
storeFile=release.jks
storePassword=…
keyAlias=tickcount
keyPassword=…
```

The file is `.gitignore`d, along with `*.jks` and `*.keystore`. For PKCS12 the two
passwords are identical.

> Android identifies an application by its signing certificate, not by its
> package name. A keystore used for a published release must be backed up and must
> never be replaced: losing it, or switching keys afterwards, permanently prevents
> shipping an update that installs over an existing one.

### Signing in CI

Add four repository secrets — `KEYSTORE_BASE64` (`base64 -w0 release.jks`),
`STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — and materialise them before the
build step in [`.github/workflows/android.yml`](.github/workflows/android.yml):

```yaml
- name: Write keystore
  env:
    KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
  run: |
    echo "$KEYSTORE_BASE64" | base64 -d > release.jks
    cat > keystore.properties <<EOF
    storeFile=release.jks
    storePassword=${{ secrets.STORE_PASSWORD }}
    keyAlias=${{ secrets.KEY_ALIAS }}
    keyPassword=${{ secrets.KEY_PASSWORD }}
    EOF
```

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose, Material 3 (Compose BOM 2026.09.00) |
| Build | AGP 9.4.1, Gradle 9.6.0, version catalog in `gradle/libs.versions.toml` |
| SDK | compileSdk 37, targetSdk 36, minSdk 29 |
| Storage | `SharedPreferences` holding one small JSON document |
| Dependencies | AndroidX and Compose only |

`minSdk` 29 means `java.time` comes from the platform, so no
`coreLibraryDesugaring` and no compatibility shims are needed.

## Project layout

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              edge-to-edge host
├── data/                        CountdownEvent model + JSON persistence
├── domain/                      pure, unit-tested logic
│   ├── Countdown.kt             the y/mo/d/h/m/s breakdown and slot formatting
│   └── CalendarMath.kt          month grid construction
└── ui/
    ├── MainViewModel.kt         state + the one clock in the process
    ├── TickCountScreen.kt       the single screen
    ├── calendar/                hand-rolled month grid + year/month picker
    ├── countdown/               the header
    └── theme/                   Material 3 colour and type
```

The countdown arithmetic is covered by unit tests in `app/src/test/`, including
month lengths, leap days, year boundaries and both daylight-saving transitions.

## App icon

![icon preview](tools/icon-preview.png)

Vector XML cannot draw CJK glyphs, so the launcher label is rasterised by
[`tools/IconGen.java`](tools/IconGen.java). It loads Microsoft YaHei, sizes the
text to fit Android's 66 dp adaptive-icon safe circle, and writes the PNG shipped
in `res/drawable-xxxhdpi/`:

```bash
java tools/IconGen.java app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
```

## Privacy

TickCount stores a list of `(date, name)` pairs in its own private
`SharedPreferences` and nothing else. It declares no permissions, makes no network
requests, and contains no analytics or advertising. The list leaves the device only
if Android's own cloud backup is enabled, which `res/xml/backup_rules.xml`
governs.

## License

[MIT](LICENSE).
