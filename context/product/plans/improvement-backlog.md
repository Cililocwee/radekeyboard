# Improvement Backlog — Rade Keyboard

Living document. Ideas beyond the v2 round (see [`v2-usability-plan.md`](v2-usability-plan.md)),
ranked within each category. Suggested next picks after v2 ships: **1, 2, 7, 13, 3, 23, 24**.

## Correctness / platform quick wins (high impact, small)

1. **Enter-key action support** — use `ic.performEditorAction()` when `EditorInfo.imeOptions`
   defines Search/Send/Go/Next/Done, and draw the matching label/icon on ENTER. Raw
   KEYCODE_ENTER breaks "Search" in some apps today.
2. **inputType-aware behavior** — numeric/phone/date fields → open in symbol/number layer (or a
   dedicated numpad); disable auto-caps in email/URI/password fields (use
   `InputConnection.getCursorCapsMode()` + `TYPE_TEXT_FLAG_CAP_SENTENCES` instead of string
   sniffing `". "`).
3. **Double-space → period + space** (toggleable) — standard muscle memory.
4. **`onEvaluateFullscreenMode` override** — avoid the ugly fullscreen extract UI in landscape.
5. **Landscape height tuning** — ~260dp of keyboard is most of a landscape screen; scale
   `keyHeight` down in landscape.
6. **Auto-caps correctness** — respect the field's `initialCapsMode`; currently fires even in
   fields that ask for none.

## Typing & gestures

7. **Cursor-control gesture** — long-press space (or a vertically-locked spacebar slide mode) →
   move the cursor left/right. Huge for fixing typos; pairs naturally with the swipe work.
8. **Swipe-left on DELETE** — delete whole word with a gesture (complements long-press).
9. **Key preview bubble** — small per-key popup on press (classic feedback; optional setting).
10. **Sound on keypress** (toggleable, `AudioManager.playSoundEffect`) — pairs with haptics.
11. **Autocorrect** — after prediction P1/P2: auto-replace on space when the top candidate score
    clears a threshold, with the strip offering the original as undo. Vietnamese diacritic
    restoration (bare ASCII → full diacritics) falls out of the folded prediction engine.
12. **VNI input option** — some Vietnamese typists prefer numbers (`a1`→á); the TelexComposer
    architecture supports a sibling `VniComposer` behind a setting.

## Rade language depth (the differentiator)

13. **Rade orthography audit** — verify every Rade grapheme is reachable: č, ĕ, ĭ, ŏ, ŭ, ư̆,
    ñ, đ, etc. Current alts cover ê/ư/ô/ơ/ă/â/đ/ñ + tone marks; audit against a Rade alphabet
    reference (SIL materials) with a native-speaker review; add missing alternates.
14. **Rade wordlist + prediction** — hand-curated 1–5K word flat-frequency list (SIL / the
    Vietnamese–Ede bilingual vocabulary database) feeding the same prediction engine;
    user-history adaptation (prediction P3) builds real frequencies over time.
15. **Rade subtype label** — if system integration ever matters: a `vi` subtype +
    `imeSubtypeExtraValue="rade"` + custom label (documented in the v2 plan, not planned).

## Features

16. **Emoji layer** — even a small curated grid beats none.
17. **Clipboard key / history strip** — show last clipboard item(s) in the suggestion strip on
    focus (Gboard-style); privacy-sensitive, opt-in.
18. **One-handed mode** — shrink + anchor left/right.
19. **Keyboard height & bottom-padding settings** — user-adjustable size (the settings screen
    from v2 hosts it).
20. **Themes** — accent-color picker + key-border toggle on top of the existing night-mode
    palette.
21. **Onboarding step for gestures** — teach spacebar swipe + long-press tone marks in the
    existing 3-step wizard (EN/VI strings exist; add a layer-switch demo).
22. **Voice-input key** — delegate to the system voice IME; low effort, often requested.

## Engineering health

23. **Decompose `ModernKeyboardView`** — extract TouchController, KeyboardRenderer,
    LayoutModel; the monolith is the biggest velocity tax (v2 already extracts
    layouts/geometry/alternates — finish the job).
24. **CI** — GitHub Actions: `./gradlew testDebugUnitTest lint assembleDebug` on PRs
    (`android-actions/setup-android`).
25. **Instrumented smoke test** — one `connectedAndroidTest` that opens the IME and types a
    tone-marked word end-to-end; catches regressions no JVM test can.
26. **Version-bump discipline automation** — a check that `versionCode` increased when release
    artifacts are built (learnings.md gotcha).
27. **Play listing refresh** — screenshots showing the new layers/settings/prediction after v2.
