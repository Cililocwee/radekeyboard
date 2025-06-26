package com.corriestroup.radekeyboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

public class ModernInputMethodService extends InputMethodService {

    private ModernKeyboardView keyboardView;
    private boolean isShiftPressed = false;
    private boolean isCapsLockOn = false;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public View onCreateInputView() {
        keyboardView = new ModernKeyboardView(this);
        keyboardView.setOnKeyPressListener(new ModernKeyboardView.OnKeyPressListener() {
            @Override
            public void onKeyPressed(String key, int keyCode) {
                handleKeyPress(key, keyCode);
            }

            @Override
            public void onSpecialKeyPressed(int specialKey) {
                handleSpecialKey(specialKey);
            }
        });

        return keyboardView;
    }

    private void handleKeyPress(String key, int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Haptic feedback (compatible with API 21+)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50); // Deprecated but works on older APIs
            }
        }

        // Handle Vietnamese special characters
        String textToCommit = key;
        switch (keyCode) {
            case 226: textToCommit = "â"; break; // ă
            case 234: textToCommit = "ê"; break; // ê
            case 432: textToCommit = "ư"; break; // ư
            case 244: textToCommit = "ô"; break; // ô
            case 417: textToCommit = "ơ"; break; // ơ
            case 273: textToCommit = "đ"; break; // đ
            case 241: textToCommit = "ñ"; break; // ñ
            // Add more as needed
        }

        // Apply shift/caps
        if (isShiftPressed || isCapsLockOn) {
            textToCommit = textToCommit.toUpperCase();
            if (isShiftPressed) {
                isShiftPressed = false;
                keyboardView.updateShiftState(false, isCapsLockOn);
            }
        }

        ic.commitText(textToCommit, 1);
    }

    private void handleSpecialKey(int specialKey) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Haptic feedback (compatible with API 21+)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(75); // Deprecated but works on older APIs
            }
        }

        switch (specialKey) {
            case ModernKeyboardView.KEY_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;

            case ModernKeyboardView.KEY_SHIFT:
                if (isShiftPressed) {
                    // Double tap = caps lock
                    isCapsLockOn = !isCapsLockOn;
                    isShiftPressed = false;
                } else {
                    isShiftPressed = true;
                }
                keyboardView.updateShiftState(isShiftPressed, isCapsLockOn);
                break;

            case ModernKeyboardView.KEY_ENTER:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                break;

            case ModernKeyboardView.KEY_SPACE:
                ic.commitText(" ", 1);
                break;

            case ModernKeyboardView.KEY_SYMBOL:
                keyboardView.toggleSymbolMode();
                break;
        }
    }
}