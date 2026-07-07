# Learnings — Rade Keyboard

Gotchas and non-obvious facts discovered while working in this codebase. Add to this
as you go.

## Text Handling

- **Tone-mark ordering is base-first.** A tone mark is committed as
  `<base char> + <combining code point>` (e.g. `a` + `́`). The service deletes
  the preceding base character and re-commits the combined pair. Get the order wrong
  and the mark renders on the wrong glyph (or not at all).
- **Tone marks only apply to vowels.** Non-vowel + tone mark is dropped (nothing is
  committed). The breve (`˘`) is the exception — it applies to any preceding char.
- **The space key travels as a character, not a special key.** The layout row uses
  the label `" "`, which falls through `handleKeyPress`'s `switch` to the default
  branch and is committed as a literal space — it does *not* go through `KEY_SPACE`.
  Both paths produce a space, but if you change space behavior, check both.
- **Punctuation auto-inserts a trailing space and can eat a preceding one.** `.`, `?`,
  `!` replace a single trailing space then commit `punct + " "`. Auto-capitalization
  keys off `". "` / `"? "` / `"! "` before the cursor.

## Keyboard View

- **`ModernKeyboardView` is a ~1000-line monolith** doing layout, drawing, touch,
  popups, and timers. When adding logic, prefer extracting pure helpers (see
  `VietnameseText`) over growing the view further.
- **Keyboard height is recomputed on layout switch.** `toggleSymbolMode()` calls
  `calculateDimensions()` + `requestLayout()` because the symbol layout has a
  different row count.
- **Colors are chosen at runtime by night mode**, not from resource qualifiers — the
  palette is selected in the view based on `Configuration.UI_MODE_NIGHT_MASK`.

## Android / IME

- **This is an IME — it sees input across every app.** Never leave `Log.d` in
  committed code; it can leak user keystrokes/context. Diagnostics must be removed or
  gated behind an explicit debug flag.
- **`method.xml` subtypes are what the system advertises.** If a locale isn't listed
  there, the OS won't surface the keyboard for it, regardless of the app's UI language.
- **The manifest namespace comes from `build.gradle`.** A `package=` attribute in
  `AndroidManifest.xml` is deprecated under AGP 8 and is ignored (build warning).
- **`versionCode` must strictly increase for every Play upload**, even for re-uploads
  of the "same" version.

## Build

- **The SDK path is not committed** (`local.properties` is gitignored). Set `sdk.dir`
  or `ANDROID_HOME` locally, or Gradle fails with "SDK location not found".
- **Release build artifacts must not be committed.** `app/release/*.aab|*.apk` and
  `build/` are gitignored; earlier history had a `.aab`/`.apk` checked in by mistake.
