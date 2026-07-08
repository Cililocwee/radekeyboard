package com.corriestroup.radekeyboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TelexComposerTest {

    /**
     * Feed a keystroke string through the composer one char at a time, maintaining
     * a simulated text buffer the way the IME applies edits at the cursor.
     */
    private static String type(String keys) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < keys.length(); i++) {
            TelexComposer.Edit edit = TelexComposer.process(buffer.toString(), keys.charAt(i));
            buffer.delete(buffer.length() - edit.deleteCount, buffer.length());
            buffer.append(edit.commit);
        }
        return buffer.toString();
    }

    @Test
    public void circumflexAndTone() {
        assertEquals("viết", type("vieets"));
        assertEquals("đây", type("ddaay"));
    }

    @Test
    public void hornsViaWIncludingUoPair() {
        assertEquals("người", type("nguwowfi"));
        assertEquals("người", type("nguowfi")); // uo + w horns both vowels
        assertEquals("ăn", type("awn"));
        assertEquals("ư", type("uw"));
    }

    @Test
    public void loneWStaysLiteral() {
        assertEquals("w", type("w"));
        assertEquals("wh", type("wh"));
    }

    @Test
    public void doubledModifierReverts() {
        assertEquals("xoong", type("xooong")); // oo → ô, third o reverts — how "xoong" is typed
        assertEquals("xông", type("xoong"));   // strict Telex: oo composes, documented tradeoff
        assertEquals("aa", type("aaa"));
        assertEquals("dd", type("ddd"));
        assertEquals("uw", type("uww"));
    }

    @Test
    public void uoPairRevertsAsAPair() {
        assertEquals("uow", type("uoww"));
    }

    @Test
    public void newStyleTonePlacement() {
        assertEquals("hoà", type("hoaf"));   // oa: tone on second vowel (new style)
        assertEquals("khoẻ", type("khoer"));
        assertEquals("thuý", type("thuys")); // uy: second vowel
        assertEquals("của", type("cuar"));   // ua: first vowel
        assertEquals("mía", type("mias"));
        assertEquals("toán", type("toans")); // final consonant pulls tone to last vowel
        assertEquals("muốn", type("muoons")); // quality-marked vowel wins
    }

    @Test
    public void quAndGiGlidesAreExcludedFromTheNucleus() {
        assertEquals("quý", type("quys"));
        assertEquals("gì", type("gif"));
        assertEquals("giá", type("gias"));
    }

    @Test
    public void toneKeyRevertAndReplacement() {
        assertEquals("as", type("ass"));  // same tone key twice reverts to literal
        assertEquals("à", type("asf"));   // different tone key replaces the tone
        assertEquals("a", type("asz"));   // z strips the tone
        assertEquals("az", type("az"));   // z literal when there is no tone
        assertEquals("ss", type("ss"));   // tone key literal with no vowel
    }

    @Test
    public void casePreservation() {
        assertEquals("VIẾT", type("VIEETS"));
        assertEquals("Đây", type("Ddaay"));
        assertEquals("Ấ", type("AAS"));
    }

    @Test
    public void outputIsPrecomposedNfc() {
        String word = type("vieets");
        assertEquals("viết", word); // single precomposed ế (U+1EBF), not e+marks
        assertEquals(4, word.length());
    }

    @Test
    public void decomposedInputFromTheRadeLayerIsTolerated() {
        // "ê" as base + combining circumflex (how the Rade layer commits text).
        TelexComposer.Edit edit = TelexComposer.process("ê", 's');
        assertEquals(2, edit.deleteCount);
        assertEquals("ế", edit.commit); // ế, NFC
    }

    @Test
    public void nonRuleKeysAreSingleCharInserts() {
        TelexComposer.Edit edit = TelexComposer.process("xin", 'h');
        assertEquals(0, edit.deleteCount);
        assertEquals("h", edit.commit);

        edit = TelexComposer.process("", 'a');
        assertEquals(0, edit.deleteCount);
        assertEquals("a", edit.commit);
    }

    @Test
    public void toneAppliesToForeignLookingWordsToo() {
        // Inherent Telex behavior; the same-key revert is the escape hatch.
        assertEquals("fỏ", type("for"));
        assertEquals("for", type("forr"));
    }
}
