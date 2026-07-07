package com.corriestroup.radekeyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
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

        float density = getResources().getDisplayMetrics().density;
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (HEIGHT_DP * density)));

        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        // Match ModernKeyboardView's palette.
        int surfaceColor = Color.parseColor(isDark ? "#1c1c1e" : "#e5e5e5");
        int textColor = Color.parseColor(isDark ? "#e5e5e5" : "#4f4f4f");
        setBackgroundColor(surfaceColor);

        for (int i = 0; i < SLOT_COUNT; i++) {
            TextView slot = new TextView(context);
            slot.setGravity(Gravity.CENTER);
            slot.setTextColor(textColor);
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
