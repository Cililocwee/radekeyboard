package com.corriestroup.radekeyboard;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure (Android-free) Telex input engine for the Vietnamese layer.
 *
 * <p>Given the word currently before the cursor and the typed key, it returns the
 * edit to apply: how many UTF-16 code units to delete and what to commit. When no
 * Telex rule fires, the edit is simply "insert the key" — the common fast path.
 *
 * <p>Rules (standard Telex): {@code aa→â ee→ê oo→ô aw→ă ow→ơ uw→ư dd→đ}
 * (with {@code uo+w → ươ}); tone keys {@code s=sắc f=huyền r=hỏi x=ngã j=nặng};
 * {@code z} removes the tone. Typing the same modifier again reverts it and emits
 * the key literally ({@code á + s → as}, {@code ô + o → oo} — how "xoong" is
 * typed). A lone {@code w} stays a literal w.
 *
 * <p>Output is precomposed NFC — what other apps expect for Vietnamese. This
 * intentionally differs from the Rade layer's decomposed combining-mark style
 * ({@code VietnameseText}); input is NFC-normalized first so mid-word layer
 * switches don't break composition.
 *
 * <p>Tone placement follows the modern "new style" ({@code hoà}, {@code thuý}):
 * the rightmost quality-marked vowel wins; else the last vowel when a final
 * consonant exists; else the first of two vowels except the {@code oa/oe/uy}
 * clusters, which carry the tone on the second. The {@code qu}/{@code gi} onset
 * glides are excluded from the nucleus.
 */
final class TelexComposer {

    /** The edit to apply at the cursor: delete {@code deleteCount} code units, insert {@code commit}. */
    static final class Edit {
        final int deleteCount;
        final String commit;

        Edit(int deleteCount, String commit) {
            this.deleteCount = deleteCount;
            this.commit = commit;
        }
    }

    // Combining marks (used only to build the precomposed tables below).
    private static final char MARK_CIRCUMFLEX = '̂';
    private static final char MARK_BREVE = '̆';
    private static final char MARK_HORN = '̛';

    private static final int QUALITY_NONE = 0;
    private static final int QUALITY_CIRCUMFLEX = 1;
    private static final int QUALITY_BREVE = 2;
    private static final int QUALITY_HORN = 3;
    private static final char[] QUALITY_MARKS = {0, MARK_CIRCUMFLEX, MARK_BREVE, MARK_HORN};

    private static final int TONE_NONE = -1;
    // sắc, huyền, hỏi, ngã, nặng
    private static final char[] TONE_MARKS = {'́', '̀', '̉', '̃', '̣'};

    /** One parsed character of the word. Consonants keep {@code raw} untouched. */
    private static final class VChar {
        char raw;          // original char (case preserved), used for consonants
        boolean vowel;
        char base;         // lowercase base vowel a/e/i/o/u/y
        int quality;       // QUALITY_*
        int tone;          // TONE_NONE or 0..4
        boolean upper;
    }

    /** Lowercase precomposed vowel → packed (base, quality, tone). */
    private static final Map<Character, Integer> DECOMPOSE = new HashMap<>();
    /** Packed (base, quality, tone) → lowercase precomposed vowel. */
    private static final Map<Integer, Character> COMPOSE = new HashMap<>();

    static {
        String bases = "aeiouy";
        for (int b = 0; b < bases.length(); b++) {
            for (int q = 0; q < QUALITY_MARKS.length; q++) {
                for (int t = TONE_NONE; t < TONE_MARKS.length; t++) {
                    StringBuilder sb = new StringBuilder().append(bases.charAt(b));
                    if (q != QUALITY_NONE) sb.append(QUALITY_MARKS[q]);
                    if (t != TONE_NONE) sb.append(TONE_MARKS[t]);
                    String nfc = Normalizer.normalize(sb, Normalizer.Form.NFC);
                    if (nfc.length() != 1) continue; // combo has no precomposed form (e.g. ê + horn)
                    int packed = pack(bases.charAt(b), q, t);
                    DECOMPOSE.put(nfc.charAt(0), packed);
                    COMPOSE.put(packed, nfc.charAt(0));
                }
            }
        }
    }

    private static int pack(char base, int quality, int tone) {
        return (base << 8) | (quality << 4) | (tone + 1);
    }

    private TelexComposer() {
        // Utility class — no instances.
    }

