---
alwaysApply: true
---

# Testing Strategy — Rade Keyboard

## Current State

Historically the project had only the generated `ExampleUnitTest` /
`ExampleInstrumentedTest` stubs. Real coverage starts with `VietnameseTextTest`
(pure tone-mark/combining logic). Grow coverage by **extracting pure logic and
testing it**, not by trying to instrument the whole IME.

## Tiers

| Tier | Location | Command | Runs on | Tests |
|------|----------|---------|---------|-------|
| Unit (JVM) | `app/src/test/java/` | `make test` (`./gradlew testDebugUnitTest`) | Local JVM, no device | Pure logic (`VietnameseText`), any framework-free helper |
| Instrumented | `app/src/androidTest/java/` | `./gradlew connectedAndroidTest` | Device/emulator | End-to-end IME behavior, activity flows |

## What to Test

- **Do test:** tone-mark detection, combining-character mapping, vowel rules,
  word-boundary/delete logic, punctuation-spacing rules — anything you can express
  without a `Context`.
- **Extract, then test:** if useful logic is trapped inside `ModernInputMethodService`
  or `ModernKeyboardView`, pull it into a pure helper (like `VietnameseText`) and
  cover the helper. This is the primary way to raise coverage here.
- **Don't unit-test:** `Canvas` drawing, touch dispatch, `InputConnection` calls,
  system-settings reads (`KeyboardSetupChecker`). These need a device — cover them
  with instrumented tests or manual verification only if they become fragile.

## Mock Boundary

There is no DI framework and no network. The natural seam is the **pure helper**:
call it directly with plain inputs and assert on plain outputs. For code that must
touch the framework, the boundary is the device (instrumented test), so keep that
code thin.

## Before Handoff

Run `make test` (and `make build`) before requesting review. If you added
text-transformation behavior without a corresponding unit test, that's a gap — close
it.
