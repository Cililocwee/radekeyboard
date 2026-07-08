package com.corriestroup.radekeyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

public class ModernKeyboardView extends View {

    // Special key codes
    public static final int KEY_DELETE = -1;
    public static final int KEY_SHIFT = -2;
    public static final int KEY_ENTER = -3;
    public static final int KEY_SPACE = -4;
    public static final int KEY_SYMBOL = -5;
    public static final int KEY_NUMBERS = -6;
    public static final int KEY_DELETE_WORD = -7;

    public static final int KEY_DELETE_SELECTION = -8;
    public static final int KEY_DELETE_CONTINUOUS = -9;
    public static final int KEY_SETTINGS = -10;

    // Layout rows live in KeyboardLayouts (pure, unit-tested); the view only picks
    // which set to render based on symbol mode and the number-row setting.

    // Long-press alternates live in LayerAlternates (per language layer); the view
    // resolves them through effectiveAlts()/previewCountFor() only.
    private KeyboardLayer layer = KeyboardLayer.RADE;

    private List<Key> keys = new ArrayList<>();
    private Paint keyPaint, textPaint, backgroundPaint;
    private OnKeyPressListener keyPressListener;
    private Key pressedKey;
    private boolean isSymbolMode = false;
    private boolean isShiftPressed = false;
    private boolean isCapsLock = false;
    private boolean numberRowEnabled = true;

    // Long press functionality
    private Key longPressedKey;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private PopupWindow longPressPopup;
    private boolean isLongPressing = false;
    // 300ms matches mainstream keyboards; 200ms triggered accidental popups for fast typists.
    private static final int LONG_PRESS_DELAY = 300;
    private List<android.widget.Button> altButtons = new ArrayList<>();
    private int selectedAltIndex = -1; // Track which alternative is currently selected

    // Multi-touch: only one pointer drives a press at a time; a second finger going
    // down commits the first key immediately (fast two-thumb typing).
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float downX, downY;
    private int touchSlop;

    // Space-bar language swipe: horizontal slide past a fraction of the space key's
    // width cycles the language layer instead of committing a space.
    private static final float SPACE_SWIPE_THRESHOLD_FRACTION = 0.25f;
    private Key spaceKeyDown;
    private float spaceDownX;
    private boolean spaceSlideActive = false;
    private int slideDirection = 0; // -1 left (previous), +1 right (next)

    // Haptics (opt-in via settings; fields cached so no prefs read per keystroke)
    private Vibrator vibrator;
    private boolean hapticsEnabled = false;
    private int hapticDurationMs = 20;

    // Dimensions
    private float keyHeight;
    private float keyMargin;
    private int keyboardHeight;
    // Bottom system inset (gesture nav area) on edge-to-edge devices; padding only,
    // keys are laid out above it.
    private int bottomInset = 0;

    // Colors (Material Design 3) — populated by applyThemeColors() based on night mode.
    private int surfaceColor;         // Background
    private int onSurfaceColor;       // Text
    private int primaryColor;         // Accent
    private int onPrimaryColor;       // Text on Accent
    private int surfaceVariantColor;  // Keys

    // Add these fields with your other private fields
    private Handler deleteHandler = new Handler();
    private Runnable continuousDeleteRunnable;
    private boolean isContinuousDeleting = false;
    private static final int CONTINUOUS_DELETE_DELAY = 300; // Time between word deletions
    private static final int CONTINUOUS_DELETE_START_DELAY = 1500; // 2 seconds to start continuous mode


    public interface OnKeyPressListener {
        void onKeyPressed(String key, int keyCode);
        void onSpecialKeyPressed(int specialKey);

        /** Space-bar swipe: {@code +1} = next layer (rightward), {@code -1} = previous. */
        void onLanguageSwipe(int direction);
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
        applyThemeColors();
        setupPaints();
        calculateDimensions();
        createKeys();
        setBackgroundColor(surfaceColor);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        refreshFromPrefs();
    }

