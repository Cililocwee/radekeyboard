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

        // Check auto-capitalization when keyboard is created
        updateAutoCapitalization();

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

        // Handle punctuation that should replace a single trailing space
        if (key.equals(".") || key.equals("?") || key.equals("!")) {
            // Check if there's exactly one space before the cursor
            CharSequence beforeCursor = ic.getTextBeforeCursor(2, 0);
            if (beforeCursor != null && beforeCursor.length() >= 1) {
                String beforeText = beforeCursor.toString();

                // If there's exactly one space at the end, replace it
                if (beforeText.endsWith(" ") && (beforeText.length() == 1 || !beforeText.endsWith("  "))) {
                    // Delete the trailing space
                    ic.deleteSurroundingText(1, 0);
                }
            }

            // Commit the punctuation + space
            ic.commitText(key + " ", 1);

            // Check if we should auto-capitalize after this text
            updateAutoCapitalization();
            return; // Exit early since we handled everything
        }

        // Handle tone marks and combining characters
        String textToCommit = key;
        if (isToneMark(key) || key.equals("˘")) {
            // Get the preceding character
            CharSequence beforeCursor = ic.getTextBeforeCursor(1, 0);
            if (beforeCursor != null && beforeCursor.length() > 0) {
                char precedingChar = beforeCursor.charAt(0);

                // Check if it's a vowel (for tone marks) or any character (for breve)
                if (key.equals("˘") || isVowel(precedingChar)) {
                    ic.deleteSurroundingText(1, 0);
                    textToCommit = beforeCursor + getCombiningCharacter(key);
                } else {
                    // If not a vowel and it's a tone mark, don't apply it
                    if (isToneMark(key)) {
                        return; // Don't commit anything
                    }
                    textToCommit = getCombiningCharacter(key);
                }
            } else {
                textToCommit = getCombiningCharacter(key);
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

        // Automatically add a space after punctuation
        if (key.equals(".") || key.equals("?") || key.equals("!")) {
            ic.commitText(" ", 1);
        }
        // Check if we should auto-capitalize after this text
        updateAutoCapitalization();
    }

    private void deleteLastWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Get text before cursor (we'll get more than we need and trim it)
        CharSequence beforeCursor = ic.getTextBeforeCursor(1000, 0);
        if (beforeCursor == null || beforeCursor.length() == 0) return;

        String text = beforeCursor.toString();
        int originalLength = text.length();
        int wordStart = originalLength;

        // Skip any trailing whitespace first
        while (wordStart > 0 && Character.isWhitespace(text.charAt(wordStart - 1))) {
            wordStart--;
        }

        // Then skip the actual word characters
        while (wordStart > 0 && !Character.isWhitespace(text.charAt(wordStart - 1))) {
            wordStart--;
        }

        // Calculate how many characters to delete
        int charsToDelete = originalLength - wordStart;

        // Delete the characters
        if (charsToDelete > 0) {
            ic.deleteSurroundingText(charsToDelete, 0);
        }
    }

    private void updateAutoCapitalization() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Get text before cursor
        CharSequence beforeCursor = ic.getTextBeforeCursor(10, 0);

        boolean shouldCapitalize = false;

        if (beforeCursor == null || beforeCursor.length() == 0) {
            // Empty field - capitalize
            shouldCapitalize = true;
        } else {
            String text = beforeCursor.toString();
            // Check if ends with sentence endings followed by space
            if (text.endsWith(". ") || text.endsWith("? ") || text.endsWith("! ")) {
                shouldCapitalize = true;
            }
        }

        if (shouldCapitalize) {
            // Only set shift if we're not already in caps lock and not already shifted
            if (!isCapsLockOn && !isShiftPressed) {
                isShiftPressed = true;  // Set shift, not caps lock
                isCapsLockOn = false;   // Make sure caps lock is off
                keyboardView.updateShiftState(isShiftPressed, isCapsLockOn);
            }
        }
    }
    private boolean isToneMark(String key) {
        return key.equals("̀") || key.equals("́") || key.equals("̂") ||
                key.equals("̃") || key.equals("̉") || key.equals("̣");
    }

    private boolean isVowel(char c) {
        char lower = Character.toLowerCase(c);
        return lower == 'a' || lower == 'e' || lower == 'i' ||
                lower == 'o' || lower == 'u' || lower == 'y' ||
                lower == 'ă' || lower == 'â' || lower == 'ê' ||
                lower == 'ô' || lower == 'ơ' || lower == 'ư';
    }

    private String getCombiningCharacter(String key) {
        switch (key) {
            case "˘": return "\u0306"; // Combining breve
            case "̀": return "\u0300"; // Combining grave accent (down tone)
            case "́": return "\u0301"; // Combining acute accent (up tone)
            case "̃": return "\u0303"; // Combining tilde (nga tone)
            case "̉": return "\u0309"; // Combining hook above (hoi tone)
            case "̣": return "\u0323"; // Combining dot below (nang tone)
            default: return key;
        }
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
                CharSequence selectedText = ic.getSelectedText(0);
                if (selectedText != null && selectedText.length() > 0) {
                    // Delete selection
                    ic.commitText("", 1);
                } else {
                    // Regular character deletion
                    ic.deleteSurroundingText(1, 0);
                }
                break;

            case ModernKeyboardView.KEY_DELETE_WORD:  // <- Add this new case
                deleteLastWord();
                break;

            case ModernKeyboardView.KEY_DELETE_CONTINUOUS:
                // Continuous word deletion
                deleteLastWord();
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
                keyboardView.toggleSymbolMode();
                break;
        }

        // Check auto-capitalization after certain special keys
        if (specialKey == ModernKeyboardView.KEY_SPACE ||
                specialKey == ModernKeyboardView.KEY_ENTER ||
                specialKey == ModernKeyboardView.KEY_DELETE ||
                specialKey == ModernKeyboardView.KEY_DELETE_WORD) {
            updateAutoCapitalization();
        }
    }
}