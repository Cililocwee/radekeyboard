package com.corriestroup.radekeyboard;

import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.List;

public class KeyboardSetupChecker {

    private Context context;
    private String keyboardId;

    public KeyboardSetupChecker(Context context) {
        this.context = context;
        this.keyboardId = context.getPackageName() + "/.ModernInputMethodService";
    }

    /**
     * Check if keyboard is enabled in system settings
     */
    public boolean isKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> enabledInputMethods = imm.getEnabledInputMethodList();

        for (InputMethodInfo info : enabledInputMethods) {
            if (info.getId().equals(keyboardId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if keyboard is set as default/current
     */
    public boolean isKeyboardDefault() {
        String currentKeyboard = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
        );

        return keyboardId.equals(currentKeyboard);
    }

    /**
     * Check if keyboard is fully set up (enabled + default)
     */
    public boolean isKeyboardFullySetup() {
        return isKeyboardEnabled() && isKeyboardDefault();
    }

    /**
     * Get setup status for UI display
     */
    public SetupStatus getSetupStatus() {
        boolean enabled = isKeyboardEnabled();
        boolean isDefault = isKeyboardDefault();

        if (!enabled) {
            return SetupStatus.NOT_ENABLED;
        } else if (!isDefault) {
            return SetupStatus.ENABLED_NOT_DEFAULT;
        } else {
            return SetupStatus.FULLY_SETUP;
        }
    }

    public enum SetupStatus {
        NOT_ENABLED,
        ENABLED_NOT_DEFAULT,
        FULLY_SETUP
    }
}