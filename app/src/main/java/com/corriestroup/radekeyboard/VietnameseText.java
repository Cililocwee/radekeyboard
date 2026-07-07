package com.corriestroup.radekeyboard;

/**
 * Pure (Android-free) helpers for Vietnamese/Rade text entry.
 *
 * <p>All logic here is deterministic and framework-independent so it can be unit
 * tested on the JVM (see {@code VietnameseTextTest}). Keep new text-transformation
 * rules here rather than inside {@code ModernInputMethodService}, which is hard to
 * test because it depends on {@code InputConnection}.
 *
 * <p>The tone-mark constants below are Unicode combining marks, which render
 * invisibly in most editors — the trailing comments name each one.
 */
final class VietnameseText {

    // Tone-mark key labels (these labels are themselves combining code points).
    private static final String GRAVE = "̀";   // huyền (down tone)
    private static final String ACUTE = "́";   // sắc (up tone)
    private static final String CIRCUMFLEX = "̂";
    private static final String TILDE = "̃";   // ngã tone
    private static final String HOOK_ABOVE = "̉"; // hỏi tone
    private static final String DOT_BELOW = "̣";  // nặng tone

    /** The breve key label ("˘", U+02D8) — applies to any preceding character, not just vowels. */
    static final String BREVE_KEY = "˘";

    private VietnameseText() {
        // Utility class — no instances.
    }

    /**
     * True if {@code key} is one of the Vietnamese tone-mark key labels. Tone marks
     * only combine onto vowels; the caller drops them otherwise.
     */
    static boolean isToneMark(String key) {
        if (key == null) return false;
        return key.equals(GRAVE) || key.equals(ACUTE) || key.equals(CIRCUMFLEX)
                || key.equals(TILDE) || key.equals(HOOK_ABOVE) || key.equals(DOT_BELOW);
    }

    /** True if {@code key} produces a combining character (a tone mark or the breve). */
    static boolean isCombiningInput(String key) {
        return isToneMark(key) || BREVE_KEY.equals(key);
    }

    /**
     * True if {@code c} is a vowel that a Vietnamese tone mark may attach to
     * (includes the Vietnamese vowel forms ă, â, ê, ô, ơ, ư).
     */
    static boolean isVowel(char c) {
        char lower = Character.toLowerCase(c);
        return lower == 'a' || lower == 'e' || lower == 'i'
                || lower == 'o' || lower == 'u' || lower == 'y'
                || lower == 'ă'  // ă
                || lower == 'â'  // â
                || lower == 'ê'  // ê
                || lower == 'ô'  // ô
                || lower == 'ơ'  // ơ
                || lower == 'ư'; // ư
    }

    /**
     * Map a tone-mark / breve key label to its Unicode combining code point. Returns
     * the input unchanged when it is not a known combining key.
     */
    static String getCombiningCharacter(String key) {
        if (key == null) return null;
        if (key.equals(BREVE_KEY)) return "̆";   // combining breve
        if (key.equals(GRAVE)) return "̀";
        if (key.equals(ACUTE)) return "́";
        if (key.equals(TILDE)) return "̃";
        if (key.equals(HOOK_ABOVE)) return "̉";
        if (key.equals(DOT_BELOW)) return "̣";
        return key;
    }
}
