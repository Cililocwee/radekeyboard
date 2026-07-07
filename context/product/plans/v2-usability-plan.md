# Rade Keyboard v2 — Smoothness, Settings, Symbol Fix, Language Layers, Prediction

## Context

Rade Keyboard (native Android IME, Java, minSdk 21 / targetSdk 35) ships a single Canvas-drawn
QWERTY layout with Rade long-press alternates. This round: (1) smoother typing + an options
menu, (2) fix the symbols view that glitches out on the Samsung S24 Ultra, (3) three language
layers — RD (current), VN (standard Vietnamese w/ Telex), ENG — switched by swiping the space
bar, (4) text prediction research + a Phase-1 implementation, (5) a durable improvement
backlog. Everything must be persisted as planning/state documents under
`context/product/plans/` so a different model can pick the work up.

**Confirmed user decisions:** VN layer = Telex **plus** long-press accent popups as fallback ·
number row ON by default, toggleable · prediction includes a Phase-1 implementation.

## Verified codebase findings

- `ModernKeyboardView.java` (~1023 lines): `QWERTY_LAYOUT` = 5 rows incl. number row (row 0);
  `SYMBOL_LAYOUT` = 4 rows, bottom row `{"ABC", ",", " ", ".", "ENTER"}`. Static `RADE_ALTS`
  (lines 60–100) + `ALT_PREVIEW_COUNT` maps; referenced at ~6 call sites (drawKey preview,
  startLongPressTimer, showLongPressPopup, handleAlternativeCommit, handleKeyPress).
- `toggleSymbolMode()` (line ~977): flips flag → `calculateDimensions()` (height derives from
  row count → **IME window resizes 5↔4 rows mid-session**) → `createKeys()` → `invalidate()` →
  `requestLayout()`. `onMeasure` returns cached `keyboardHeight`, ignores spec. `onSizeChanged`
  calls `createKeys()` but **not** `invalidate()`. `createKeys()` early-returns at width 0.
- Touch (`onTouchEvent` ~557–628): **single pointer only** (`event.getX()`), exact bounding-box
  hit test (gaps = dead zones), `LONG_PRESS_DELAY = 200ms`, ACTION_MOVE off-key cancels the
  press. 100ms `ValueAnimator` with a single shared `pressScale`. No gesture detection.
- No haptics / no VIBRATE permission (deliberately removed — decision to be superseded).
- No settings UI. Prefs: `"app_prefs"` with `"selected_language"` (onboarding UI locale only —
  do NOT reuse that key).
- Service: `handleKeyPress` (punctuation-space replacement → `VietnameseText` combining-mark
  path → post-hoc shift uppercase → `commitText`); `handleSpecialKey` has `if (ic == null)
  return;` at line 166 **before** the switch. No `onStartInputView`/lifecycle overrides. Enter
  sends raw KEYCODE_ENTER (no `performEditorAction` — backlog item). Delete-and-recommit is the
  established tone-mark pattern.
- `VietnameseText`: pure, emits **decomposed** combining marks (U+0300/01/02/03/09/23, breve
  U+0306). No Telex. `VietnameseTextTest` proves the pure-class JUnit4 pattern.
- `method.xml`: subtypes `vi` + `en_US`; subtype switching unhandled (both identical).
- `context/product/plans/` is empty; `decisions.md` has 3 entries incl. "vibration removed".

---

## Workstream 1 — Symbol-view glitch fix (S24 Ultra) — DO FIRST

**Root cause:** every symbol toggle resizes the IME window (~52dp) mid-session; One UI's
soft-input window resize/inset path mishandles it (clipped/stale frame). Compounded by:
`onSizeChanged` without `invalidate()`; stale `keys` list when `createKeys()` early-returns at
width 0 while `isSymbolMode` already flipped; zero `WindowInsets` handling despite targetSdk 35
(Android 15 enforced edge-to-edge).

**Fix — constant keyboard height across layouts** (makes the OEM resize path unreachable):
1. `calculateDimensions()`: derive `keyboardHeight` from the QWERTY row count
   (`KeyboardLayouts.qwertyRows(numberRowEnabled).length`), never from symbol mode.
2. `createKeys()`: per-row height = `(keyboardHeight - topMargin - keyMargin*rows) / rows` for
   the *current* layout — symbol's 4 rows stretch ~25% taller (Gboard-style), same total.