    /**
     * Re-read the persisted settings. Called at creation and from the service's
     * onStartInputView, so settings changes apply when the keyboard (re)opens —
     * never mid-session, which would resize the IME window (see calculateDimensions).
     */
    public void refreshFromPrefs() {
        hapticsEnabled = KeyboardPrefs.isHapticsEnabled(getContext());
        hapticDurationMs = KeyboardPrefs.getHapticDurationMs(getContext());
        boolean numberRow = KeyboardPrefs.isNumberRowEnabled(getContext());
        if (numberRow != numberRowEnabled) {
            numberRowEnabled = numberRow;
            calculateDimensions();
            createKeys();
            requestLayout();
            invalidate();
        }
    }

    /**
     * The long-press alternates a key actually shows: the active layer's base
     * alternates, plus its column digit first when the number row is hidden. All
     * alternate lookups must go through here — never read the layer maps directly.
     */
    private String[] effectiveAlts(String label) {
        String[] base = LayerAlternates.alts(layer).get(label.toLowerCase());
        return KeyboardLayouts.effectiveAlternates(label, base, numberRowEnabled);
    }

    private Integer previewCountFor(String label) {
        return LayerAlternates.previewCounts(layer).get(label.toLowerCase());
    }

    /** Set the active language layer (drawn on the space bar; selects alternates). */
    public void setLanguageLayer(KeyboardLayer newLayer) {
        if (newLayer != null && newLayer != layer) {
            layer = newLayer;
            invalidate();
        }
    }

    /**
     * Key-down feedback. No-op unless enabled in settings; reads only cached fields
     * so it stays off the SharedPreferences path per keystroke.
     */
    private void performKeyHaptic() {
        if (!hapticsEnabled || vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    hapticDurationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(hapticDurationMs);
        }
    }

    /** Pull the shared palette (KeyboardTheme honors the user's theme setting). */
    private void applyThemeColors() {
        KeyboardTheme theme = KeyboardTheme.resolve(getContext());
        primaryColor = theme.primary;
        surfaceColor = theme.surface;
        surfaceVariantColor = theme.surfaceVariant;
        onSurfaceColor = theme.onSurface;
        onPrimaryColor = theme.onPrimary;
    }

    /** Re-resolve colors and repaint — used when the theme setting changes live. */
    public void refreshTheme() {
        applyThemeColors();
        keyPaint.setColor(surfaceVariantColor);
        textPaint.setColor(onSurfaceColor);
        backgroundPaint.setColor(surfaceColor);
        setBackgroundColor(surfaceColor);
        invalidate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-theme if day/night flipped while visible (matters in "system" mode).
        refreshTheme();
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

    private String[][] currentLayout() {
        return isSymbolMode ? KeyboardLayouts.symbolRows()
                : KeyboardLayouts.qwertyRows(numberRowEnabled);
    }

    private void calculateDimensions() {
        float density = getResources().getDisplayMetrics().density;
        keyHeight = 48 * density; // 48dp
        keyMargin = 4 * density;  // 4dp

        // The keyboard height is CONSTANT across QWERTY/symbol layouts — it is always
        // derived from the QWERTY row count. Symbol rows stretch to fill the same
        // total instead (see createKeys). A mid-session IME window resize on layout
        // toggle is what glitched the symbols view on Samsung One UI; only the
        // number-row setting (applied between sessions) may change the height.
        int numberOfRows = KeyboardLayouts.heightRowCount(numberRowEnabled);

        // Reduce top margin to make keyboard shorter
        float topMargin = keyMargin * 0.2f; // Use half the normal margin for top
        keyboardHeight = (int) (keyHeight * numberOfRows + topMargin + keyMargin * numberOfRows);

        textPaint.setTextSize(16 * density); // 16sp
    }

    private void createKeys() {
        keys.clear();
        String[][] layout = currentLayout();

        float totalWidth = getWidth();
        if (totalWidth == 0) return; // View not measured yet

        float availableWidth = totalWidth - keyMargin * 2;
        float topMargin = keyMargin * 0.5f; // Use reduced top margin

        // Stretch this layout's rows to fill the fixed keyboardHeight: with the
        // 5-row QWERTY this equals keyHeight; the 4-row symbol layout gets taller keys.
        int rowCount = layout.length;
        float rowHeight = (keyboardHeight - topMargin - keyMargin * rowCount) / rowCount;

        for (int row = 0; row < layout.length; row++) {
            String[] rowKeys = layout[row];
            float y = topMargin + row * (rowHeight + keyMargin); // Use reduced top margin

            // Calculate total weight for this row
            float totalWeight = 0;
            for (String keyLabel : rowKeys) {
                if (keyLabel.equals("SHIFT") || keyLabel.equals("DELETE")) {
                    totalWeight += 1.5f; // 1.5x width
                } else if (keyLabel.equals(" ")) {
                    totalWeight += 5.0f; // 5x width
                } else if (keyLabel.equals("ENTER")) {
                    totalWeight += 1.5f;
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
                } else if (keyLabel.equals(" ")) {
                    keyWidth = unitWidth * 5.0f;
                } else if (keyLabel.equals("ENTER")) {
                    keyWidth = unitWidth * 1.5f;
                } else {
                    keyWidth = unitWidth;
                }

                Key key = new Key(keyLabel, currentX, y, keyWidth, rowHeight);
                keys.add(key);

                currentX += keyWidth + keyMargin;
            }
        }
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createKeys();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            requestApplyInsets();
        }
    }

