package com.corriestroup.radekeyboard;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Keyboard options, opened from the gear key on the symbols layout. Every control
 * writes through {@link KeyboardPrefs} immediately; the IME picks the values up the
 * next time the keyboard opens (onStartInputView).
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings_title);

        SwitchCompat hapticsSwitch = findViewById(R.id.switch_haptics);
        RadioGroup durationGroup = findViewById(R.id.group_haptic_duration);
        TextView durationLabel = findViewById(R.id.label_haptic_strength);
        SwitchCompat numberRowSwitch = findViewById(R.id.switch_number_row);

        hapticsSwitch.setChecked(KeyboardPrefs.isHapticsEnabled(this));
        checkDurationButton(durationGroup, KeyboardPrefs.getHapticDurationMs(this));
        setDurationEnabled(durationGroup, durationLabel, hapticsSwitch.isChecked());
        numberRowSwitch.setChecked(KeyboardPrefs.isNumberRowEnabled(this));

        hapticsSwitch.setOnCheckedChangeListener((button, checked) -> {
            KeyboardPrefs.setHapticsEnabled(this, checked);
            setDurationEnabled(durationGroup, durationLabel, checked);
        });

        durationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int duration = durationForButton(checkedId);
            if (duration > 0) {
                KeyboardPrefs.setHapticDurationMs(this, duration);
            }
        });

        numberRowSwitch.setOnCheckedChangeListener((button, checked) ->
                KeyboardPrefs.setNumberRowEnabled(this, checked));
    }

    private void checkDurationButton(RadioGroup group, int durationMs) {
        int id;
        if (durationMs <= 10) {
            id = R.id.haptic_light;
        } else if (durationMs >= 40) {
            id = R.id.haptic_strong;
        } else {
            id = R.id.haptic_medium;
        }
        group.check(id);
    }

    private int durationForButton(int checkedId) {
        if (checkedId == R.id.haptic_light) return 10;
        if (checkedId == R.id.haptic_medium) return 20;
        if (checkedId == R.id.haptic_strong) return 40;
        return -1;
    }

    private void setDurationEnabled(RadioGroup group, TextView label, boolean enabled) {
        label.setEnabled(enabled);
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }
}