3. `onSizeChanged`: add `invalidate()` after `createKeys()`.
4. `onDraw` stale-keys guard: if `keys.isEmpty() && getWidth() > 0` → `createKeys()` first.
5. Bottom insets: `onApplyWindowInsets` (API 30+: `WindowInsets.Type.systemBars()|
   mandatorySystemGestures()`; API 21–29: `getSystemWindowInsetBottom()`); store, and
   `onMeasure` returns `keyboardHeight + bottomInset` (background paint already covers it).
   Apply only when inset > 0; `requestApplyInsets()` in `onAttachedToWindow`.
6. `onMeasure`: honor an `EXACTLY` heightMeasureSpec defensively.

Decision-log entry: "Keyboard height constant across layouts; symbol rows stretch."

## Workstream 2 — Typing smoothness

New pure helper **`KeyGeometry.java`** + `KeyGeometryTest`: `nearestKeyIndex(...)` — clamp
point to each key rect, min squared distance (exact hits = 0). `findKeyAt` falls back to it →
no dead zones between keys.

`onTouchEvent` rewrite (use `getActionMasked()` + pointer-id tracking; fields
`activePointerId`, `downX/downY`, `touchSlop = ViewConfiguration.getScaledTouchSlop()`):
- **`ACTION_POINTER_DOWN` commits the currently pressed key immediately** (the fix for fast
  two-thumb typing; if long-pressing, commit popup selection instead), then the new pointer
  becomes active + presses its key.
- `ACTION_MOVE`: use `findPointerIndex(activePointerId)` coords only. Within touch slop: ignore
  wobble. Beyond slop: *slide* the press to the neighbor key (restart long-press timer) instead
  of dropping to null.
