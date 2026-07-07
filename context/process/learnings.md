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

## v2 Round (2026-07-07)

- **The Samsung symbol-view glitch was a mid-session IME window resize.** The old
  code derived keyboard height from the current layout's row count (5 QWERTY vs 4
  symbols), so toggling layouts resized the IME window while open — a path One UI
  mishandles. Keyboard height must stay constant across layout toggles (symbol rows
  stretch); only settings applied on keyboard reopen may change it.
- **The old press ValueAnimator drew nothing.** Both branches of the pressed-rect
  math rendered the full key rect; the animator just burned an invalidate per frame
  and shared one scale across keys. Instant color highlight replaced it.
- **`Comparator.comparing` / `List.sort` need API 24** — minSdk is 21. Lint's NewApi
  check catches this only if you run `make lint`; run it before handing off.
- **Multi-touch commits on ACTION_POINTER_DOWN.** Real keyboards commit the first
  key the moment a second finger lands. Track one active pointer id; never
  retro-press a finger that was merely resting when the active one lifts.
- **Telex output is NFC; Rade output is decomposed.** `TelexComposer` (VN layer)
  emits precomposed NFC and NFD-normalizes its input, so mid-word layer switches
  survive. Don't "unify" the two conventions — other apps expect NFC Vietnamese,
  and the shipped Rade behavior expects base+combining commits.
- **`onUpdateSelection` is the one hook that keeps a suggestion strip in sync** —
  it fires after every commit, delete, and cursor tap; no need to sprinkle refresh
  calls through the key handlers.
- **HermitDave FrequencyWords is CC-BY-SA** — attribution lives in the gzipped
  asset headers (`assets/dict/*.txt.gz`, `#` comment lines); keep it when
  regenerating dictionaries.
