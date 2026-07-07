package com.corriestroup.radekeyboard;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class KeyboardLayoutsTest {

    @Test
    public void qwertyWithNumberRowHasFiveRowsStartingWithDigits() {
        String[][] rows = KeyboardLayouts.qwertyRows(true);
        assertEquals(5, rows.length);
        assertArrayEquals(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, rows[0]);
        assertEquals("q", rows[1][0]);
    }

    @Test
    public void qwertyWithoutNumberRowDropsOnlyTheDigitRow() {
        String[][] rows = KeyboardLayouts.qwertyRows(false);
        assertEquals(4, rows.length);
        assertArrayEquals(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}, rows[0]);
        assertEquals("SHIFT", rows[2][0]);
        assertEquals("ENTER", rows[3][rows[3].length - 1]);
    }

    @Test
    public void heightRowCountFollowsNumberRowSetting() {
        assertEquals(5, KeyboardLayouts.heightRowCount(true));
        assertEquals(4, KeyboardLayouts.heightRowCount(false));
    }

    @Test
    public void symbolRowsContainSettingsKeyInBottomRow() {
        String[][] rows = KeyboardLayouts.symbolRows();
        assertEquals(4, rows.length);
        assertArrayEquals(new String[]{"ABC", "SETTINGS", ",", " ", ".", "ENTER"},
                rows[rows.length - 1]);
    }

    @Test
    public void allTenTopRowKeysGainTheirColumnDigit() {
        String[] expectedDigits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        String[] topRow = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        for (int i = 0; i < topRow.length; i++) {
            String[] merged = KeyboardLayouts.effectiveAlternates(topRow[i], null, false);
            assertArrayEquals("digit for " + topRow[i],
                    new String[]{expectedDigits[i]}, merged);
        }
    }

    @Test
    public void digitIsPrependedBeforeBaseAlternates() {
        String[] base = {"@", "ă", "â"};
        String[] merged = KeyboardLayouts.effectiveAlternates("a", base, false);
        // "a" is not on the top letter row, so no digit applies.
        assertSame(base, merged);

        merged = KeyboardLayouts.effectiveAlternates("w", new String[]{"^"}, false);
        assertArrayEquals(new String[]{"2", "^"}, merged);
    }

    @Test
    public void numberRowOnLeavesAlternatesUntouched() {
        String[] base = {"^"};
        assertSame(base, KeyboardLayouts.effectiveAlternates("w", base, true));
        assertNull(KeyboardLayouts.effectiveAlternates("q", null, true));
    }

    @Test
    public void mergingIsCaseInsensitiveAndRepeatable() {
        String[] base = {"^"};
        String[] once = KeyboardLayouts.effectiveAlternates("W", base, false);
        assertArrayEquals(new String[]{"2", "^"}, once);
        // Repeat with the same base — proves no shared state was mutated.
        String[] twice = KeyboardLayouts.effectiveAlternates("W", base, false);
        assertArrayEquals(new String[]{"2", "^"}, twice);
        assertArrayEquals(new String[]{"^"}, base);
    }

    @Test
    public void nonTopRowKeysNeverGainDigits() {
        String[] base = {"-"};
        assertSame(base, KeyboardLayouts.effectiveAlternates("g", base, false));
        assertSame(base, KeyboardLayouts.effectiveAlternates(",", base, false));
        assertNull(KeyboardLayouts.effectiveAlternates("SHIFT", null, false));
    }
}
