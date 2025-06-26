package com.corriestroup.radekeyboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

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

    // Keyboard layouts
    private static final String[][] QWERTY_LAYOUT = {
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DELETE"},
            {"SYM", "SPACE", "ENTER"}
    };

    private static final String[][] SYMBOL_LAYOUT = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "$", "%", "&", "*", "(", ")", "-", "+"},
            {"SYM", "!", "?", ":", ";", "'", "\"", ",", ".", "/"},
            {"SYM", "SPACE", "ENTER"}
    };

    // Vietnamese diacritics map
    private static final Map<String, String[]> VIETNAMESE_ALTS = new HashMap<>();
    static {
        VIETNAMESE_ALTS.put("a", new String[]{"ă", "â", "á", "à", "ạ", "ả", "ã"});
        VIETNAMESE_ALTS.put("e", new String[]{"ê", "é", "è", "ẹ", "ẻ", "ẽ"});
        VIETNAMESE_ALTS.put("i", new String[]{"í", "ì", "ị", "ỉ", "ĩ"});
        VIETNAMESE_ALTS.put("o", new String[]{"ô", "ơ", "ó", "ò", "ọ", "ỏ", "õ"});
        VIETNAMESE_ALTS.put("u", new String[]{"ư", "ú", "ù", "ụ", "ủ", "ũ"});
        VIETNAMESE_ALTS.put("d", new String[]{"đ"});
        VIETNAMESE_ALTS.put("y", new String[]{"ý", "ỳ", "ỵ", "ỷ", "ỹ"});
    }

    private List<Key> keys = new ArrayList<>();
    private Paint keyPaint, textPaint, backgroundPaint;
    private OnKeyPressListener keyPressListener;
    private Key pressedKey;
    private boolean isSymbolMode = false;
    private boolean isShiftPressed = false;
    private boolean isCapsLock = false;

    // Animation
    private ValueAnimator pressAnimator;
    private float pressScale = 1.0f;

    // Dimensions
    private float keyHeight;
    private float keyMargin;
    private int keyboardHeight;

    // Colors (Material Design 3)
    private int surfaceColor = Color.parseColor("#1C1B1F");
    private int onSurfaceColor = Color.parseColor("#E6E0E9");
    private int primaryColor = Color.parseColor("#D0BCFF");
    private int onPrimaryColor = Color.parseColor("#381E72");
    private int surfaceVariantColor = Color.parseColor("#49454F");

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
        keyboardHeight = (int) (keyHeight * 4.5f + keyMargin * 5);

        textPaint.setTextSize(16 * density); // 16sp
    }

    private void createKeys() {
        keys.clear();
        String[][] layout = isSymbolMode ? SYMBOL_LAYOUT : QWERTY_LAYOUT;

        float totalWidth = getWidth();
        if (totalWidth == 0) return; // View not measured yet

        float availableWidth = totalWidth - keyMargin * 2;

        for (int row = 0; row < layout.length; row++) {
            String[] rowKeys = layout[row];
            float y = keyMargin + row * (keyHeight + keyMargin);

            // Calculate total weight for this row
            float totalWeight = 0;
            for (String keyLabel : rowKeys) {
                if (keyLabel.equals("SHIFT") || keyLabel.equals("DELETE")) {
                    totalWeight += 1.5f; // 1.5x width
                } else if (keyLabel.equals("SPACE")) {
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
                } else if (keyLabel.equals("SPACE")) {
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

        if (key == pressedKey) {
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (key.label.equals("SHIFT") && (isShiftPressed || isCapsLock)) {
            keyColor = primaryColor;
            textColor = onPrimaryColor;
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

        // Draw key text
        textPaint.setColor(textColor);
        String displayText = getDisplayText(key.label);

        float centerX = key.x + key.width / 2;
        float centerY = key.y + key.height / 2 + textPaint.getTextSize() / 3;

        canvas.drawText(displayText, centerX, centerY, textPaint);
    }

    private String getDisplayText(String label) {
        switch (label) {
            case "SHIFT": return "⇧";
            case "DELETE": return "⌫";
            case "ENTER": return "↵";
            case "SPACE": return "space";
            case "SYM": return isSymbolMode ? "ABC" : "123";
            default: return label;
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
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (pressedKey != null) {
                    handleKeyPress(pressedKey);
                    animateKeyPress(false);
                    pressedKey = null;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (pressedKey != null) {
                    animateKeyPress(false);
                    pressedKey = null;
                    invalidate();
                }
                return true;
        }

        return super.onTouchEvent(event);
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
            default:
                // Check if this key has Vietnamese alternatives
                if (VIETNAMESE_ALTS.containsKey(label.toLowerCase())) {
                    // For now, just send the base character
                    // Later we can add long-press popup for alternatives
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
        createKeys();
        invalidate();
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