    /**
     * Process one typed key against the word before the cursor.
     *
     * @param wordBeforeCursor the trailing letter run before the cursor ("" if none)
     * @param key the typed character with the user's case already applied
     * @return never null; when no rule fires: {@code deleteCount == 0}, commit is the key itself
     */
    static Edit process(String wordBeforeCursor, char key) {
        String original = wordBeforeCursor == null ? "" : wordBeforeCursor;
        String word = Normalizer.normalize(original, Normalizer.Form.NFC);

        String newWord = applyRules(word, key);
        if (newWord == null) {
            return new Edit(0, String.valueOf(key));
        }
        if (!word.equals(original)) {
            // Input contained decomposed marks (e.g. Rade-layer residue): replace the
            // whole word rather than diffing across different normal forms.
            return new Edit(original.length(), newWord);
        }
        int prefix = commonPrefixLength(original, newWord);
        return new Edit(original.length() - prefix, newWord.substring(prefix));
    }

    /** Returns the full new word, or null when no rule fires (commit the key literally). */
    private static String applyRules(String word, char key) {
        if (word.isEmpty()) return null;
        char k = Character.toLowerCase(key);

        VChar[] chars = parse(word);

        if (k == 'd') {
            return applyDd(chars, key);
        }
        if (k == 'a' || k == 'e' || k == 'o') {
            return applyCircumflex(chars, k, key);
        }
        if (k == 'w') {
            return applyHorn(chars, key);
        }
        int tone = toneForKey(k);
        if (tone != TONE_NONE) {
            return applyTone(chars, tone, key);
        }
        if (k == 'z') {
            return applyToneRemoval(chars);
        }
        return null;
    }

    private static int toneForKey(char k) {
        switch (k) {
            case 's': return 0; // sắc
            case 'f': return 1; // huyền
            case 'r': return 2; // hỏi
            case 'x': return 3; // ngã
            case 'j': return 4; // nặng
            default: return TONE_NONE;
        }
    }

    // ---- individual rules -------------------------------------------------

    /** dd → đ on an immediately preceding d; đ + d reverts to literal "dd". */
    private static String applyDd(VChar[] chars, char typedKey) {
        VChar last = chars[chars.length - 1];
        if (last.vowel) return null;
        if (Character.toLowerCase(last.raw) == 'd') {
            char dj = last.raw == 'D' ? 'Đ' : 'đ';
            return render(chars, chars.length - 1) + dj;
        }
        if (last.raw == 'đ' || last.raw == 'Đ') {
            char d = last.raw == 'Đ' ? 'D' : 'd';
            return render(chars, chars.length - 1) + d + typedKey;
        }
        return null;
    }

    /** aa/ee/oo → circumflex on the trailing matching vowel; already-circumflexed reverts. */
    private static String applyCircumflex(VChar[] chars, char base, char typedKey) {
        VChar last = chars[chars.length - 1];
        if (!last.vowel || last.base != base) return null;
        if (last.quality == QUALITY_CIRCUMFLEX) {
            // Revert: â + a → "aa" (tone stays on the first vowel; e.g. ố + o → "óo").
            last.quality = QUALITY_NONE;
            return render(chars, chars.length) + typedKey;
        }
        last.quality = QUALITY_CIRCUMFLEX; // replaces a breve/horn if present
        return render(chars, chars.length);
    }

    /** w → ă/ơ/ư on the last vowel; uo → ươ as a pair; already-marked reverts. */
    private static String applyHorn(VChar[] chars, char typedKey) {
        int[] run = lastVowelRun(chars);
        if (run == null) return null;
        int runStart = run[0];
        int runEnd = run[1]; // exclusive

        // The uo pair: horn both (nước, người). Fully horned ươ reverts to "uow".
        if (runEnd - runStart >= 2) {
            VChar v1 = chars[runEnd - 2];
            VChar v2 = chars[runEnd - 1];
            if (v1.base == 'u' && v2.base == 'o') {
                if (v1.quality == QUALITY_HORN && v2.quality == QUALITY_HORN) {
                    v1.quality = QUALITY_NONE;
                    v2.quality = QUALITY_NONE;
                    return render(chars, chars.length) + typedKey;
                }
                v1.quality = QUALITY_HORN;
                v2.quality = QUALITY_HORN;
                return render(chars, chars.length);
            }
        }

        VChar last = chars[runEnd - 1];
        int wQuality;
        if (last.base == 'a') {
            wQuality = QUALITY_BREVE;
        } else if (last.base == 'o' || last.base == 'u') {
            wQuality = QUALITY_HORN;
        } else {
            return null;
        }
        if (last.quality == wQuality) {
            last.quality = QUALITY_NONE;
            return render(chars, chars.length) + typedKey;
        }
        last.quality = wQuality; // replaces a circumflex if present
        return render(chars, chars.length);
    }

