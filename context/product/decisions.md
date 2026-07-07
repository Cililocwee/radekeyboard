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
