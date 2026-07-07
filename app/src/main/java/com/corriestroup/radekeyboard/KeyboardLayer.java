package com.corriestroup.radekeyboard;

/**
 * The three language layers, cycled by swiping horizontally on the space bar.
 * The active layer is app state persisted via {@link KeyboardPrefs} — it is
 * deliberately NOT synced to system IME subtypes (Rade has no ISO 639-1 code and
 * two sources of truth invite drift; see decisions.md).
 */
enum KeyboardLayer {
    RADE("rd", "Ê Đê"),
    VIETNAMESE("vn", "Tiếng Việt"),
    ENGLISH("en", "English");

    /** Value stored in SharedPreferences. */
    final String prefValue;
    /** Label drawn permanently on the space bar. */
    final String spacebarLabel;

    KeyboardLayer(String prefValue, String spacebarLabel) {
        this.prefValue = prefValue;
        this.spacebarLabel = spacebarLabel;
    }

    /** Next layer for a rightward space-bar swipe. */
    KeyboardLayer next() {
        KeyboardLayer[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** Previous layer for a leftward space-bar swipe. */
    KeyboardLayer previous() {
        KeyboardLayer[] all = values();
        return all[(ordinal() + all.length - 1) % all.length];
    }

    static KeyboardLayer fromPrefValue(String value) {
        for (KeyboardLayer layer : values()) {
            if (layer.prefValue.equals(value)) {
                return layer;
            }
        }
        return RADE;
    }
}