    @Override
    public android.view.WindowInsets onApplyWindowInsets(android.view.WindowInsets insets) {
        // targetSdk 35 renders edge-to-edge: pad the keyboard above the gesture-nav
        // area so the bottom row isn't under it. Devices that already place the IME
        // window above the nav bar report 0 here, making this a no-op.
        int newInset;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            newInset = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                    | android.view.WindowInsets.Type.mandatorySystemGestures()).bottom;
        } else {
            newInset = insets.getSystemWindowInsetBottom();
        }
        if (newInset != bottomInset && newInset >= 0) {
            bottomInset = newInset;
            requestLayout();
        }
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Honor an exact height if a parent imposes one; otherwise use our own.
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
        } else {
            setMeasuredDimension(width, keyboardHeight + bottomInset);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Guard against a stale/empty key list (createKeys early-returns while the
        // view is unmeasured; a layout toggle in that window would otherwise draw
        // the wrong layout's keys — part of the Samsung symbol-view glitch).
        if (keys.isEmpty() && getWidth() > 0) {
            createKeys();
        }

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
                key.label.equals("SETTINGS") ||
                key.label.equals(",") ||
                key.label.equals(".") ||
                key.label.equals("ENTER");

        if (key == pressedKey) {
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (key.label.equals("SHIFT")) {
            // Special handling for SHIFT key
            if (isCapsLock) {
                keyColor = primaryColor; // Blue background for caps lock
                textColor = onPrimaryColor;
            } else if (isShiftPressed) {
                keyColor = surfaceVariantColor; // Normal background for shift
                textColor = primaryColor;
            } else {
                keyColor = surfaceVariantColor; // Normal background
                textColor = primaryColor;
            }
        } else if (key == longPressedKey && isLongPressing) {
            // Visual feedback for long press
            keyColor = primaryColor;
            textColor = onPrimaryColor;
        } else if (isSpecialKey) {
            // Special keys use primary color as default
            textColor = primaryColor;
        }


        // Pressed feedback is an instant color change (see keyColor above) — the old
        // scale animator shared one scale across keys and glitched under multi-touch.
        RectF keyRect = new RectF(key.x, key.y, key.x + key.width, key.y + key.height);

        // Draw key background with rounded corners
        keyPaint.setColor(keyColor);
        canvas.drawRoundRect(keyRect, 8, 8, keyPaint);


        // Draw key text or drawable
        if (shouldUseDrawable(key.label)) {
            drawKeyDrawable(canvas, key, textColor);
        } else if (key.label.equals(" ")) {
            drawSpacebarLabel(canvas, key, textColor);
        } else {
            textPaint.setColor(textColor);
            String displayText = getDisplayText(key.label);
            float centerX = key.x + key.width / 2;

            // Save original text size
            float originalTextSize = textPaint.getTextSize();

            // Set larger text size for specific keys
            if (key.label.equals(".") || key.label.equals(",")) { // Replace with your desired keys
                textPaint.setTextSize(originalTextSize * 1.5f); // 30% larger
            }

            if (isSymbolMode) {
                // In symbol mode, center the text perfectly
                float centerY = key.y + key.height / 2 + textPaint.getTextSize() / 2;
                canvas.drawText(displayText, centerX, centerY, textPaint);
                // textPaint is shared across keys AND draw passes: without this
                // restore, the 1.5x bump for "."/"," compounds every redraw until
                // the symbol view is unreadable (the "can't see anything" bug).
                textPaint.setTextSize(originalTextSize);
            } else {
                // In QWERTY mode, position main text lower to make room for alternatives
                float mainTextY = key.y + key.height / 2 + textPaint.getTextSize() / 2 + 8;
                canvas.drawText(displayText, centerX, mainTextY, textPaint);

                // Draw alternative character preview (smaller, less opaque)
                String[] alternatives = effectiveAlts(key.label);
                Integer previewCount = previewCountFor(key.label);
                if (previewCount == null && alternatives != null && alternatives.length > 0) {
                    previewCount = 1; // e.g. a digit gained via the number-row setting
                }

                if (alternatives != null && alternatives.length > 0 && previewCount != null && previewCount > 0) {
                    // Save original text size and color
                    float originalSize = textPaint.getTextSize();
                    int originalColor = textPaint.getColor();

                    // Set smaller size and semi-transparent color (always use onSurfaceColor for previews)
                    textPaint.setTextSize(originalSize * 0.7f);
                    textPaint.setColor(Color.argb(128, Color.red(onSurfaceColor), Color.green(onSurfaceColor), Color.blue(onSurfaceColor))); // 50% opacity of normal text color

                    // Calculate how many alternatives to show (don't exceed available alternatives)
                    int numToShow = Math.min(previewCount, alternatives.length);

                    // Adjust positioning for keys with larger fonts
                    boolean isLargerFontKey = key.label.equals(".") || key.label.equals(",");
                    float altTextOffset = isLargerFontKey ? originalSize * 0.3f : originalSize * 0.6f; // More offset for larger font keys

                    if (numToShow == 1) {
                        // Single alternative - center it above main text
                        float altY = key.y + key.height / 2 - altTextOffset;
                        canvas.drawText(alternatives[0], centerX, altY, textPaint);
                    } else {
                        // Multiple alternatives - spread them horizontally
                        float totalWidth, startX;

                        if (isLargerFontKey) {
                            // Extra spacing for comma and period keys
                            totalWidth = key.width * 0.6f; // More spread for these keys
                            startX = key.x + key.width * 0.2f; // Adjusted start position
                        } else {
                            // Normal spacing for other keys
                            totalWidth = key.width * 0.4f;
                            startX = key.x + key.width * 0.3f;
                        }

                        float spacing = totalWidth / (numToShow + 1); // Evenly space them
                        float altY = key.y + key.height / 2 - altTextOffset;

                        for (int i = 0; i < numToShow; i++) {
                            float altX = startX + spacing * (i + 1);
                            canvas.drawText(alternatives[i], altX, altY, textPaint);
                        }
                    }

                    // Restore original paint settings
                    textPaint.setTextSize(originalSize);
                    textPaint.setColor(originalColor);
                }

// Restore original text size
                textPaint.setTextSize(originalTextSize);
            }
        }
    }

    /**
     * The space bar shows the active layer's name (Gboard-style); during a language
     * swipe it previews the layer that releasing would switch to.
     */
    private void drawSpacebarLabel(Canvas canvas, Key key, int pressedTextColor) {
        // The chevrons are always drawn — they advertise that the space bar slides.
        String label;
        int color;
        if (spaceSlideActive && slideDirection != 0) {
            KeyboardLayer target = slideDirection > 0 ? layer.next() : layer.previous();
            label = "‹ " + target.spacebarLabel + " ›";
            color = primaryColor;
        } else if (key == pressedKey) {
            label = "‹ " + layer.spacebarLabel + " ›";
            color = pressedTextColor;
        } else {
            label = "‹ " + layer.spacebarLabel + " ›";
            color = Color.argb(140, Color.red(onSurfaceColor),
                    Color.green(onSurfaceColor), Color.blue(onSurfaceColor));
        }

        float originalSize = textPaint.getTextSize();
        int originalColor = textPaint.getColor();
        textPaint.setTextSize(originalSize * 0.75f);
        textPaint.setColor(color);
        float centerX = key.x + key.width / 2;
        float centerY = key.y + key.height / 2 + textPaint.getTextSize() / 2;
        canvas.drawText(label, centerX, centerY, textPaint);
        textPaint.setTextSize(originalSize);
        textPaint.setColor(originalColor);
    }

    private boolean shouldUseDrawable(String label) {
        return label.equals("SHIFT") || label.equals("DELETE") || label.equals("ENTER")
                || label.equals("SETTINGS");
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
            case "SHIFT":
                if (isCapsLock) {
                    return R.drawable.ic_shift;
                } else if (isShiftPressed) {
                    return R.drawable.ic_shift;
                } else {
                    return R.drawable.ic_shift_hollow;
                }
            case "DELETE": return R.drawable.ic_delete;
            case "ENTER": return R.drawable.ic_enter;
            case "SETTINGS": return R.drawable.ic_settings;
            default: return 0;
        }
    }

    private String getDisplayText(String label) {
        switch (label) {
            case "ABC": return "ABC";
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                beginPress(event.getPointerId(0), event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_POINTER_DOWN: {
                // The language-swipe gesture owns the interaction once active.
                if (spaceSlideActive) {
                    return true;
                }
                // A second finger going down while the first still rests is how fast
                // two-thumb typing looks: commit the current key immediately, then the
                // new pointer takes over.
                if (isLongPressing) {
                    cancelLongPressTimer();
                    handleAlternativeCommit();
                    resetInteractionState();
                } else if (pressedKey != null) {
                    cancelLongPressTimer();
                    handleKeyPress(pressedKey);
                    pressedKey = null;
                }
                int downIndex = event.getActionIndex();
                beginPress(event.getPointerId(downIndex),
                        event.getX(downIndex), event.getY(downIndex));
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                int moveIndex = event.findPointerIndex(activePointerId);
                if (moveIndex < 0) return true;
                float x = event.getX(moveIndex);
                float y = event.getY(moveIndex);

                // Space-bar swipe. This branch swallows all moves that started on the
                // space key (including vertical drift) — it must run before the
                // slide-to-neighbor logic below or drifting off the space bar would
                // kill the gesture.
                if (spaceKeyDown != null) {
                    float dx = x - spaceDownX;
                    float threshold = spaceKeyDown.width * SPACE_SWIPE_THRESHOLD_FRACTION;
                    if (!spaceSlideActive && Math.abs(dx) > threshold) {
                        spaceSlideActive = true;
                        cancelLongPressTimer();
                        pressedKey = null; // releasing must not commit a space
                        performKeyHaptic();
                    }
                    if (spaceSlideActive) {
                        int dir = dx > 0 ? 1 : -1;
                        if (dir != slideDirection) {
                            slideDirection = dir;
                            invalidate();
                        }
                    }
                    return true;
                }

                if (isLongPressing) {
                    handleAlternativeSelection(x, y);
                    return true;
                }
                if (pressedKey != null) {
                    // A wobble within touch slop must not drop or move the press.
                    float dx = x - downX;
                    float dy = y - downY;
                    if (dx * dx + dy * dy <= (float) touchSlop * touchSlop) {
                        return true;
                    }
                    Key currentKey = findKeyAt(x, y);
                    if (currentKey != null && currentKey != pressedKey) {
                        // Slide the press to the neighbor instead of cancelling —
                        // the commit on UP uses wherever the finger ended.
                        cancelLongPressTimer();
                        pressedKey = currentKey;
                        downX = x;
                        downY = y;
                        startLongPressTimer(currentKey);
                        invalidate();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int upIndex = event.getActionIndex();
                if (event.getPointerId(upIndex) == activePointerId) {
                    commitActivePress();
                    // Don't retro-press a finger that was merely resting: further
                    // moves are ignored until the next pointer-down.
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                commitActivePress();
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLongPressTimer();
                stopContinuousDelete();
                resetInteractionState();
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
        }

        return true;
    }

    /** Start tracking {@code pointerId} and press the key under it. */
    private void beginPress(int pointerId, float x, float y) {
        activePointerId = pointerId;
        downX = x;
        downY = y;
        Key touchedKey = findKeyAt(x, y);
        if (touchedKey != null && touchedKey.label.equals(" ")) {
            spaceKeyDown = touchedKey;
            spaceDownX = x;
        } else {
            spaceKeyDown = null;
        }
        spaceSlideActive = false;
        slideDirection = 0;
        if (touchedKey != null) {
            pressedKey = touchedKey;
            performKeyHaptic();
            invalidate();
            startLongPressTimer(touchedKey);
        } else {
            pressedKey = null;
        }
    }

    /** Commit whatever the active pointer was doing (tap, popup selection, or language swipe), then reset. */
    private void commitActivePress() {
        cancelLongPressTimer();
        stopContinuousDelete();
        if (spaceSlideActive) {
            if (keyPressListener != null && slideDirection != 0) {
                keyPressListener.onLanguageSwipe(slideDirection);
            }
        } else if (isLongPressing) {
            handleAlternativeCommit();
        } else if (pressedKey != null) {
            handleKeyPress(pressedKey);
        }
        resetInteractionState();
    }

    private void resetInteractionState() {
        pressedKey = null;
        isLongPressing = false;
        isContinuousDeleting = false;
        selectedAltIndex = -1;
        spaceKeyDown = null;
        spaceSlideActive = false;
        slideDirection = 0;
        hideLongPressPopup();
        invalidate();
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

        // Special handling for DELETE key long press - don't do anything since it already fired
        if (longPressedKey.label.equals("DELETE")) {
            return; // Just return, don't send any additional key presses
        }

        if (selectedAltIndex >= 0 && selectedAltIndex < altButtons.size()) {
            // User selected an alternative
            String selectedText = altButtons.get(selectedAltIndex).getText().toString();
            keyPressListener.onKeyPressed(selectedText, selectedText.charAt(0));
        } else {
            // No alternative selected - use the first alternative character
            String[] alternatives = effectiveAlts(longPressedKey.label);
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
        // Special handling for DELETE key
        if (key.label.equals("DELETE")) {
            longPressedKey = key;
            longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPressing = true;
                    performKeyHaptic();
                    // First long press deletes a word
                    keyPressListener.onSpecialKeyPressed(KEY_DELETE_WORD);

                    // Start continuous deletion after additional delay
                    deleteHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startContinuousDelete();
                        }
                    }, CONTINUOUS_DELETE_START_DELAY - LONG_PRESS_DELAY);
                }
            };
            longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
            return;
        }

        // Rest of your existing long press logic for other keys...
        String[] alternatives = effectiveAlts(key.label);
        if (alternatives == null || alternatives.length == 0) {
            return;
        }

        longPressedKey = key;
        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                isLongPressing = true;
                performKeyHaptic();
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
        stopContinuousDelete(); // Add this line
        hideLongPressPopup();
    }

    private void showLongPressPopup(Key key) {
        String[] alternatives = effectiveAlts(key.label);
        if (alternatives == null || alternatives.length == 0) return;

        // Create a proper popup with all alternatives
        showAlternativesPopup(key, alternatives);
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
        popupLayout.setPadding(24, 18, 24, 18); // Increased from (16, 12, 16, 12)

        // Add border and rounded corners
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(surfaceColor);
        background.setCornerRadius(16); // Increased from 12
        background.setStroke(3, primaryColor); // Increased from 2
        popupLayout.setBackground(background);

        // Add shadow/elevation effect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            popupLayout.setElevation(20); // Increased from 16
        }

        // Calculate standard key width
        float standardKeyWidth = key.width * 1.2f; // Increased by 20%

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
            altButton.setTextSize(22); // Increased from 18

            // Create button background
            android.graphics.drawable.GradientDrawable buttonBg = new android.graphics.drawable.GradientDrawable();
            buttonBg.setColor(surfaceVariantColor);
            buttonBg.setCornerRadius(8);
            altButton.setBackground(buttonBg);

            altButton.setPadding(16, 16, 16, 16); // Increased from (12, 12, 12, 12)
            altButton.setMinWidth(0);
            altButton.setMinimumWidth(0);

            // Set fixed width based on standard key width
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    (int)standardKeyWidth,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.leftMargin = 12; // Increased from 8
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

        // key.x/key.y are view-local; showAtLocation wants window coordinates, and
        // this view no longer sits at the window origin (the suggestion strip is
        // above it) — convert, or every popup lands one strip-height too high.
        int[] windowOffset = new int[2];
        getLocationInWindow(windowOffset);
        int popupX = windowOffset[0] + (int)(key.x + key.width / 2 - popupWidth / 2);
        int popupY = windowOffset[1] + (int)(key.y - popupHeight - 20);

        // Keep popup on screen
        if (popupX < 10) popupX = 10;
        if (popupX + popupWidth > windowOffset[0] + getWidth() - 10) {
            popupX = windowOffset[0] + getWidth() - popupWidth - 10;
        }

        longPressPopup.showAtLocation(this, android.view.Gravity.NO_GRAVITY, popupX, popupY);
    }
    private void hideLongPressPopup() {
        if (longPressPopup != null && longPressPopup.isShowing()) {
            longPressPopup.dismiss();
            longPressPopup = null;
        }
    }

    private Key findKeyAt(float x, float y) {
        // Nearest key by clamped distance: an exact hit has distance 0, and the
        // margins between keys stop being dead zones that drop fast taps.
        Key best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Key key : keys) {
            float d = KeyGeometry.distanceSquaredToRect(x, y,
                    key.x, key.y, key.x + key.width, key.y + key.height);
            if (d < bestDistance) {
                bestDistance = d;
                best = key;
            }
        }
        return best;
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
            case "SETTINGS":
                keyPressListener.onSpecialKeyPressed(KEY_SETTINGS);
                break;
            default:
                keyPressListener.onKeyPressed(label, label.charAt(0));
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

    private void startContinuousDelete() {
        if (!isLongPressing || longPressedKey == null || !longPressedKey.label.equals("DELETE")) {
            return;
        }

        isContinuousDeleting = true;
        continuousDeleteRunnable = new Runnable() {
            @Override
            public void run() {
                if (isContinuousDeleting && isLongPressing) {
                    keyPressListener.onSpecialKeyPressed(KEY_DELETE_CONTINUOUS);
                    deleteHandler.postDelayed(this, CONTINUOUS_DELETE_DELAY);
                }
            }
        };
        deleteHandler.post(continuousDeleteRunnable);
    }

    private void stopContinuousDelete() {
        isContinuousDeleting = false;
        if (continuousDeleteRunnable != null) {
            deleteHandler.removeCallbacks(continuousDeleteRunnable);
            continuousDeleteRunnable = null;
        }
    }
}