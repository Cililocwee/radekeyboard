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

        // Handle special Rade characters and combining breve
        String textToCommit = key;
        if (key.equals("˘")) {
            // Combining breve - add it to the preceding character
            CharSequence beforeCursor = ic.getTextBeforeCursor(1, 0);
            if (beforeCursor != null && beforeCursor.length() > 0) {
                ic.deleteSurroundingText(1, 0);
                textToCommit = beforeCursor + "̆"; // Unicode combining breve U+0306
            } else {
                textToCommit = "̆"; // Just the combining breve if no preceding character
            }
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

            case ModernKeyboardView.KEY_NUMBERS:
                // TODO: Implement number layout toggle
                // For now, just show a toast
                android.widget.Toast.makeText(this, "Numbers mode - coming soon!", android.widget.Toast.LENGTH_SHORT).show();
                break;
        }
    }
}