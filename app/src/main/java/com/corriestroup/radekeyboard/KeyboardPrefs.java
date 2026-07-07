package com.corriestroup.radekeyboard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single access point for the keyboard's persisted settings. Uses the app's one
 * SharedPreferences file ("app_prefs", shared with the onboarding language toggle)
 * so there is exactly one source of truth.
 *
 * <p>The IME re-reads these in {@code onStartInputView}, so changes made in
 * {@link SettingsActivity} apply the next time the keyboard opens.
 */
final class KeyboardPrefs {

    private static final String PREFS_NAME = "app_prefs";

    static final String KEY_HAPTICS_ENABLED = "pref_haptics_enabled";
    static final String KEY_HAPTIC_DURATION_MS = "pref_haptic_duration_ms";
    static final String KEY_NUMBER_ROW = "pref_number_row";
    static final String KEY_KEYBOARD_LAYER = "keyboard_layer";

    static final int DEFAULT_HAPTIC_DURATION_MS = 20;

    private KeyboardPrefs() {
        // Utility class — no instances.
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Key-press vibration. Deliberately opt-in (defaults off) — see decisions.md. */
    static boolean isHapticsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_HAPTICS_ENABLED, false);
    }

    static void setHapticsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply();
    }

    static int getHapticDurationMs(Context context) {
        return prefs(context).getInt(KEY_HAPTIC_DURATION_MS, DEFAULT_HAPTIC_DURATION_MS);
    }

    static void setHapticDurationMs(Context context, int durationMs) {
        prefs(context).edit().putInt(KEY_HAPTIC_DURATION_MS, durationMs).apply();
    }

    /** Dedicated digit row above QWERTY; when off, digits become long-press alternates. */
    static boolean isNumberRowEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NUMBER_ROW, true);
    }

    static void setNumberRowEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NUMBER_ROW, enabled).apply();
    }

    /** Active language layer ("rd" | "vn" | "en"); see {@code KeyboardLayer}. */
    static String getKeyboardLayer(Context context) {
        return prefs(context).getString(KEY_KEYBOARD_LAYER, "rd");
    }

    static void setKeyboardLayer(Context context, String layerPrefValue) {
        prefs(context).edit().putString(KEY_KEYBOARD_LAYER, layerPrefValue).apply();
    }
}
