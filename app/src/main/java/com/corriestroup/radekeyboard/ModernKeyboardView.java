package com.corriestroup.radekeyboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModernKeyboardView extends View {

    // Special key codes
    public static final int KEY_DELETE = -1;
    public static final int KEY_SHIFT = -2;
    public static final int KEY_ENTER = -3;
    public static final int KEY_SPACE = -4;
    public static final int KEY_SYMBOL = -5;
    public static final int KEY_NUMBERS = -6;

    // Keyboard layouts
    private static final String[][] QWERTY_LAYOUT = {{"1","2","3","4","5","6","7","8","9","0"},
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DELETE"},
            {"123", ",", " ", ".", "ENTER"}
    };

    private static final String[][] SYMBOL_LAYOUT = {

            {"[","]","{","}","<",">","^","÷"},
            {"@", "#", "$", "&", "_", "-", "(", ")", "=", "%"},
            {"~", "\"", "*", "'", ":", "/", "!", "?", "+", "DELETE"},
            {"ABC", ",", " ", ".", "ENTER"}
    };

    // Vietnamese/Rade diacritics map for long-press
    private static final Map<String, String[]> RADE_ALTS = new HashMap<>();
    static {
        // Tone marks on g, h, j, k, l
        RADE_ALTS.put("g", new String[]{"̀","-"}); // Down tone (grave accent)
        RADE_ALTS.put("h", new String[]{"̉","+"}); // Hoi tone (hook above)
        RADE_ALTS.put("j", new String[]{"̃","="}); // Nga tone (tilde)
        RADE_ALTS.put("k", new String[]{"́","("}); // Up tone (acute accent)
        RADE_ALTS.put("l", new String[]{"̣",")"}); // Nang tone (dot below)

        // Other symbol alternatives (keeping your existing ones)
        RADE_ALTS.put("q", new String[]{"%"});
        RADE_ALTS.put("w", new String[]{"^"});
        RADE_ALTS.put("e", new String[]{"~","ê"});
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
        // g, h, j, k, l now have tone marks instead

        RADE_ALTS.put("z", new String[]{"_"});
        RADE_ALTS.put("x", new String[]{"$"});
        RADE_ALTS.put("c", new String[]{"\""});
        RADE_ALTS.put("v", new String[]{"'"});
        RADE_ALTS.put("b", new String[]{":"});
        RADE_ALTS.put("n", new String[]{";"});
        RADE_ALTS.put("m", new String[]{"/"});

        // Long-press alternatives for bottom row
        RADE_ALTS.put(",", new String[]{"˘"}); // Comma shows breve on long press
        RADE_ALTS.put(".", new String[]{",","!", "?", ";", ":"});


    }

    // Add this new map after your RADE_ALTS map
    private static final Map<String, Integer> ALT_PREVIEW_COUNT = new HashMap<>();
    static {
        // All keys with alternatives set to show 1 preview by default
        ALT_PREVIEW_COUNT.put("a", 1); // Show ă (first alternative)
        ALT_PREVIEW_COUNT.put("e", 1); // Show ê
        ALT_PREVIEW_COUNT.put("o", 1); // Show ô (first alternative)
        ALT_PREVIEW_COUNT.put("u", 1); // Show ư
        ALT_PREVIEW_COUNT.put("d", 1); // Show đ

        // Tone marks
        ALT_PREVIEW_COUNT.put("g", 2); // Show ̀
        ALT_PREVIEW_COUNT.put("h", 2); // Show ̉
        ALT_PREVIEW_COUNT.put("j", 2); // Show ̃
        ALT_PREVIEW_COUNT.put("k", 2); // Show ́
        ALT_PREVIEW_COUNT.put("l", 2); // Show ̣

        // Symbol alternatives
        ALT_PREVIEW_COUNT.put("q", 1); // Show %
        ALT_PREVIEW_COUNT.put("w", 1); // Show ^
        ALT_PREVIEW_COUNT.put("r", 1); // Show |
        ALT_PREVIEW_COUNT.put("t", 1); // Show [
        ALT_PREVIEW_COUNT.put("y", 1); // Show ]
        ALT_PREVIEW_COUNT.put("i", 1); // Show >
        ALT_PREVIEW_COUNT.put("p", 1); // Show }
        ALT_PREVIEW_COUNT.put("s", 1); // Show #
        ALT_PREVIEW_COUNT.put("f", 1); // Show *
        ALT_PREVIEW_COUNT.put("z", 1); // Show _
        ALT_PREVIEW_COUNT.put("x", 1); // Show $
        ALT_PREVIEW_COUNT.put("c", 1); // Show "
        ALT_PREVIEW_COUNT.put("v", 1); // Show '
        ALT_PREVIEW_COUNT.put("b", 1); // Show :
        ALT_PREVIEW_COUNT.put("n", 1); // Show ;
        ALT_PREVIEW_COUNT.put("m", 1); // Show /

        // Bottom row
        ALT_PREVIEW_COUNT.put(",", 1); // Show ˘
        ALT_PREVIEW_COUNT.put(".", 3); // Show ? (first alternative)
    }
    private List<Key> keys = new ArrayList<>();
    private Paint keyPaint, textPaint, backgroundPaint;
    private OnKeyPressListener keyPressListener;
    private Key pressedKey;
    private boolean isSymbolMode = false;
    private boolean isShiftPressed = false;
    private boolean isCapsLock = false;

    private boolean isNumberKey(String label) {
        return label.matches("[0-9]"); // Returns true for digits 0-9
    }

    // Long press functionality
    private Key longPressedKey;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private PopupWindow longPressPopup;
    private boolean isLongPressing = false;
    private static final int LONG_PRESS_DELAY = 200; // Reduced from 500ms to 200ms
    private List<android.widget.Button> altButtons = new ArrayList<>();
    private int selectedAltIndex = -1; // Track which alternative is currently selected

    // Animation
    private ValueAnimator pressAnimator;
    private float pressScale = 1.0f;

    // Vibratino
    private boolean vibrationEnabled = false;

    // Dimensions
    private float keyHeight;
    private float keyMargin;
    private int keyboardHeight;

    // Colors (Material Design 3)
    private int surfaceColor = Color.parseColor("#e5e5e5");         // Background
    private int onSurfaceColor = Color.parseColor("#4f4f4f");       // Text
    private int primaryColor = Color.parseColor("#27b8cd");         // Accent
    private int onPrimaryColor = Color.parseColor("#e7e7e7");       // Text on Accent
    private int surfaceVariantColor = Color.parseColor("#e7e7e7");  //  Keys


    public interface OnKeyPressListener {
        void onKeyPressed(String key, int keyCode);
        void onSpecialKeyPressed(int specialKey);
    }

    public ModernKeyboardView(Context context) {
        super(context);
        init();
    }

    public ModernKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setupPaints();
        calculateDimensions();
        createKeys();
        setBackgroundColor(surfaceColor);
    }

    private void setupPaints() {
        keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyPaint.setColor(surfaceVariantColor);
        keyPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(onSurfaceColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(surfaceColor);
    }

    private void calculateDimensions() {
        float density = getResources().getDisplayMetrics().density;
        keyHeight = 48 * density; // 48dp
        keyMargin = 4 * density;  // 4dp

        // Calculate keyboard height based on current layout
        String[][] currentLayout = isSymbolMode ? SYMBOL_LAYOUT : QWERTY_LAYOUT;
        int numberOfRows = currentLayout.length;

        // Reduce top margin to make keyboard shorter
        float topMargin = keyMargin * 0.2f; // Use half the normal margin for top
        keyboardHeight = (int) (keyHeight * numberOfRows + topMargin + keyMargin * numberOfRows);

        textPaint.setTextSize(16 * density); // 16sp
    }

    private void createKeys() {
        keys.clear();
        String[][] layout = isSymbolMode ? SYMBOL_LAYOUT : QWERTY_LAYOUT;

        float totalWidth = getWidth();
        if (totalWidth == 0) return; // View not measured yet

        float availableWidth = totalWidth - keyMargin * 2;
        float topMargin = keyMargin * 0.5f; // Use reduced top margin

        for (int row = 0; row < layout.length; row++) {
            String[] rowKeys = layout[row];
            float y = topMargin + row * (keyHeight + keyMargin); // Use reduced top margin

            // Calculate total weight for this row
            float totalWeight = 0;
            for (String keyLabel : rowKeys) {
                if (keyLabel.equals("SHIFT") || keyLabel.equals("DELETE")) {
                    totalWeight += 1.5f; // 1.5x width
                } else if (keyLabel.equals("SPACE") || keyLabel.equals(" ")) {
                    totalWeight += 5.0f; // 5x width
                } else if (keyLabel.equals("ENTER")) {
                    totalWeight += 1.5f;
                } else if (keyLabel.equals("SYM")) {
                    totalWeight += 1.0f;
                } else {
                    totalWeight += 1.0f; // Standard key
                }
            }

            // Calculate unit width
            float unitWidth = (availableWidth - keyMargin * (rowKeys.length - 1)) / totalWeight;
            float currentX = keyMargin;

            for (String keyLabel : rowKeys) {
                float keyWidth;

                // Set key widths based on type
                if (keyLabel.equals("SHIFT") || keyLabel.equals("DELETE")) {
                    keyWidth = unitWidth * 1.5f;
                } else if (keyLabel.equals("SPACE") || keyLabel.equals(" ")) {
                    keyWidth = unitWidth * 5.0f;
                } else if (keyLabel.equals("ENTER")) {
                    keyWidth = unitWidth * 1.5f;
                } else {
                    keyWidth = unitWidth;
                }

                Key key = new Key(keyLabel, currentX, y, keyWidth, keyHeight);
                keys.add(key);

                currentX += keyWidth + keyMargin;
            }
        }
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createKeys();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, keyboardHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // Draw keys
        for (Key key : keys) {
            drawKey(canvas, key);
        }
    }

    private void drawKey(Canvas canvas, Key key) {
        // Determine key colors
        int keyColor = surfaceVariantColor;
        int textColor = onSurfaceColor;

        // Special keys that should always use primary color
        boolean isSpecialKey = key.label.equals("DELETE") ||
                key.label.equals("SHIFT") ||
                key.label.equals("123") ||
                key.label.equals("ABC") ||
                key.label.equals(",") ||
                key.label.equals(".") ||
                key.label.equals("ENTER");

        if (key == pressedKey) {
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (key.label.equals("SHIFT") && (isShiftPressed || isCapsLock)) {
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (key == longPressedKey && isLongPressing) {
            // Visual feedback for long press
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (isSpecialKey) {
            // Special keys use primary color as default
//            keyColor = primaryColor;
            textColor = primaryColor;
        }

        // Apply press animation
        float scale = (key == pressedKey) ? pressScale : 1.0f;
        float scaledWidth = key.width * scale;
        float scaledHeight = key.height * scale;
        float offsetX = (key.width - scaledWidth) / 2;
        float offsetY = (key.height - scaledHeight) / 2;

        RectF keyRect = new RectF(
                key.x + offsetX,
                key.y + offsetY,
                key.x + scaledWidth,
                key.y + scaledHeight
        );

        // Draw key background with rounded corners
        keyPaint.setColor(keyColor);
        canvas.drawRoundRect(keyRect, 8, 8, keyPaint);


        // Draw key text or drawable
        if (shouldUseDrawable(key.label)) {
            drawKeyDrawable(canvas, key, textColor);
        } else {
            textPaint.setColor(textColor);
            String displayText = getDisplayText(key.label);
            float centerX = key.x + key.width / 2;

            if (isSymbolMode) {
                // In symbol mode, center the text perfectly
                float centerY = key.y + key.height / 2 + textPaint.getTextSize() / 2;
                canvas.drawText(displayText, centerX, centerY, textPaint);
            } else {
                // In QWERTY mode, position main text lower to make room for alternatives
                float mainTextY = key.y + key.height / 2 + textPaint.getTextSize() / 2 + 8;
                canvas.drawText(displayText, centerX, mainTextY, textPaint);

                // Draw alternative character preview (smaller, less opaque)
                String[] alternatives = RADE_ALTS.get(key.label.toLowerCase());
                Integer previewCount = ALT_PREVIEW_COUNT.get(key.label.toLowerCase());

                if (alternatives != null && alternatives.length > 0 && previewCount != null && previewCount > 0) {
                    // Save original text size and color
                    float originalSize = textPaint.getTextSize();
                    int originalColor = textPaint.getColor();

                    // Set smaller size and semi-transparent color (always use onSurfaceColor for previews)
                    textPaint.setTextSize(originalSize * 0.7f);
                    textPaint.setColor(Color.argb(128, Color.red(onSurfaceColor), Color.green(onSurfaceColor), Color.blue(onSurfaceColor))); // 50% opacity of normal text color

                    // Calculate how many alternatives to show (don't exceed available alternatives)
                    int numToShow = Math.min(previewCount, alternatives.length);

                    if (numToShow == 1) {
                        // Single alternative - center it above main text
                        float altY = key.y + key.height / 2 - originalSize * 0.6f;
                        canvas.drawText(alternatives[0], centerX, altY, textPaint);
                    } else {
                        // Multiple alternatives - spread them horizontally
                        float totalWidth = key.width * 0.4f; // Use 80% of key width
                        float spacing = totalWidth / (numToShow + 1); // Evenly space them
                        float startX = key.x + key.width * 0.3f; // Start at 10% from left edge
                        float altY = key.y + key.height / 2 - originalSize * 0.6f;

                        for (int i = 0; i < numToShow; i++) {
                            float altX = startX + spacing * (i + 1);
                            canvas.drawText(alternatives[i], altX, altY, textPaint);
                        }
                    }

                    // Restore original paint settings
                    textPaint.setTextSize(originalSize);
                    textPaint.setColor(originalColor);
                }
            }
        }
    }
    private boolean shouldUseDrawable(String label) {
        return label.equals("SHIFT") || label.equals("DELETE") || label.equals("ENTER");
    }

    private void drawKeyDrawable(Canvas canvas, Key key, int tintColor) {
        int drawableRes = getDrawableForKey(key.label);
        if (drawableRes != 0) {
            Drawable drawable = getContext().getResources().getDrawable(drawableRes);
            drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);

            int size = (int)(Math.min(key.width, key.height) * 0.4f);
            int left = (int)(key.x + (key.width - size) / 2);
            int top = (int)(key.y + (key.height - size) / 2);

            drawable.setBounds(left, top, left + size, top + size);
            drawable.draw(canvas);
        }
    }

    private int getDrawableForKey(String label) {
        switch (label) {
            case "SHIFT": return R.drawable.ic_shift;
            case "DELETE": return R.drawable.ic_delete;
            case "ENTER": return R.drawable.ic_enter;
            default: return 0;
        }
    }

    private String getDisplayText(String label) {
        switch (label) {
            case "SPACE": return "space";
            case "SYM": return isSymbolMode ? "ABC" : "123";
            case "ABC": return "ABC";  // Add this line
            default:
                // Apply uppercase for letters when shift or caps lock is on
                if ((isShiftPressed || isCapsLock) && label.length() == 1 && Character.isLetter(label.charAt(0))) {
                    return label.toUpperCase();
                }
                return label;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Key touchedKey = findKeyAt(x, y);
                if (touchedKey != null) {
                    pressedKey = touchedKey;
                    animateKeyPress(true);
                    invalidate();
                    startLongPressTimer(touchedKey);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isLongPressing) {
                    handleAlternativeSelection(x, y);
                    return true;
                } else if (pressedKey != null) {
                    Key currentKey = findKeyAt(x, y);
                    if (currentKey != pressedKey) {
                        cancelLongPressTimer();
                        animateKeyPress(false);
                        pressedKey = null;
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                cancelLongPressTimer(); // Add this line

                if (isLongPressing) {
                    handleAlternativeCommit();
                } else if (pressedKey != null) {
                    handleKeyPress(pressedKey);
                }

                // Reset all state
                if (pressedKey != null) {
                    animateKeyPress(false);
                    pressedKey = null;
                }
                isLongPressing = false;
                selectedAltIndex = -1;
                hideLongPressPopup();
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLongPressTimer();
                if (pressedKey != null) {
                    animateKeyPress(false);
                    pressedKey = null;
                }
                isLongPressing = false;
                selectedAltIndex = -1;
                hideLongPressPopup();
                invalidate();
                return true;
        }

        return true;
    }

    private void handleAlternativeSelection(float x, float y) {
        if (altButtons.isEmpty() || longPressPopup == null || !longPressPopup.isShowing()) {
            return;
        }

        // Convert touch coordinates to screen coordinates
        int[] keyboardLocation = new int[2];
        this.getLocationOnScreen(keyboardLocation);
        float screenX = x + keyboardLocation[0];
        float screenY = y + keyboardLocation[1];

        int newSelectedIndex = -1;

        // Check each button to see if touch is inside its bounds
        for (int i = 0; i < altButtons.size(); i++) {
            View button = altButtons.get(i);
            int[] buttonLocation = new int[2];
            button.getLocationOnScreen(buttonLocation);

            // Button bounds in screen coordinates
            int left = buttonLocation[0];
            int top = buttonLocation[1];
            int right = left + button.getWidth();
            int bottom = top + button.getHeight();

            if (screenX >= left && screenX <= right && screenY >= top && screenY <= bottom) {
                newSelectedIndex = i;
                break;
            }
        }

        // Update selection only if it changed
        if (newSelectedIndex != selectedAltIndex) {
            // Reset previously selected button
            if (selectedAltIndex != -1 && selectedAltIndex < altButtons.size()) {
                resetButtonBackground(selectedAltIndex);
            }

            // Highlight newly selected button
            if (newSelectedIndex != -1) {
                highlightButton(newSelectedIndex);
            }

            selectedAltIndex = newSelectedIndex;
        }
    }
    private void resetButtonBackground(int index) {
        if (index >= 0 && index < altButtons.size()) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(surfaceVariantColor);
            bg.setCornerRadius(8);
            altButtons.get(index).setBackground(bg);
            altButtons.get(index).setTextColor(onSurfaceColor);
        }
    }

    private void highlightButton(int index) {
        if (index >= 0 && index < altButtons.size()) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(primaryColor);
            bg.setCornerRadius(8);
            altButtons.get(index).setBackground(bg);
            altButtons.get(index).setTextColor(onPrimaryColor);
        }
    }

    private void handleAlternativeCommit() {
        if (keyPressListener == null || longPressedKey == null) return;

        if (selectedAltIndex >= 0 && selectedAltIndex < altButtons.size()) {
            // User selected an alternative
            String selectedText = altButtons.get(selectedAltIndex).getText().toString();
            keyPressListener.onKeyPressed(selectedText, selectedText.charAt(0));
        } else {
            // No alternative selected - use the first alternative character
            String[] alternatives = RADE_ALTS.get(longPressedKey.label.toLowerCase());
            if (alternatives != null && alternatives.length > 0) {
                String firstAlt = alternatives[0];

                // Apply case logic if it's a letter
                String outputText = firstAlt;
                if ((isShiftPressed || isCapsLock) && firstAlt.length() == 1 && Character.isLetter(firstAlt.charAt(0))) {
                    outputText = firstAlt.toUpperCase();
                }

                keyPressListener.onKeyPressed(outputText, outputText.charAt(0));
            } else {
                // Fallback to original if no alternatives exist (shouldn't happen in long press context)
                String originalLabel = longPressedKey.label;
                String outputText = originalLabel;
                if ((isShiftPressed || isCapsLock) && originalLabel.length() == 1 && Character.isLetter(originalLabel.charAt(0))) {
                    outputText = originalLabel.toUpperCase();
                }
                keyPressListener.onKeyPressed(outputText, outputText.charAt(0));
            }
        }
    }
    private void startLongPressTimer(Key key) {
        // Only start timer for keys with alternatives
        if (!RADE_ALTS.containsKey(key.label.toLowerCase())) {
            return;
        }

        longPressedKey = key;
        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                isLongPressing = true;
                showLongPressPopup(key);
            }
        };
        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
    }

    private void cancelLongPressTimer() {
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
        hideLongPressPopup();
    }

    private void showLongPressPopup(Key key) {
        String[] alternatives = RADE_ALTS.get(key.label.toLowerCase());
        if (alternatives == null || alternatives.length == 0) return;

        // Create a proper popup with all alternatives
        showAlternativesPopup(key, alternatives);

        // Add vibration feedback for long press
        if (vibrationEnabled){
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(100);
                    }
                }
            } catch (Exception e) {
                // Silent fail
            }
        }

    }

    private void showAlternativesPopup(Key key, String[] alternatives) {
        if (longPressPopup != null && longPressPopup.isShowing()) {
            longPressPopup.dismiss();
        }

        altButtons.clear();
        selectedAltIndex = -1;

        // Create popup layout
        android.widget.LinearLayout popupLayout = new android.widget.LinearLayout(getContext());
        popupLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        popupLayout.setBackgroundColor(surfaceColor);
        popupLayout.setPadding(16, 12, 16, 12);

        // Add border and rounded corners
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(surfaceColor);
        background.setCornerRadius(12);
        background.setStroke(2, primaryColor);
        popupLayout.setBackground(background);

        // Add shadow/elevation effect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            popupLayout.setElevation(16);
        }

        // Calculate standard key width
        float standardKeyWidth = key.width;

        // Create buttons for each alternative
        for (int i = 0; i < alternatives.length; i++) {
            final String alt = alternatives[i];

            // Apply uppercase if shift is pressed and it's a letter
            String displayText = alt;
            if ((isShiftPressed || isCapsLock) && alt.length() == 1 && Character.isLetter(alt.charAt(0))) {
                displayText = alt.toUpperCase();
            }

            android.widget.Button altButton = new android.widget.Button(getContext());
            altButton.setText(displayText);
            altButton.setTextColor(onSurfaceColor);
            altButton.setTextSize(18);

            // Create button background
            android.graphics.drawable.GradientDrawable buttonBg = new android.graphics.drawable.GradientDrawable();
            buttonBg.setColor(surfaceVariantColor);
            buttonBg.setCornerRadius(8);
            altButton.setBackground(buttonBg);

            altButton.setPadding(12, 12, 12, 12);
            altButton.setMinWidth(0);
            altButton.setMinimumWidth(0);

            // Set fixed width based on standard key width
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    (int)standardKeyWidth,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.leftMargin = 8;
            }
            altButton.setLayoutParams(params);

            // Make buttons non-interactive since we handle touch manually
            altButton.setClickable(false);
            altButton.setFocusable(false);

            altButtons.add(altButton);
            popupLayout.addView(altButton);
        }

        // Create popup - SIMPLIFIED FLAGS
        longPressPopup = new PopupWindow(popupLayout,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                false);

        // Simple popup settings
        longPressPopup.setTouchable(false);
        longPressPopup.setOutsideTouchable(false);
        longPressPopup.setFocusable(false);

        // Calculate popup position
        popupLayout.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupLayout.getMeasuredWidth();
        int popupHeight = popupLayout.getMeasuredHeight();

        int popupX = (int)(key.x + key.width / 2 - popupWidth / 2);
        int popupY = (int)(key.y - popupHeight - 20);

        // Keep popup on screen
        if (popupX < 10) popupX = 10;
        if (popupX + popupWidth > getWidth() - 10) popupX = getWidth() - popupWidth - 10;
        if (popupY < 10) popupY = (int)(key.y + key.height + 20);

        longPressPopup.showAtLocation(this, android.view.Gravity.NO_GRAVITY, popupX, popupY);
    }
    private void hideLongPressPopup() {
        if (longPressPopup != null && longPressPopup.isShowing()) {
            longPressPopup.dismiss();
            longPressPopup = null;
        }
    }

    private Key findKeyAt(float x, float y) {
        for (Key key : keys) {
            if (x >= key.x && x <= key.x + key.width &&
                    y >= key.y && y <= key.y + key.height) {
                return key;
            }
        }
        return null;
    }

    private void animateKeyPress(boolean pressed) {
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }

        float targetScale = pressed ? 0.95f : 1.0f;
        pressAnimator = ValueAnimator.ofFloat(pressScale, targetScale);
        pressAnimator.setDuration(100);
        pressAnimator.setInterpolator(new DecelerateInterpolator());
        pressAnimator.addUpdateListener(animation -> {
            pressScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    private void handleKeyPress(Key key) {
        if (keyPressListener == null) return;

        String label = key.label;

        // Handle special keys
        switch (label) {
            case "SHIFT":
                keyPressListener.onSpecialKeyPressed(KEY_SHIFT);
                break;
            case "DELETE":
                keyPressListener.onSpecialKeyPressed(KEY_DELETE);
                break;
            case "ENTER":
                keyPressListener.onSpecialKeyPressed(KEY_ENTER);
                break;
            case "SPACE":
                keyPressListener.onSpecialKeyPressed(KEY_SPACE);
                break;
            case "SYM":
                keyPressListener.onSpecialKeyPressed(KEY_SYMBOL);
                break;
            case "123":
                keyPressListener.onSpecialKeyPressed(KEY_NUMBERS);
                break;
            case ",":
                // Regular comma
                keyPressListener.onKeyPressed(",", ',');
                break;
            case ".":
                // Regular period
                keyPressListener.onKeyPressed(".", '.');
                break;
            case "ABC":
                keyPressListener.onSpecialKeyPressed(KEY_SYMBOL);
                break;
            default:
                // Check if this key has Rade alternatives
                if (RADE_ALTS.containsKey(label.toLowerCase())) {
                    // Regular key with potential alternatives
                    keyPressListener.onKeyPressed(label, label.charAt(0));
                } else {
                    // Regular key
                    keyPressListener.onKeyPressed(label, label.charAt(0));
                }
                break;
        }
    }

    public void setOnKeyPressListener(OnKeyPressListener listener) {
        this.keyPressListener = listener;
    }

    public void updateShiftState(boolean shifted, boolean capsLock) {
        this.isShiftPressed = shifted;
        this.isCapsLock = capsLock;
        invalidate();
    }

    public void toggleSymbolMode() {
        isSymbolMode = !isSymbolMode;
        calculateDimensions(); // Recalculate keyboard height for new layout
        createKeys();
        invalidate();
        requestLayout(); // Request a new layout measurement
    }

    private static class Key {
        String label;
        float x, y, width, height;

        Key(String label, float x, float y, float width, float height) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}