- `ACTION_POINTER_UP` of the active pointer: commit like UP, reset long-press/continuous-delete
  state, set `activePointerId = INVALID` (don't retro-press a resting finger). Non-active: ignore.
- Keep commit-on-UP for the active pointer (long-press popups/tone marks depend on it).
- **Remove `ValueAnimator`/`pressScale`/`animateKeyPress`** (shared-state glitches under rapid
  multi-touch); instant pressed-color highlight (already drawn via `key == pressedKey`).
- `LONG_PRESS_DELAY` 200 → 300ms (200 triggers accidental popups for fast typists).
- Haptics on key-down (`ACTION_DOWN`, `ACTION_POINTER_DOWN`, popup-open): cached `Vibrator`;
  API ≥ 26 `VibrationEffect.createOneShot(duration, DEFAULT_AMPLITUDE)`, else legacy
  `vibrate(ms)`. Reads cached fields only (never SharedPreferences per keystroke). Inert until
  Workstream 3 lands (default off).

## Workstream 3 — Settings menu

**`SettingsActivity`** (not an in-keyboard overlay: standard widgets + accessibility, and
avoids the exact window-resize path Workstream 1 eliminates).

- **`KeyboardPrefs.java`**: static wrapper over existing `"app_prefs"`. Keys:
  `pref_haptics_enabled` (bool, default **false**), `pref_haptic_duration_ms` (int, default 20;
  UI choices 10/20/40 = Light/Medium/Strong radio group, enabled only when haptics on),
  `pref_number_row` (bool, default **true**), and (Workstream 4) `keyboard_layer`
  (`"rd"|"vn"|"en"`, default `"rd"`).
- `activity_settings.xml`: switches + radio group; write-through on change. EN strings +
  `values-vi/` translations.
- Manifest: re-add `<uses-permission android:name="android.permission.VIBRATE"/>`; register
  activity (`exported="false"`).
- **Cog key**: SYMBOL bottom row → `{"ABC", "SETTINGS", ",", " ", ".", "ENTER"}`; new constant
  `KEY_SETTINGS = -10`; Material gear vector `ic_settings.xml`; wire into
  `shouldUseDrawable`/`getDrawableForKey`/`isSpecialKey`/view `handleKeyPress`.
- Service: `case KEY_SETTINGS` **placed before the `ic == null` guard** in `handleSpecialKey`
  (must open without an input connection):
  `Intent` + `FLAG_ACTIVITY_NEW_TASK` + `startActivity` + `requestHideSelf(0)`.
- New `onStartInputView` override: `keyboardView.refreshFromPrefs()` +
  `updateAutoCapitalization()` (also fixes stale auto-caps on refocus). Number-row changes
  apply on keyboard reopen — the safe re-measure path.
- **Number row off** (via pure **`KeyboardLayouts.java`** + `KeyboardLayoutsTest`):
  `qwertyRows(false)` drops row 0 (4-row QWERTY, height follows automatically);
  `effectiveAlternates(label, baseAlts, numberRowEnabled)` prepends the key's digit
  (q→1 … p→0) to long-press alternates — **never mutates static maps**; digit becomes
  `alternates[0]` so the existing preview drawing shows it above q..p, and long-press default
  commit is the digit.
- Replace **every** `RADE_ALTS.get/containsKey` call site with an instance `effectiveAlts()`
  helper; final `grep RADE_ALTS` to confirm only the definition + helper remain.
- Decision-log entry: vibration re-added opt-in (supersedes the removal decision).

## Workstream 4 — Language layers RD / VN / ENG + space-bar swipe

New files (all pure Java except view/service edits):

- **`KeyboardLayer.java`** — enum `RADE("rd","Ê Đê")`, `VIETNAMESE("vn","Tiếng Việt")`,
  `ENGLISH("en","English")` with `next()`/`previous()`/`fromPrefValue()` (default RADE).
  Persisted via `KeyboardPrefs` key `keyboard_layer`.
- **`LayerAlternates.java`** — per-layer alt + preview-count maps:
  `alts(KeyboardLayer)` / `previewCounts(KeyboardLayer)`.
  - RD: current `RADE_ALTS`/`ALT_PREVIEW_COUNT` moved verbatim (combining-mark strings —
    copy byte-identical). RD-only.
  - VN: precomposed accent popups (Telex fallback): `a→{â,ă,à,á,ả,ã,ạ}`,
    `e→{ê,è,é,ẻ,ẽ,ẹ}`, `o→{ô,ơ,ò,ó,ỏ,õ,ọ}`, `u→{ư,ù,ú,ủ,ũ,ụ}`, `i→{ì,í,ỉ,ĩ,ị}`,
    `y→{ỳ,ý,ỷ,ỹ,ỵ}`, `d→{đ}` (most-used first so preview index 0 is â/ê/ô/ư/đ);
    g/h/j/k/l get plain symbols; drop the `","→"˘"` breve entry.
  - ENG: symbols/digits only (RD map minus all Vietnamese/combining entries).
- **`TelexComposer.java`** + **`TelexComposerTest.java`** — pure state machine:
  `Edit process(String wordBeforeCursor, char key)` → `{deleteCount, commit}` (NFC output —
  VN mode emits **precomposed NFC**, what other apps expect; `VietnameseText`'s decomposed
  style stays RD-only). Rules: `aa→â, ee→ê, oo→ô, aw→ă, ow→ơ` (incl. `uo+w→ươ`, tones
  preserved), `uw→ư, dd→đ`; tone keys `s/f/r/x/j` = sắc/huyền/hỏi/ngã/nặng; repeated
  modifier/tone key reverts to literal (`á`+`s`→`as`, `ô`+`o`→`oo` — how "xoong" is typed);
  `z` strips tone; lone `w` stays literal; rules no-op without a plausible syllable.
  **Tone placement: modern "new style"** (`hoà`, `thuý`): rightmost quality-marked vowel →
  else last vowel if final consonant → else first of two vowels except `oa/oe/uy` → second;
  `qu`/`gi` glides excluded from the nucleus. Case-insensitive matching, case-preserving
  output (`VIEETS`→`VIẾT`). ~20 test cases specified: `vieets→viết`, `ddaay→đây`,
  `nguwowfi→người`, `nguowfi→người`, `xooong→xoong`, `hoaf→hoà`, `thuys→thuý`, `cuar→của`,
  `toans→toán`, `muoons→muốn`, `ass→as`, `asz→a`/`az→az`, `asf→à`, caps, `quys→quý`,
  `gif→gì`, `ss→ss`, NFC assertion (`viết`), decomposed-input tolerance
  (`"ê"`+`s`→`ế`, deleteCount 2).
- **Service integration (VN branch):** read trailing word via `getTextBeforeCursor(32,0)`
  (letters + NON_SPACING_MARK; null → plain commit fallback); resolve shift case **before**
  the composer and consume one-shot shift (post-hoc `toUpperCase()` would corrupt multi-char
  recommits); apply edit inside `beginBatchEdit()`/`endBatchEdit()` with
  `deleteSurroundingText(deleteCount,0)` + `commitText`. **No composing region** (matches the
  codebase pattern; avoids lifecycle cleanup; documented tradeoff). ENG branch: plain commit.
  RD branch: existing path verbatim. `updateAutoCapitalization()` unchanged (all layers).
- **Space-bar swipe:** listener gains `onLanguageSwipe(int direction)`. On DOWN on the `" "`
  key: hold a direct `Key` reference, record `spaceDownX`, track that pointer. On MOVE (branch
  placed **before** the leave-key-cancel branch, and only for the pointer that pressed space):
  past threshold `0.25f * spaceKey.width` → slide mode (cancel pending space/long-press),
  live-preview target layer name on the spacebar (`primaryColor`, chevrons); direction may
  flip until UP. On UP: fire `onLanguageSwipe(±1)`, commit **no space**; otherwise normal
  space. Reset on CANCEL. Service cycles `next()`/`previous()`, persists, calls
  `keyboardView.setLanguageLayer(...)`.
- **Spacebar label:** always draw the active layer's label centered on the space key (~0.75x
  text size, ~55% alpha) — Gboard-style indicator.
