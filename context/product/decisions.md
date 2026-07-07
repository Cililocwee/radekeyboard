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

## 2026-07-07 - Vibration/haptics removed

**Context:** Earlier commits added key-press vibration, then a commit titled "Remove
vibration" landed but left the vibration code (and `VIBRATE` permission) in place.
**Decision:** Vibration is fully removed — no haptic feedback on key press, and the
`VIBRATE` permission is dropped.
**Why:** Aligns the code with the stated intent; keeps the app permission-light
(fewer permissions = smoother Play review and more user trust). Re-adding haptics
should be a deliberate, opt-in feature, not silent.
