package com.corriestroup.radekeyboard;

import java.text.Normalizer;

/**
 * Pure (Android-free) diacritic folding for dictionary matching: Vietnamese users
 * routinely type bare ASCII and expect suggestions to restore the marks, so both
 * dictionary keys and the typed prefix are folded before comparison
 * ({@code nguoi} matches {@code người}).
 *
 * <p>Handles both this keyboard's decomposed output (base + combining marks, the
 * Rade layer) and precomposed NFC text from other keyboards by normalizing to NFD
 * first. {@code đ} needs a special case — it does not decompose to d + mark.
 */
final class DiacriticFolder {

    private DiacriticFolder() {
        // Utility class — no instances.
    }

    /** Lowercased, mark-stripped form of {@code s} for dictionary keys and lookups. */
    static String fold(String s) {
        if (s == null || s.isEmpty()) return "";
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(nfd.length());
        for (int i = 0; i < nfd.length(); i++) {
            char c = nfd.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (c == 'đ' || c == 'Đ') {
                sb.append('d');
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