- **Subtypes: keep internal-only switching.** `method.xml` unchanged; no
  `onCurrentInputMethodSubtypeChanged`. Rade has no ISO 639-1 code (`rad` renders ugly/blank
  locale names), and syncing 3 layers with system subtypes creates two sources of truth.
  Tradeoff (system globe key won't reflect the layer) documented in a `method.xml` comment.

## Workstream 5 — Text prediction (research done; implement P0+P1)

Research verdicts (full cited report saved as
`context/product/plans/text-prediction-research.md` in step 0): **reject** AOSP LatinIME JNI
graft (15–25 days, NDK, JVM-untestable), KenLM/TFLite (size/effort vs. a hobby IME),
`setCandidatesViewShown` (janky, OEM-inconsistent). Keyboard-grade quality needs only
frequency unigrams + bigrams (<10 MB even at Gboard scale; we need ~1–2 MB heap, +~400 KB APK).

**P0 — Suggestion strip UI (1–2d):** `onCreateInputView()` returns a vertical `LinearLayout`:
`SuggestionStripView` (3 theme-aware slots, Canvas or TextViews, night-mode palette) above
`ModernKeyboardView`. Always present (no window-height jumps — coordinates with Workstream 1).
Strip sits **above the number row**. Tap → callback into service. Dummy data to validate.

**P1 — Folded prefix completion (3–5d), pure Java + unit tests:**
- `DiacriticFolder`: NFD → strip `\p{Mn}` → `đ/Đ→d` (đ doesn't decompose!). Folded `nguoi`
  matches `người`. Handles both this IME's decomposed output and NFC from other apps.
- `WordDictionary`: sorted folded-key `String[]` + parallel freq array; two binary searches per
  prefix lookup (µs — UI-thread safe); loaded from gzipped `assets/dict/{vi,en}.txt` on a
  background thread at service `onCreate()` (strip empty until ready).
- `SuggestionEngine`: current-word extraction, top-3 by frequency, exact-diacritic matches
  ranked above folded; refresh on key events + `onUpdateSelection`.
- Commit: `beginBatchEdit` + `deleteSurroundingText(wordLen,0)` + `commitText(word+" ",1)`.
- **Privacy:** check `EditorInfo` in `onStartInput`; disable suggestions for password /
  `NO_SUGGESTIONS` fields.
- Dictionary selected by active layer (vi ↔ VN, en ↔ ENG; RD initially reuses vi or ships a
  small flat-frequency Rade list — engine is "wordlist asset + folding rule" so Rade slots in).
- Data prep (one-off script, not shipped): HermitDave FrequencyWords `vi_50k`/`en_50k`
  frequencies ∩ hunspell-vi / Viet74K validity (check hunspell-vi GPL lineage before
  embedding — if problematic, use Viet74K + frequency list alone).

**Later phases (documented, not this round):** P2 bigram next-word (highest payoff for
Vietnamese — syllable compounds like `Việt→Nam`); P3 user-history adaptive dictionary
(first persisted data beyond prefs — needs a decision-log entry; also the long-term Rade
frequency story).

## Cross-workstream integration

- **Alternates pipeline (3 workstreams touch it):** effective alts =
  `KeyboardLayouts.effectiveAlternates(label, LayerAlternates.alts(layer).get(label),
  numberRowEnabled)` — layer map first, then digit prepend. Single instance method
  `effectiveAlts(label)` in the view; all call sites go through it.
- **Touch rewrite × space swipe:** implement the multi-touch rewrite first; space-swipe tracks
  the *pointer id* that pressed space (not global X), and `ACTION_POINTER_DOWN` while sliding
  should be ignored (gesture owns the interaction).
- **Strip × height:** the strip is a sibling view above the keyboard — `keyboardHeight` math
  untouched; IME window grows once at creation, never mid-session.
- **Prefs:** one file (`app_prefs`), all access through `KeyboardPrefs`.

## Implementation order (commit checkpoints per workflow conventions)

0. **Persist planning docs** (first commit): `context/product/plans/v2-usability-plan.md`
   (this plan), `context/product/plans/text-prediction-research.md` (full cited research
   report from the research agent), and the improvement backlog below as
   `context/product/plans/improvement-backlog.md`. Log new decisions in
   `context/product/decisions.md` (constant height; vibration opt-in supersedes removal;
   internal-only layer switching; VN emits NFC; new-style tone placement).
1. Pure helpers + tests: `KeyboardLayouts`, `KeyGeometry` (+ move layouts out of the view).
2. Workstream 1 glitch fix → `make build`, device-verify symbol toggle on the S24 Ultra early.
3. Workstream 2 touch rewrite (haptics inert).
4. Workstream 3 settings (prefs, activity, manifest, cog, number row).
5. Workstream 4: layer enum + spacebar label (inert) → `LayerAlternates` refactor (RD
   byte-identical) → `TelexComposer` + tests green → service branching → swipe gesture →
   VN/ENG alt polish.
6. Workstream 5: P0 strip → P1 engine + dictionaries.
7. Final: `make build && make test && make lint`; grep for stray `Log.d` (IME privacy rule).

Each step ends with `make test` + `make build`; steps 2–6 are independently shippable.

## Verification

- **JVM tests:** `KeyboardLayoutsTest` (row selection; digit prepend incl. p→0; null base;
  no-mutation on repeat calls), `KeyGeometryTest` (exact/gap/corner/below-bottom),
  `TelexComposerTest` (~20 cases above), `DiacriticFolderTest` (đ, decomposed+NFC inputs),
  `SuggestionEngineTest`/`WordDictionaryTest` (prefix ranking, folded vs exact).
- **Device matrix** (S24 Ultra Android 15 + one API ≤ 25 emulator for legacy vibrate):
  rapid symbol toggles mid-word (no blanking; same height); fast two-thumb typing (no drops;
  second thumb commits first key); taps in key gaps; finger wobble + slide-to-neighbor;
  300ms tone popups + slide-select; DELETE word/continuous unchanged; cog → settings →
  toggles persist → reopen picks up number-row-off (4 rows, digit previews, long-press q
  commits "1"); vibration off/10/20/40; spacebar swipe both directions across all three
  layers + persistence across keyboard restarts; VN Telex typing incl. auto-caps and
  punctuation spacing; ENG plain typing; suggestion strip completions incl. bare-ASCII
  `nguoi→người`, tap-to-commit, hidden in password fields; dark mode; gesture nav vs
  3-button nav; rotation.

## Key risks

- Symbol keys ~25% taller (intentional; tell the user). Popup positioning after taller rows —
  verify tone popups post-Workstream-1.
- `ACTION_POINTER_UP` resets must mirror `ACTION_UP` or continuous delete wedges on.
- Telex hijacks non-Vietnamese words in VN mode (`for`→`fỏ`) — inherent; same-key revert is
  the standard escape; document, don't "fix".
- Mid-word layer switches leave mixed decomposed/NFC text — composer NFC-normalizes input and
  whole-word-replaces when combining marks present (test #20).
- Missed `RADE_ALTS` call site → preview/popup mismatch; enforce with final grep.
- Inset double-gap on 3-button nav; hunspell-vi license (GPL lineage) — verify before embedding.

---

## Improvement backlog (item 5)

Moved to its own living document: [`improvement-backlog.md`](improvement-backlog.md).

