---
alwaysApply: true
---

# Code Conventions — Rade Keyboard (Android / Java)

This is a single-module Android app (`com.android.application`). There is no
router/service/CRUD layering — the meaningful boundaries are between the **view**
(drawing + touch), the **service** (text manipulation), and **pure helpers**.

## Component Boundaries

| Layer | File(s) | Responsibility | Must NOT do |
|-------|---------|----------------|-------------|
| View | `ModernKeyboardView` | Draw keys on `Canvas`, handle touch, show long-press popups, emit key events via `OnKeyPressListener` | Touch `InputConnection` or commit text |
| Service | `ModernInputMethodService` | Receive key events, transform text, commit via `InputConnection`, manage shift/caps/auto-cap | Own drawing or layout math |
| Pure helper | `VietnameseText` | Tone-mark detection, combining-character mapping, vowel checks — no Android imports | Reference `Context`, `View`, or any framework type |
| Activities | `MainActivity`, `OnboardingActivity` | Setup flow, routing, language toggle | Contain keyboard logic |
| Setup | `KeyboardSetupChecker` | Read `InputMethodManager` / `Settings.Secure` state | Mutate settings |

**Data flow:** `ModernKeyboardView` (touch) → `OnKeyPressListener` → `ModernInputMethodService` → `InputConnection`.

## Adding a Key / Layout Change

1. Edit the relevant `String[][]` layout in `ModernKeyboardView` (`QWERTY_LAYOUT` /
   `SYMBOL_LAYOUT`).
2. If the key has long-press alternates, add them to `RADE_ALTS` and (optionally) a
   preview count to `ALT_PREVIEW_COUNT`.
3. Special keys (SHIFT/DELETE/ENTER/etc.) are dispatched in `handleKeyPress(Key)` by
   label — add a `case` there and a matching `KEY_*` constant if it's a new special key.
4. Handle the resulting key in `ModernInputMethodService.handleKeyPress` /
   `handleSpecialKey`.

## Text Transformation

- **Put pure logic in `VietnameseText`** (tone marks, combining chars, vowel rules)
  and unit-test it. The service should call the helper, not re-implement the rules.
- Vietnamese tone marks combine onto the *preceding* base character — the base char
  is deleted and re-committed with the combining code point appended. Order matters
  (base first, then the `\u03xx` combining mark). See `learnings.md`.

## Android Specifics

- **minSdk 21** — guard any API newer than 21 with `Build.VERSION.SDK_INT` checks
  (there are existing examples).
- **No debug logging in committed code.** Diagnostics must be removed or gated; the
  IME runs against every app on the device and `Log.d` leaks user context.
- **Manifest namespace** comes from `build.gradle` (`namespace`), not a `package=`
  attribute — don't reintroduce the attribute.
- **Permissions:** only declare what's used. The app is intentionally
  permission-light; adding one requires a real, referenced call site.

## Verifying New APIs / Library Behavior

Android APIs vary sharply by SDK level. When using an API you're not certain about at
minSdk 21, verify with **context7** (or the official Android docs) rather than
assuming — especially around `InputConnection`, `InputMethodManager`, and
`VibrationEffect`-style version-gated calls.
