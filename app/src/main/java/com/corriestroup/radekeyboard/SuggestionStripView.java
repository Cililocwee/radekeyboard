package com.corriestroup.radekeyboard;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * The word-suggestion strip shown above the keyboard: three tappable slots that
 * blank out when there are no candidates. It is a permanent part of the input view
 * (never hidden), so the IME window height stays constant — the same invariant the
 * keyboard's constant-height fix relies on.
 */
public class SuggestionStripView extends LinearLayout {

    /** Callback when the user taps a suggestion. */
    public interface OnSuggestionTapListener {
        void onSuggestionTapped(String word);
    }

    private static final int SLOT_COUNT = 3;
    private static final int HEIGHT_DP = 44;

    private final TextView[] slots = new TextView[SLOT_COUNT];
    private OnSuggestionTapListener listener;

    public SuggestionStripView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);

        for (int i = 0; i < SLOT_COUNT; i++) {
            TextView slot = new TextView(context);
            slot.setGravity(Gravity.CENTER);
            slot.setTextSize(16);
            slot.setMaxLines(1);
            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            slot.setLayoutParams(lp);
            slot.setOnClickListener(v -> {
                CharSequence text = ((TextView) v).getText();
                if (text.length() > 0 && listener != null) {
                    listener.onSuggestionTapped(text.toString());
                }
            });
            slots[i] = slot;
            addView(slot);
        }
        refreshTheme();
    }

    /** Re-resolve the shared palette — used when the theme setting changes live. */
    public void refreshTheme() {
        KeyboardTheme theme = KeyboardTheme.resolve(getContext());
        setBackgroundColor(theme.surface);
        for (TextView slot : slots) {
            slot.setTextColor(theme.onSurface);
        }
    }

    /** The strip's fixed height in pixels — the parent must size it with this. */
    public int getFixedHeightPx() {
        return (int) (HEIGHT_DP * getResources().getDisplayMetrics().density);
    }

    public void setOnSuggestionTapListener(OnSuggestionTapListener listener) {
        this.listener = listener;
    }

    /** Show up to three candidates; extra slots blank. Pass an empty list to clear. */
    public void setSuggestions(List<String> suggestions) {
        List<String> safe = suggestions == null ? Collections.<String>emptyList() : suggestions;
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i].setText(i < safe.size() ? safe.get(i) : "");
        }
    }
}
