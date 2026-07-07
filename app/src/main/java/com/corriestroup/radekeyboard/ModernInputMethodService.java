package com.corriestroup.radekeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class ModernInputMethodService extends InputMethodService {

    private ModernKeyboardView keyboardView;
    private boolean isShiftPressed = false;
    private boolean isCapsLockOn = false;
    private KeyboardLayer currentLayer = KeyboardLayer.RADE;

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

            @Override
            public void onLanguageSwipe(int direction) {
                handleLanguageSwipe(direction);
            }
        });

        currentLayer = KeyboardLayer.fromPrefValue(KeyboardPrefs.getKeyboardLayer(this));
        keyboardView.setLanguageLayer(currentLayer);

        // Check auto-capitalization when keyboard is created
        updateAutoCapitalization();

        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        // Pick up settings changed while the keyboard was closed (number row,
        // haptics, layer), and refresh auto-caps for the newly focused field.
        if (keyboardView != null) {
            keyboardView.refreshFromPrefs();
            currentLayer = KeyboardLayer.fromPrefValue(KeyboardPrefs.getKeyboardLayer(this));
            keyboardView.setLanguageLayer(currentLayer);
        }
        updateAutoCapitalization();
    }

    private void handleLanguageSwipe(int direction) {
        currentLayer = direction > 0 ? currentLayer.next() : currentLayer.previous();
        KeyboardPrefs.setKeyboardLayer(this, currentLayer.prefValue);
        keyboardView.setLanguageLayer(currentLayer);
    }

    private void handleKeyPress(String key, int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

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

        // Layer-specific text paths; Rade falls through to the combining-mark logic below.
        if (currentLayer == KeyboardLayer.VIETNAMESE) {
            handleVietnameseKey(ic, key);
            return;
        }
        if (currentLayer == KeyboardLayer.ENGLISH) {
            commitPlainKey(ic, key);
            return;
        }

        // Handle tone marks and combining characters
        String textToCommit = key;
        if (VietnameseText.isCombiningInput(key)) {
            // Get the preceding character
            CharSequence beforeCursor = ic.getTextBeforeCursor(1, 0);
            if (beforeCursor != null && beforeCursor.length() > 0) {
                char precedingChar = beforeCursor.charAt(0);

                // Check if it's a vowel (for tone marks) or any character (for breve)
                if (VietnameseText.BREVE_KEY.equals(key) || VietnameseText.isVowel(precedingChar)) {
                    ic.deleteSurroundingText(1, 0);
                    textToCommit = beforeCursor + VietnameseText.getCombiningCharacter(key);
                } else {
                    // If not a vowel and it's a tone mark, don't apply it
                    if (VietnameseText.isToneMark(key)) {
                        return; // Don't commit anything
                    }
                    textToCommit = VietnameseText.getCombiningCharacter(key);
                }
            } else {
                textToCommit = VietnameseText.getCombiningCharacter(key);
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

    /**
     * Vietnamese layer: single letters run through the Telex composer against the
     * word before the cursor. Shift is resolved BEFORE composing (the post-hoc
     * uppercase in the Rade path would corrupt multi-character recommits), and the
     * edit is applied as one batch. Output is precomposed NFC.
     */
    private void handleVietnameseKey(InputConnection ic, String key) {
        if (key.length() != 1 || !Character.isLetter(key.charAt(0))) {
            commitPlainKey(ic, key);
            return;
        }
        char typed = key.charAt(0);
        if (isShiftPressed || isCapsLockOn) {
            typed = Character.toUpperCase(typed);
            consumeOneShotShift();
        }

        CharSequence before = ic.getTextBeforeCursor(32, 0);
        TelexComposer.Edit edit = TelexComposer.process(extractTrailingWord(before), typed);
        ic.beginBatchEdit();
        if (edit.deleteCount > 0) {
            ic.deleteSurroundingText(edit.deleteCount, 0);
        }
        ic.commitText(edit.commit, 1);
        ic.endBatchEdit();
        updateAutoCapitalization();
    }

    /** English layer / non-letter fallback: commit as-is with the usual shift handling. */
    private void commitPlainKey(InputConnection ic, String key) {
        String text = key;
        if (isShiftPressed || isCapsLockOn) {
            text = text.toUpperCase();
            consumeOneShotShift();
        }
        ic.commitText(text, 1);
        updateAutoCapitalization();
    }

    private void consumeOneShotShift() {
        if (isShiftPressed) {
            isShiftPressed = false;
            keyboardView.updateShiftState(false, isCapsLockOn);
        }
    }

    /**
     * The letter run (including combining marks, which the Rade layer commits)
     * immediately before the cursor — the composer's input word.
     */
    private static String extractTrailingWord(CharSequence before) {
        if (before == null || before.length() == 0) return "";
        int start = before.length();
        while (start > 0) {
            char c = before.charAt(start - 1);
            if (Character.isLetter(c) || Character.getType(c) == Character.NON_SPACING_MARK) {
                start--;
            } else {
                break;
            }
        }
        return before.subSequence(start, before.length()).toString();
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
    private void handleSpecialKey(int specialKey) {
        // Settings must open even without an input connection, so it is handled
        // before the null guard below.
        if (specialKey == ModernKeyboardView.KEY_SETTINGS) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requestHideSelf(0);
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

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