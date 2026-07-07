package com.corriestroup.radekeyboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure (Android-free) keyboard layout definitions and row/alternate selection.
 *
 * <p>The layout arrays used to live inside {@code ModernKeyboardView}; they are kept
 * here so row selection (number row on/off) and long-press-alternate merging can be
 * unit tested on the JVM (see {@code KeyboardLayoutsTest}).
 */
final class KeyboardLayouts {

    private static final String[][] QWERTY_LAYOUT = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DELETE"},
            {"123", ",", " ", ".", "ENTER"}
    };

    private static final String[][] SYMBOL_LAYOUT = {
            {"[", "]", "{", "}", "<", ">", "^", "÷"},
            {"@", "#", "$", "&", "_", "-", "(", ")", "=", "%"},
            {"~", "\"", "*", "'", ":", "/", "!", "?", "+", "DELETE"},
            {"ABC", "SETTINGS", ",", " ", ".", "ENTER"}
    };

    /**
     * Digit gained as a long-press alternate by each top-letter-row key when the
     * dedicated number row is hidden (q→1 … p→0, mirroring their column).
     */
    static final Map<String, String> TOP_ROW_DIGITS;
    static {
        Map<String, String> digits = new HashMap<>();
        String[] topRow = QWERTY_LAYOUT[1];
        String[] numberRow = QWERTY_LAYOUT[0];
        for (int i = 0; i < topRow.length; i++) {
            digits.put(topRow[i], numberRow[i]);
        }
        TOP_ROW_DIGITS = Collections.unmodifiableMap(digits);
    }

    private KeyboardLayouts() {
        // Utility class — no instances.
    }

    /**
     * The QWERTY rows for the given number-row setting. With the number row off the
     * digit row is dropped (4 rows); digits move to long-press alternates via
     * {@link #effectiveAlternates}. The returned array is fresh but shares the row
     * references — callers must not mutate rows.
     */
    static String[][] qwertyRows(boolean numberRowEnabled) {
        if (numberRowEnabled) {
            return Arrays.copyOf(QWERTY_LAYOUT, QWERTY_LAYOUT.length);
        }
        return Arrays.copyOfRange(QWERTY_LAYOUT, 1, QWERTY_LAYOUT.length);
    }

    /** The symbol-mode rows (independent of the number-row setting). */
    static String[][] symbolRows() {
        return Arrays.copyOf(SYMBOL_LAYOUT, SYMBOL_LAYOUT.length);
    }

    /** Row count the keyboard height is derived from — always the QWERTY row count. */
    static int heightRowCount(boolean numberRowEnabled) {
        return qwertyRows(numberRowEnabled).length;
    }

    /**
     * The long-press alternates a key should actually show. When the number row is
     * hidden, top-letter-row keys gain their column's digit as the FIRST alternate
     * (so the preview shows it and a plain long-press commits it), followed by the
     * key's base alternates. Never mutates {@code baseAlternates}; returns it
     * unchanged (possibly null) when no digit applies.
     */
    static String[] effectiveAlternates(String label, String[] baseAlternates,
                                        boolean numberRowEnabled) {
        if (numberRowEnabled || label == null) {
            return baseAlternates;
        }
        String digit = TOP_ROW_DIGITS.get(label.toLowerCase());
        if (digit == null) {
            return baseAlternates;
        }
        if (baseAlternates == null || baseAlternates.length == 0) {
            return new String[]{digit};
        }
        String[] merged = new String[baseAlternates.length + 1];
        merged[0] = digit;
        System.arraycopy(baseAlternates, 0, merged, 1, baseAlternates.length);
        return merged;
    }
}
