# Product Decisions — Rade Keyboard

Non-obvious product/technical decisions worth remembering, so they don't get
"fixed" by a later contributor who lacks the context. Use `/retro` to surface new ones.

<!-- Format:
## YYYY-MM-DD - [Decision]

**Context:** why this came up
**Decision:** what we chose
**Why:** the reasoning / trade-off
-->

## 2026-07-07 - Default keyboard language is Vietnamese

**Context:** The app is a Vietnamese/Rade keyboard; the onboarding offers an EN/VI toggle.
**Decision:** The persisted default (`SharedPreferences` `selected_language`) is `vi`.
**Why:** The target audience is Vietnamese/Rade speakers; English is the fallback, not the default.

## 2026-07-07 - Reuse the existing Play upload key (do not regenerate)

**Context:** The app is already live on the Play Store, so an upload key was created
during the first release. Signing was never wired into Gradle (releases were signed via
Android Studio's wizard), so the key's location is easy to forget.
**Decision:** Always reuse the existing upload key. Never `keytool -genkey` a new one for
this app.
**Why:** Play registers the original upload key; a new key is rejected ("signed with the
wrong key"). Recovering/rotating a lost key requires a Play App Signing upload-key reset.
**Known facts** (password is NOT stored here — password manager only):
- Keystore: `~/Desktop/Important Documents/radekeyboard-keystore`
- Format: PKCS12 → store password and key password are the same single value.
- Key alias: `radekeyboard-key`
- The keystore IS password-protected (an empty password was verified incorrect).
- PKCS12 gotcha: `keytool -list` shows the alias even with a wrong password (prints an
  "integrity NOT verified" warning) — listing is not proof the password is blank.
- Recovered from the original clone's `.idea/workspace.xml` (`GenerateSignedApkSettings`).

## 2026-07-07 - Vibration/haptics removed

**Context:** Earlier commits added key-press vibration, then a commit titled "Remove
vibration" landed but left the vibration code (and `VIBRATE` permission) in place.
**Decision:** Vibration is fully removed — no haptic feedback on key press, and the
`VIBRATE` permission is dropped.
**Why:** Aligns the code with the stated intent; keeps the app permission-light
(fewer permissions = smoother Play review and more user trust). Re-adding haptics
should be a deliberate, opt-in feature, not silent.

## 2026-07-07 - Keyboard height is constant across layouts (symbol rows stretch)

**Context:** On the Samsung S24 Ultra the symbols view "glitched out" — toggling layouts
resized the IME window mid-session (5 rows ↔ 4 rows), which One UI's soft-input
resize/inset path mishandles (clipped/stale frame).
**Decision:** `keyboardHeight` is always derived from the QWERTY row count; the symbol
layout's 4 rows stretch taller to fill the same total height (Gboard-style).
**Why:** Removing the mid-session window resize makes the OEM bug path unreachable — the
most robust fix. Do not revert to per-layout heights.

## 2026-07-07 - Vibration re-added as an opt-in setting (supersedes "Vibration/haptics removed")

**Context:** User requested key-press haptics with a toggle + duration selector as part of
the v2 settings menu. The earlier removal decision explicitly reserved room for a
"deliberate, opt-in" re-introduction.
**Decision:** Haptics behind `pref_haptics_enabled` (default OFF), duration selectable
10/20/40 ms; `VIBRATE` permission restored; `VibrationEffect` on API 26+, legacy fallback
down to minSdk 21.
**Why:** Deliberate opt-in keeps default behavior and Play-review posture unchanged while
giving users who want feedback a supported path.

## 2026-07-07 - Language layers (RD/VN/ENG) switch internally, not via system subtypes

**Context:** v2 adds three layers switched by spacebar swipe. Rade has no ISO 639-1 code
(`rad` is ISO 639-3 and renders poorly as a `Locale`), and syncing internal layers with
system subtypes creates two sources of truth.
**Decision:** The active layer is app state (`app_prefs`/`keyboard_layer`), shown as a
permanent spacebar label; `method.xml` stays `vi` + `en_US` and subtype changes are ignored.
**Why:** One source of truth and a clean Rade story. Tradeoff: the system globe key/subtype
UI doesn't reflect the layer.

## 2026-07-07 - VN layer emits precomposed NFC; Rade keeps decomposed combining marks

**Context:** `VietnameseText` (RD layer) commits base char + combining mark (decomposed).
The new Telex engine for the VN layer needed an output convention.
**Decision:** `TelexComposer` outputs NFC-normalized precomposed characters (e.g. `ế`
U+1EBF); the RD layer's decomposed style is unchanged. The composer NFD-tolerates mixed
input from mid-word layer switches.
**Why:** NFC is what other apps, spell checkers, and search expect for Vietnamese; changing
RD's convention would risk regressions in the shipped Rade behavior.

## 2026-07-07 - Telex tone placement uses modern "new style" orthography

**Context:** Vietnamese has two tone-placement conventions for glide clusters: old style
(`hòa`, `túy`) vs new style (`hoà`, `thuý`).
**Decision:** `TelexComposer` places tones new-style: `oa`/`oe`/`uy` clusters carry the tone
on the second vowel.
**Why:** Matches contemporary keyboards (Gboard, Laban Key defaults) and school orthography;
documented in a class comment so it isn't "fixed" later.

## 2026-07-07 - Keyboard theme defaults to LIGHT; dark is opt-in

**Context:** The v2 build followed system dark mode (palette logic from the
better-ui merge) and the user's dark-mode phone rendered the keyboard black —
"gross" compared to the light gray it always shipped with.
**Decision:** `pref_theme` defaults to "light"; "dark" and "system" are choices in
the keyboard settings panel. One shared palette (`KeyboardTheme`) drives the
keyboard, suggestion strip, popups, and settings panel.
**Why:** Never change the product's established look out from under users;
following the OS is a preference, not a default.

## 2026-07-07 - Keyboard settings are an in-keyboard overlay, not an Activity

**Context:** The first settings implementation was a SettingsActivity; the user
rejected it ("rendered as a separate app/screen instead of just an overlay").
**Decision:** The gear key swaps a `SettingsPanelView` into the IME window at
exactly the keyboard's current size; Done restores the keyboard and applies
changes immediately. SettingsActivity was deleted.
**Why:** Keyboard options belong in the keyboard; same-size swap also preserves
the constant-window-height invariant.
