# Project: Rade Keyboard

Rade Keyboard is a native **Android soft keyboard (IME)** for the Vietnamese/Rade
language. It renders a fully custom, `Canvas`-drawn keyboard with long-press
diacritic/tone-mark entry, and ships a setup/onboarding flow that walks the user
through enabling the keyboard and setting it as default.

- **Language:** Java
- **Build:** Gradle (AGP 8.4, `com.android.application`)
- **Min / Target SDK:** 21 / 35
- **Package:** `com.corriestroup.radekeyboard`

## Architecture

There is no backend and no database. The app is an `InputMethodService` plus two
`Activity` screens. Everything is driven by user touch → key events → text committed
to whatever field currently has focus.

```
Touch (ModernKeyboardView)
  → OnKeyPressListener callback
    → ModernInputMethodService (key handling, tone marks, auto-caps, spacing)
      → InputConnection.commitText() into the focused app
```

- **`ModernInputMethodService`** — the IME service. Owns key handling, Vietnamese
  tone-mark combining, auto-capitalization, and punctuation spacing rules. This is
  where text actually gets committed.
- **`ModernKeyboardView`** — a custom `View` that draws the whole keyboard on a
  `Canvas`: layouts (QWERTY + symbols), long-press alternate-character popups,
  continuous delete, and press animations. It knows nothing about the target field;
  it only emits key events via `OnKeyPressListener`.
- **`VietnameseText`** — pure (Android-free) helper for tone-mark / combining-char
  logic. Unit-tested. Prefer adding new text-transformation logic here so it stays
  testable.
- **`MainActivity`** — entry point; routes to onboarding or the "ready/test" screen
  depending on setup state.
- **`OnboardingActivity`** — 3-step setup flow (enable → set default → test) with an
  EN/VI language toggle.
- **`KeyboardSetupChecker`** — queries `InputMethodManager` / `Settings.Secure` to
  determine whether the keyboard is enabled and default.

## Key Files

| Path | Purpose |
|------|---------|
| `app/src/main/java/.../ModernInputMethodService.java` | IME service — key handling, tone marks, auto-caps |
| `app/src/main/java/.../ModernKeyboardView.java` | Custom Canvas-drawn keyboard view (layouts, popups, delete) |
| `app/src/main/java/.../VietnameseText.java` | Pure tone-mark/combining-char helpers (unit-tested) |
| `app/src/main/java/.../MainActivity.java` | Launcher; routes to onboarding vs. test screen |
| `app/src/main/java/.../OnboardingActivity.java` | Setup wizard + language toggle |
| `app/src/main/java/.../KeyboardSetupChecker.java` | Detects enabled/default IME state |
| `app/src/main/AndroidManifest.xml` | Declares the IME service + activities |
| `app/src/main/res/xml/method.xml` | IME subtypes (locales advertised to the system) |
| `app/src/main/res/values/strings.xml` + `values-vi/` | EN + VI localization |
| `app/build.gradle` | Module build config (SDK versions, deps, release settings) |
| `context/architecture/` | Concepts, conventions, component/state diagram |
| `context/process/` | Learnings, retrospectives, audit |
| `context/product/` | Decisions, plans |

## Development

Utility commands are wrapped in the `Makefile`. The Android SDK must be discoverable
(via `local.properties` `sdk.dir` or the `ANDROID_HOME` env var).

```bash
make build            # Assemble the debug APK
make release          # Assemble the release App Bundle (.aab)
make test             # Run JVM unit tests
make lint             # Run Android Lint
make install          # Install the debug build on a connected device/emulator
make clean            # Clean build outputs
```

All of these delegate to `./gradlew`. If `make` is unavailable, run the underlying
Gradle task directly (e.g. `./gradlew assembleDebug`).

## Testing

- **Unit tests (JVM):** `app/src/test/java/...` — run with `make test`
  (`./gradlew testDebugUnitTest`). Pure logic only; no Android framework.
- **Instrumented tests:** `app/src/androidTest/java/...` — require a device/emulator.

Most keyboard behavior lives inside Android framework classes (`InputMethodService`,
`View`) which are hard to unit-test without a device. **The correct move is to extract
pure logic into helpers like `VietnameseText` and test those.** See
`.claude/rules/testing-strategy.md`.

## Conventions

### Before Making Changes

- **Read the existing implementation first** — match the surrounding style before
  adding a key, layout row, or handler branch.
- Check `context/product/decisions.md` for prior decisions.
- Check `context/process/learnings.md` for known gotchas (there are several around
  tone-mark ordering, the space key, and the setup checker).
- Any change to text-transformation logic should go through / add to `VietnameseText`
  and gain a unit test.

### After Making Changes

- Build (`make build`) and run unit tests (`make test`) before handing off.
- If the change involved a non-obvious decision, log it in `context/product/decisions.md`.
- If you learned something useful, add it to `context/process/learnings.md`.

### Code Style

- Java, 4-space indentation, matching existing files.
- Keep `ModernKeyboardView` focused on drawing/touch and `ModernInputMethodService`
  focused on text; don't leak `InputConnection` logic into the view.
- No debug `Log.d` left in committed code — gate diagnostics or remove them.
- Colors and dimensions that the user can see should be theme-aware where practical
  (the keyboard palette already switches on night mode).
- Use mermaid for any diagrams.

@context/architecture/concepts.md
@context/architecture/conventions.md
@context/process/learnings.md
