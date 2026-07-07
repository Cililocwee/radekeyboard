# Conventions — Rade Keyboard

## Build & Tooling

| Concern | Choice |
|---------|--------|
| Language | Java |
| Build system | Gradle + Android Gradle Plugin 8.4 |
| Plugin | `com.android.application` (single module `:app`) |
| Namespace / applicationId | `com.corriestroup.radekeyboard` |
| minSdk / targetSdk / compileSdk | 21 / 35 / 35 |
| Version | `versionCode` + `versionName` in `app/build.gradle` — bump both per release |
| View binding | Enabled (`buildFeatures.viewBinding true`) |
| Dependencies | `androidx.appcompat`, `androidx.constraintlayout`; JUnit + Espresso for tests |

## Android SDK Location

Gradle needs to find the SDK. Either:
- Set `sdk.dir=/path/to/Android/sdk` in `local.properties` (gitignored), or
- Export `ANDROID_HOME` before building.

`local.properties` is gitignored and must never be committed.

## Source Layout

```
app/src/main/java/com/corriestroup/radekeyboard/   # Java sources
app/src/main/res/                                  # layouts, drawables, values, xml/
app/src/main/AndroidManifest.xml                   # IME service + activities
app/src/test/java/...                              # JVM unit tests
app/src/androidTest/java/...                       # instrumented tests
```

- `res/values/` = English (default); `res/values-vi/` = Vietnamese.
- `res/xml/method.xml` declares the IME subtypes (locales advertised to the system).
- Keyboard colors are chosen at runtime in `ModernKeyboardView` based on night mode.

## Versioning & Release

- Release artifact is an **App Bundle** (`.aab`) built with `make release`.
- Build outputs (`app/release/`, `build/`) are **not** committed — they're gitignored.
  Signing config and upload live outside the repo (see the deploy playbook /
  `/prepare-to-deploy`).
- Bump `versionCode` (integer, must increase every Play upload) and `versionName`
  (human string) together in `app/build.gradle`.

## Git

- Target branch for PRs: `main`.
- Don't commit build artifacts, `local.properties`, or IDE caches under `.idea/`.

## Logging

No `Log.d`/`Log.v` diagnostics in committed code. The IME sees input across every app,
so leaked logs are a privacy concern. Remove or gate diagnostics behind an explicit
debug flag.
