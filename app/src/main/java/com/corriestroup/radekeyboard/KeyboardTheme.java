package com.corriestroup.radekeyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * The keyboard's single color palette, shared by the keyboard view, suggestion
 * strip, popups, and settings panel so they can never drift apart.
 *
 * <p>The theme preference defaults to LIGHT — the keyboard has always shipped
 * light gray, and following system dark mode surprised users ("it's black now").
 * Dark and follow-system are opt-in via the settings panel.
 */
final class KeyboardTheme {

    static final String THEME_LIGHT = "light";
    static final String THEME_DARK = "dark";
    static final String THEME_SYSTEM = "system";

    final int surface;         // Background
    final int surfaceVariant;  // Keys
    final int onSurface;       // Text
    final int primary;         // Accent
    final int onPrimary;       // Text on accent
    final boolean isDark;

    private KeyboardTheme(boolean dark) {
        isDark = dark;
        primary = Color.parseColor("#27b8cd"); // Accent (shared)
        if (dark) {
            surface = Color.parseColor("#1c1c1e");
            surfaceVariant = Color.parseColor("#2c2c2e");
            onSurface = Color.parseColor("#e5e5e5");
            onPrimary = Color.parseColor("#0d1416");
        } else {
            surface = Color.parseColor("#e5e5e5");
            surfaceVariant = Color.parseColor("#e7e7e7");
            onSurface = Color.parseColor("#4f4f4f");
            onPrimary = Color.parseColor("#e7e7e7");
        }
    }

    /** Resolve the palette from the theme preference (and night mode when "system"). */
    static KeyboardTheme resolve(Context context) {
        String pref = KeyboardPrefs.getTheme(context);
        boolean dark;
        if (THEME_DARK.equals(pref)) {
            dark = true;
        } else if (THEME_SYSTEM.equals(pref)) {
            int nightMode = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            dark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        } else {
            dark = false; // light — the default
        }
        return new KeyboardTheme(dark);
    }
}
