package com.corriestroup.radekeyboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

/**
 * Keyboard options rendered as an overlay INSIDE the IME window (it swaps in for
 * the keyboard at the same size — never a separate screen). Opened by the gear
 * key on the symbols layout; every control writes through {@link KeyboardPrefs}.
 */
public class SettingsPanelView extends FrameLayout {

    /** Callbacks for the hosting service. */
    public interface Listener {
        /** Done tapped — restore the keyboard and re-apply prefs. */
        void onDone();

        /** Theme choice changed — re-theme keyboard chrome immediately. */
        void onThemeChanged();
    }

    private Listener listener;
    private RadioGroup themeGroup;
    private SwitchCompat hapticsSwitch;
    private RadioGroup durationGroup;
    private TextView durationLabel;
    private SwitchCompat numberRowSwitch;
    private boolean binding = false; // suppress listeners while loading values

    public SettingsPanelView(Context context) {
        // The IME service context has no AppCompat theme; inflating SwitchCompat
        // with it crashes on first layout. Wrap in the app theme.
        super(new android.view.ContextThemeWrapper(context, R.style.AppTheme));
        LayoutInflater.from(getContext()).inflate(R.layout.settings_panel, this, true);

        themeGroup = findViewById(R.id.group_theme);
        hapticsSwitch = findViewById(R.id.switch_haptics);
        durationGroup = findViewById(R.id.group_haptic_duration);
        durationLabel = findViewById(R.id.label_haptic_strength);
        numberRowSwitch = findViewById(R.id.switch_number_row);
        Button doneButton = findViewById(R.id.btn_done);

        doneButton.setOnClickListener(v -> {
            if (listener != null) listener.onDone();
        });

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding) return;
            String theme = KeyboardTheme.THEME_LIGHT;
            if (checkedId == R.id.theme_dark) theme = KeyboardTheme.THEME_DARK;
            else if (checkedId == R.id.theme_system) theme = KeyboardTheme.THEME_SYSTEM;
            KeyboardPrefs.setTheme(getContext(), theme);
            applyTheme();
            if (listener != null) listener.onThemeChanged();
        });

        hapticsSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (binding) return;
            KeyboardPrefs.setHapticsEnabled(getContext(), checked);
            setDurationEnabled(checked);
        });

        durationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding) return;
            if (checkedId == R.id.haptic_light) {
                KeyboardPrefs.setHapticDurationMs(getContext(), 10);
            } else if (checkedId == R.id.haptic_medium) {
                KeyboardPrefs.setHapticDurationMs(getContext(), 20);
            } else if (checkedId == R.id.haptic_strong) {
                KeyboardPrefs.setHapticDurationMs(getContext(), 40);
            }
        });

        numberRowSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (binding) return;
            KeyboardPrefs.setNumberRowEnabled(getContext(), checked);
        });

        refresh();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Re-read prefs into the controls and re-apply the palette. Call before showing. */
    public void refresh() {
        binding = true;
        String theme = KeyboardPrefs.getTheme(getContext());
        if (KeyboardTheme.THEME_DARK.equals(theme)) {
            themeGroup.check(R.id.theme_dark);
        } else if (KeyboardTheme.THEME_SYSTEM.equals(theme)) {
            themeGroup.check(R.id.theme_system);
        } else {
            themeGroup.check(R.id.theme_light);
        }

        boolean haptics = KeyboardPrefs.isHapticsEnabled(getContext());
        hapticsSwitch.setChecked(haptics);
        setDurationEnabled(haptics);
        int duration = KeyboardPrefs.getHapticDurationMs(getContext());
        if (duration <= 10) {
            durationGroup.check(R.id.haptic_light);
        } else if (duration >= 40) {
            durationGroup.check(R.id.haptic_strong);
        } else {
            durationGroup.check(R.id.haptic_medium);
        }

        numberRowSwitch.setChecked(KeyboardPrefs.isNumberRowEnabled(getContext()));
        binding = false;

        applyTheme();
    }

    private void setDurationEnabled(boolean enabled) {
        durationLabel.setEnabled(enabled);
        for (int i = 0; i < durationGroup.getChildCount(); i++) {
            durationGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    /** Tint the panel with the shared palette so it matches the keyboard. */
    private void applyTheme() {
        KeyboardTheme theme = KeyboardTheme.resolve(getContext());
        setBackgroundColor(theme.surface);
        tintTextViews(this, theme.onSurface);
        TextView title = findViewById(R.id.settings_title);
        title.setTextColor(theme.primary);
        Button done = findViewById(R.id.btn_done);
        done.setTextColor(theme.primary);
    }

    private static void tintTextViews(View view, int color) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintTextViews(group.getChildAt(i), color);
            }
        }
    }
}
