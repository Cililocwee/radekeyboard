package com.corriestroup.radekeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM unit tests for the pure Vietnamese/Rade text helpers. These run on the host
 * with no Android framework, which is exactly why the logic was extracted out of
 * {@code ModernInputMethodService}.
 */
public class VietnameseTextTest {

    // Combining code points expected out of getCombiningCharacter().
    private static final String COMBINING_GRAVE = "̀";
    private static final String COMBINING_ACUTE = "́";
    private static final String COMBINING_TILDE = "̃";
    private static final String COMBINING_HOOK = "̉";
    private static final String COMBINING_DOT = "̣";
    private static final String COMBINING_BREVE = "̆";

    // Tone-mark KEY labels (as they appear in RADE_ALTS).
    private static final String KEY_GRAVE = "̀";
    private static final String KEY_ACUTE = "́";
    private static final String KEY_TILDE = "̃";
    private static final String KEY_HOOK = "̉";
    private static final String KEY_DOT = "̣";
    private static final String KEY_BREVE = "˘"; // ˘

    @Test
    public void toneMarks_areRecognized() {
        assertTrue(VietnameseText.isToneMark(KEY_GRAVE));
        assertTrue(VietnameseText.isToneMark(KEY_ACUTE));
        assertTrue(VietnameseText.isToneMark(KEY_TILDE));
        assertTrue(VietnameseText.isToneMark(KEY_HOOK));
        assertTrue(VietnameseText.isToneMark(KEY_DOT));
    }

    @Test
    public void nonToneMarks_areNotToneMarks() {
        assertFalse(VietnameseText.isToneMark("a"));
        assertFalse(VietnameseText.isToneMark(KEY_BREVE)); // breve is not a tone mark
        assertFalse(VietnameseText.isToneMark(""));
        assertFalse(VietnameseText.isToneMark(null));
    }

    @Test
    public void breveAndToneMarks_areCombiningInput() {
        assertTrue(VietnameseText.isCombiningInput(KEY_BREVE));
        assertTrue(VietnameseText.isCombiningInput(KEY_ACUTE));
        assertFalse(VietnameseText.isCombiningInput("a"));
        assertFalse(VietnameseText.isCombiningInput(null));
    }

    @Test
    public void vowels_includeAsciiAndVietnameseForms() {
        for (char c : new char[] {'a', 'e', 'i', 'o', 'u', 'y'}) {
            assertTrue("expected vowel: " + c, VietnameseText.isVowel(c));
        }
        // Uppercase should still count.
        assertTrue(VietnameseText.isVowel('A'));
        assertTrue(VietnameseText.isVowel('E'));
        // Vietnamese vowel forms.
        assertTrue(VietnameseText.isVowel('ă')); // ă
        assertTrue(VietnameseText.isVowel('â')); // â
        assertTrue(VietnameseText.isVowel('ê')); // ê
        assertTrue(VietnameseText.isVowel('ô')); // ô
        assertTrue(VietnameseText.isVowel('ơ')); // ơ
        assertTrue(VietnameseText.isVowel('ư')); // ư
    }

    @Test
    public void consonants_areNotVowels() {
        for (char c : new char[] {'b', 'c', 'd', 'g', 'z', '1', ' ', '.'}) {
            assertFalse("did not expect vowel: " + c, VietnameseText.isVowel(c));
        }
    }

    @Test
    public void combiningCharacter_mapsEachToneMark() {
        assertEquals(COMBINING_GRAVE, VietnameseText.getCombiningCharacter(KEY_GRAVE));
        assertEquals(COMBINING_ACUTE, VietnameseText.getCombiningCharacter(KEY_ACUTE));
        assertEquals(COMBINING_TILDE, VietnameseText.getCombiningCharacter(KEY_TILDE));
        assertEquals(COMBINING_HOOK, VietnameseText.getCombiningCharacter(KEY_HOOK));
        assertEquals(COMBINING_DOT, VietnameseText.getCombiningCharacter(KEY_DOT));
    }

    @Test
    public void combiningCharacter_mapsBreveKeyToCombiningBreve() {
        // The breve KEY is the spacing breve U+02D8; it must map to the COMBINING breve.
        assertEquals(COMBINING_BREVE, VietnameseText.getCombiningCharacter(KEY_BREVE));
    }

    @Test
    public void combiningCharacter_passesThroughUnknownAndNull() {
        assertEquals("a", VietnameseText.getCombiningCharacter("a"));
        assertNull(VietnameseText.getCombiningCharacter(null));
    }
}