    /** Tone keys: place/replace the tone; the same tone twice reverts to literal. */
    private static String applyTone(VChar[] chars, int tone, char typedKey) {
        int[] nucleus = nucleusRange(chars);
        if (nucleus == null) return null;

        int existing = TONE_NONE;
        for (int i = nucleus[0]; i < nucleus[1]; i++) {
            if (chars[i].tone != TONE_NONE) existing = chars[i].tone;
        }
        if (existing == tone) {
            // Same tone key again: drop the tone and emit the key literally (á+s → "as").
            clearTones(chars, nucleus);
            return render(chars, chars.length) + typedKey;
        }
        clearTones(chars, nucleus);
        chars[tonePlacementIndex(chars, nucleus)].tone = tone;
        return render(chars, chars.length);
    }

    private static String applyToneRemoval(VChar[] chars) {
        int[] nucleus = nucleusRange(chars);
        if (nucleus == null) return null;
        boolean hadTone = false;
        for (int i = nucleus[0]; i < nucleus[1]; i++) {
            if (chars[i].tone != TONE_NONE) hadTone = true;
        }
        if (!hadTone) return null; // literal z
        clearTones(chars, nucleus);
        return render(chars, chars.length);
    }

    private static void clearTones(VChar[] chars, int[] nucleus) {
        for (int i = nucleus[0]; i < nucleus[1]; i++) {
            chars[i].tone = TONE_NONE;
        }
    }

    // ---- syllable analysis ------------------------------------------------

    /** [start, end) of the last maximal vowel run, or null if the word has no vowel. */
    private static int[] lastVowelRun(VChar[] chars) {
        int end = -1;
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i].vowel) {
                end = i + 1;
                break;
            }
        }
        if (end < 0) return null;
        int start = end - 1;
        while (start > 0 && chars[start - 1].vowel) {
            start--;
        }
        return new int[]{start, end};
    }

    /** The tone-bearing nucleus: the last vowel run minus a qu/gi onset glide. */
    private static int[] nucleusRange(VChar[] chars) {
        int[] run = lastVowelRun(chars);
        if (run == null) return null;
        int start = run[0];
        int end = run[1];
        if (start > 0 && end - start > 1) {
            char onset = Character.toLowerCase(chars[start - 1].raw);
            char firstVowel = chars[start].base;
            if ((onset == 'q' && firstVowel == 'u') || (onset == 'g' && firstVowel == 'i')) {
                start++;
            }
        }
        return start < end ? new int[]{start, end} : null;
    }

    /** New-style tone placement within the nucleus (see class comment). */
    private static int tonePlacementIndex(VChar[] chars, int[] nucleus) {
        int start = nucleus[0];
        int end = nucleus[1];

        // 1. Rightmost quality-marked vowel (ê/ô/ă/ơ/ư/â) wins: nước, người, muốn.
        for (int i = end - 1; i >= start; i--) {
            if (chars[i].quality != QUALITY_NONE) return i;
        }
        // 2. A final consonant pulls the tone onto the last vowel: toán.
        if (end < chars.length && !chars[end].vowel) {
            return end - 1;
        }
        int n = end - start;
        if (n == 1) return start;
        if (n == 2) {
            String pair = "" + chars[start].base + chars[start + 1].base;
            // New style: oa/oe/uy carry the tone on the second vowel (hoà, khoẻ, thuý).
            if (pair.equals("oa") || pair.equals("oe") || pair.equals("uy")) {
                return start + 1;
            }
            return start; // của, mía
        }
        return start + 1; // three vowels, no marks: middle (khuỷu-type)
    }

    // ---- parsing / rendering ----------------------------------------------

    private static VChar[] parse(String word) {
        VChar[] chars = new VChar[word.length()];
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            VChar v = new VChar();
            v.raw = c;
            char lower = Character.toLowerCase(c);
            Integer packed = DECOMPOSE.get(lower);
            if (packed != null) {
                v.vowel = true;
                v.base = (char) (packed >> 8);
                v.quality = (packed >> 4) & 0xF;
                v.tone = (packed & 0xF) - 1;
                v.upper = Character.isUpperCase(c);
            }
            chars[i] = v;
        }
        return chars;
    }

    /** Render the first {@code count} chars back to an NFC string. */
    private static String render(VChar[] chars, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            VChar v = chars[i];
            if (!v.vowel) {
                sb.append(v.raw);
                continue;
            }
            Character composed = COMPOSE.get(pack(v.base, v.quality, v.tone));
            if (composed == null) {
                // Cannot happen for combos we created, but stay safe.
                composed = v.base;
            }
            sb.append(v.upper ? Character.toUpperCase(composed) : composed);
        }
        return sb.toString();
    }

    private static int commonPrefixLength(String a, String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }
}
