package com.corriestroup.radekeyboard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Long-press alternates and key-preview counts per language layer. Pure data — the
 * view looks these up through its {@code effectiveAlts()} helper (which also merges
 * in digits when the number row is hidden).
 *
 * <p>The Rade maps hold combining code points (the tone-mark labels render
 * invisibly in most editors); they were moved verbatim from the view. The
 * Vietnamese maps use precomposed NFC characters as the long-press fallback to
 * Telex input; English keeps symbols only.
 */
final class LayerAlternates {

    private static final Map<String, String[]> RADE_ALTS = new HashMap<>();
    private static final Map<String, Integer> RADE_PREVIEW = new HashMap<>();
    private static final Map<String, String[]> VN_ALTS = new HashMap<>();
    private static final Map<String, Integer> VN_PREVIEW = new HashMap<>();
    private static final Map<String, String[]> EN_ALTS = new HashMap<>();
    private static final Map<String, Integer> EN_PREVIEW = new HashMap<>();

    static {
        // ---- Rade (moved verbatim from ModernKeyboardView) ----
        // Tone marks on g, h, j, k, l
        RADE_ALTS.put("g", new String[]{"̀", "-"}); // Down tone (grave accent)
        RADE_ALTS.put("h", new String[]{"̉", "+"}); // Hoi tone (hook above)
        RADE_ALTS.put("j", new String[]{"̃", "="}); // Nga tone (tilde)
        RADE_ALTS.put("k", new String[]{"́", "("}); // Up tone (acute accent)
        RADE_ALTS.put("l", new String[]{"̣", ")"}); // Nang tone (dot below)

        RADE_ALTS.put("q", new String[]{"%"});
        RADE_ALTS.put("w", new String[]{"^"});
        RADE_ALTS.put("e", new String[]{"~", "ê"});
        RADE_ALTS.put("r", new String[]{"|"});
        RADE_ALTS.put("t", new String[]{"["});
        RADE_ALTS.put("y", new String[]{"]"});
        RADE_ALTS.put("u", new String[]{"<", "ư"});
        RADE_ALTS.put("i", new String[]{">"});
        RADE_ALTS.put("o", new String[]{"{", "ô", "ơ"});
        RADE_ALTS.put("p", new String[]{"}"});

        RADE_ALTS.put("a", new String[]{"@", "ă", "â"});
        RADE_ALTS.put("s", new String[]{"#"});
        RADE_ALTS.put("d", new String[]{"&", "đ"});
        RADE_ALTS.put("f", new String[]{"*"});

        RADE_ALTS.put("z", new String[]{"_"});
        RADE_ALTS.put("x", new String[]{"$"});
        RADE_ALTS.put("c", new String[]{"\""});
        RADE_ALTS.put("v", new String[]{"'"});
        RADE_ALTS.put("b", new String[]{":"});
        RADE_ALTS.put("n", new String[]{";", "ñ"});
        RADE_ALTS.put("m", new String[]{"/"});

        RADE_ALTS.put(",", new String[]{"˘"}); // Comma shows breve on long press
        RADE_ALTS.put(".", new String[]{",", "!", "?"});

        RADE_PREVIEW.put("a", 1);
        RADE_PREVIEW.put("e", 1);
        RADE_PREVIEW.put("o", 1);
        RADE_PREVIEW.put("u", 1);
        RADE_PREVIEW.put("d", 1);
        RADE_PREVIEW.put("g", 2);
        RADE_PREVIEW.put("h", 2);
        RADE_PREVIEW.put("j", 2);
        RADE_PREVIEW.put("k", 2);
        RADE_PREVIEW.put("l", 2);
        RADE_PREVIEW.put("q", 1);
        RADE_PREVIEW.put("w", 1);
        RADE_PREVIEW.put("r", 1);
        RADE_PREVIEW.put("t", 1);
        RADE_PREVIEW.put("y", 1);
        RADE_PREVIEW.put("i", 1);
        RADE_PREVIEW.put("p", 1);
        RADE_PREVIEW.put("s", 1);
        RADE_PREVIEW.put("f", 1);
        RADE_PREVIEW.put("z", 1);
        RADE_PREVIEW.put("x", 1);
        RADE_PREVIEW.put("c", 1);
        RADE_PREVIEW.put("v", 1);
        RADE_PREVIEW.put("b", 1);
        RADE_PREVIEW.put("n", 1);
        RADE_PREVIEW.put("m", 1);
        RADE_PREVIEW.put(",", 1);
        RADE_PREVIEW.put(".", 3);

        // ---- Vietnamese: precomposed accents as the Telex fallback ----
        VN_ALTS.put("a", new String[]{"â", "ă", "à", "á", "ả", "ã", "ạ"});
        VN_ALTS.put("e", new String[]{"ê", "è", "é", "ẻ", "ẽ", "ẹ"});
        VN_ALTS.put("o", new String[]{"ô", "ơ", "ò", "ó", "ỏ", "õ", "ọ"});
        VN_ALTS.put("u", new String[]{"ư", "ù", "ú", "ủ", "ũ", "ụ"});
        VN_ALTS.put("i", new String[]{"ì", "í", "ỉ", "ĩ", "ị"});
        VN_ALTS.put("y", new String[]{"ỳ", "ý", "ỷ", "ỹ", "ỵ"});
        VN_ALTS.put("d", new String[]{"đ", "&"});
        VN_ALTS.put("q", new String[]{"%"});
        VN_ALTS.put("w", new String[]{"^"});
        VN_ALTS.put("r", new String[]{"|"});
        VN_ALTS.put("t", new String[]{"["});
        VN_ALTS.put("s", new String[]{"#"});
        VN_ALTS.put("f", new String[]{"*"});
        VN_ALTS.put("g", new String[]{"-"});
        VN_ALTS.put("h", new String[]{"+"});
        VN_ALTS.put("j", new String[]{"="});
        VN_ALTS.put("k", new String[]{"("});
        VN_ALTS.put("l", new String[]{")"});
        VN_ALTS.put("z", new String[]{"_"});
        VN_ALTS.put("x", new String[]{"$"});
        VN_ALTS.put("c", new String[]{"\""});
        VN_ALTS.put("v", new String[]{"'"});
        VN_ALTS.put("b", new String[]{":"});
        VN_ALTS.put("n", new String[]{";"});
        VN_ALTS.put("m", new String[]{"/"});
        VN_ALTS.put(".", new String[]{",", "!", "?"});

        VN_PREVIEW.put("a", 1);
        VN_PREVIEW.put("e", 1);
        VN_PREVIEW.put("o", 1);
        VN_PREVIEW.put("u", 1);
        VN_PREVIEW.put("d", 1);
        VN_PREVIEW.put(".", 3);
        for (String k : new String[]{"q", "w", "r", "t", "s", "f", "g", "h", "j", "k",
                "l", "z", "x", "c", "v", "b", "n", "m", "i", "y"}) {
            VN_PREVIEW.put(k, 1);
        }

        // ---- English: symbols only ----
        EN_ALTS.put("q", new String[]{"%"});
        EN_ALTS.put("w", new String[]{"^"});
        EN_ALTS.put("e", new String[]{"~"});
        EN_ALTS.put("r", new String[]{"|"});
        EN_ALTS.put("t", new String[]{"["});
        EN_ALTS.put("y", new String[]{"]"});
        EN_ALTS.put("u", new String[]{"<"});
        EN_ALTS.put("i", new String[]{">"});
        EN_ALTS.put("o", new String[]{"{"});
        EN_ALTS.put("p", new String[]{"}"});
        EN_ALTS.put("a", new String[]{"@"});
        EN_ALTS.put("s", new String[]{"#"});
        EN_ALTS.put("d", new String[]{"&"});
        EN_ALTS.put("f", new String[]{"*"});
        EN_ALTS.put("g", new String[]{"-"});
        EN_ALTS.put("h", new String[]{"+"});
        EN_ALTS.put("j", new String[]{"="});
        EN_ALTS.put("k", new String[]{"("});
        EN_ALTS.put("l", new String[]{")"});
        EN_ALTS.put("z", new String[]{"_"});
        EN_ALTS.put("x", new String[]{"$"});
        EN_ALTS.put("c", new String[]{"\""});
        EN_ALTS.put("v", new String[]{"'"});
        EN_ALTS.put("b", new String[]{":"});
        EN_ALTS.put("n", new String[]{";"});
        EN_ALTS.put("m", new String[]{"/"});
        EN_ALTS.put(".", new String[]{",", "!", "?"});

        EN_PREVIEW.put(".", 3);
        for (String k : new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
                "a", "s", "d", "f", "g", "h", "j", "k", "l",
                "z", "x", "c", "v", "b", "n", "m"}) {
            EN_PREVIEW.put(k, 1);
        }
    }

    private static final Map<String, String[]> RADE_ALTS_RO = Collections.unmodifiableMap(RADE_ALTS);
    private static final Map<String, String[]> VN_ALTS_RO = Collections.unmodifiableMap(VN_ALTS);
    private static final Map<String, String[]> EN_ALTS_RO = Collections.unmodifiableMap(EN_ALTS);
    private static final Map<String, Integer> RADE_PREVIEW_RO = Collections.unmodifiableMap(RADE_PREVIEW);
    private static final Map<String, Integer> VN_PREVIEW_RO = Collections.unmodifiableMap(VN_PREVIEW);
    private static final Map<String, Integer> EN_PREVIEW_RO = Collections.unmodifiableMap(EN_PREVIEW);

    private LayerAlternates() {
        // Utility class — no instances.
    }

    static Map<String, String[]> alts(KeyboardLayer layer) {
        switch (layer) {
            case VIETNAMESE: return VN_ALTS_RO;
            case ENGLISH: return EN_ALTS_RO;
            case RADE:
            default: return RADE_ALTS_RO;
        }
    }

    static Map<String, Integer> previewCounts(KeyboardLayer layer) {
        switch (layer) {
            case VIETNAMESE: return VN_PREVIEW_RO;
            case ENGLISH: return EN_PREVIEW_RO;
            case RADE:
            default: return RADE_PREVIEW_RO;
        }
    }
}
