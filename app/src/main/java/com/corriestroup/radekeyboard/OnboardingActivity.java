package com.corriestroup.radekeyboard;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;




public class OnboardingActivity extends AppCompatActivity {

    private KeyboardSetupChecker setupChecker;
    private LinearLayout welcomeScreen, step1Screen, step2Screen, step3Screen, completeScreen;
    private Button nextButton, enableButton, selectButton, finishButton;
    private TextView langEnglish, langVietnamese;

    private ImageView step1Check, step2Check, step3Check;
    private TextView step1Status, step2Status, step3Status;
    private Handler statusHandler;
    private Runnable statusChecker;

    private static final String PREF_LANGUAGE = "selected_language";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved language BEFORE setting content view, but don't recreate
        String savedLanguage = getSavedLanguage();
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        setContentView(R.layout.activity_onboarding);

        setupChecker = new KeyboardSetupChecker(this);
        initViews();
        showWelcomeScreen();

        // Set up automatic status checking
        statusHandler = new Handler(Looper.getMainLooper());
        statusChecker = new Runnable() {
            @Override
            public void run() {
                updateStepStatuses();

                // Auto-advance if steps are completed
                if (setupChecker.isKeyboardFullySetup()) {
                    showStep3();
                } else {
                    // Check again in 2 seconds
                    statusHandler.postDelayed(this, 2000);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Debug logging
        android.util.Log.d("OnboardingActivity", "onResume called");
        android.util.Log.d("OnboardingActivity", "Keyboard enabled: " + setupChecker.isKeyboardEnabled());
        android.util.Log.d("OnboardingActivity", "Keyboard default: " + setupChecker.isKeyboardDefault());
        android.util.Log.d("OnboardingActivity", "Fully setup: " + setupChecker.isKeyboardFullySetup());

        updateStepStatuses();

        // Auto-advance if steps are completed
        if (setupChecker.isKeyboardFullySetup()) {
            showStep3();
        } else {
            // Start automatic checking
            statusHandler.postDelayed(statusChecker, 2000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop automatic checking when activity is paused
        statusHandler.removeCallbacks(statusChecker);
    }

    private void initViews() {
        // Screen containers
        welcomeScreen = findViewById(R.id.welcome_screen);
        step1Screen = findViewById(R.id.step1_screen);
        step2Screen = findViewById(R.id.step2_screen);
        step3Screen = findViewById(R.id.step3_screen);
        completeScreen = findViewById(R.id.complete_screen);

        // Buttons
        nextButton = findViewById(R.id.next_button);
        enableButton = findViewById(R.id.enable_button);
        selectButton = findViewById(R.id.select_button);
        finishButton = findViewById(R.id.finish_button);

        // Language toggle TextViews
        langEnglish = findViewById(R.id.lang_english);
        langVietnamese = findViewById(R.id.lang_vietnamese);

        // Restore the saved language state
        String currentLanguage = getSavedLanguage();
        updateLanguageButtonStates(currentLanguage);


        // Status indicators
        step1Check = findViewById(R.id.step1_check);
        step2Check = findViewById(R.id.step2_check);
        step3Check = findViewById(R.id.step3_check);
        step1Status = findViewById(R.id.step1_status);
        step2Status = findViewById(R.id.step2_status);
        step3Status = findViewById(R.id.step3_status);

        // Set click listeners
        nextButton.setOnClickListener(v -> showStep1());
        enableButton.setOnClickListener(v -> openLanguageSettings());
        selectButton.setOnClickListener(v -> openInputMethodSettings());
        finishButton.setOnClickListener(v -> finishOnboarding());

        // Language toggle listeners
        langEnglish.setOnClickListener(v -> switchLanguage("en"));
        langVietnamese.setOnClickListener(v -> switchLanguage("vi"));
    }

    private void showWelcomeScreen() {
        hideAllScreens();
        welcomeScreen.setVisibility(View.VISIBLE);
    }

    private void showStep1() {
        hideAllScreens();
        step1Screen.setVisibility(View.VISIBLE);
        updateStepStatuses();
    }

    private void showStep2() {
        hideAllScreens();
        step2Screen.setVisibility(View.VISIBLE);
        updateStepStatuses();
    }

    private void showStep3() {
        hideAllScreens();
        step3Screen.setVisibility(View.VISIBLE);
        updateStepStatuses();
    }

    private void showCompleteScreen() {
        hideAllScreens();
        completeScreen.setVisibility(View.VISIBLE);
    }

    private void hideAllScreens() {
        welcomeScreen.setVisibility(View.GONE);
        step1Screen.setVisibility(View.GONE);
        step2Screen.setVisibility(View.GONE);
        step3Screen.setVisibility(View.GONE);
        completeScreen.setVisibility(View.GONE);
    }

    private void updateStepStatuses() {
        KeyboardSetupChecker.SetupStatus status = setupChecker.getSetupStatus();

        // Step 1: Enable keyboard
        if (setupChecker.isKeyboardEnabled()) {
            step1Check.setVisibility(View.VISIBLE);
            step1Status.setText("✅ " + getString(R.string.completed)); // You'll need to add this string
            step1Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // Auto-advance to step 2
            if (step1Screen.getVisibility() == View.VISIBLE) {
                showStep2();
            }
        } else {
            step1Check.setVisibility(View.GONE);
            step1Status.setText(getString(R.string.step1_status_pending));
            step1Status.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Step 2: Set as default
        if (setupChecker.isKeyboardDefault()) {
            step2Check.setVisibility(View.VISIBLE);
            step2Status.setText("✅ " + getString(R.string.completed));
            step2Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // Auto-advance to step 3
            if (step2Screen.getVisibility() == View.VISIBLE) {
                showStep3();
            }
        } else if (setupChecker.isKeyboardEnabled()) {
            step2Check.setVisibility(View.GONE);
            step2Status.setText(getString(R.string.step2_status_pending));
            step2Status.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            step2Check.setVisibility(View.GONE);
            step2Status.setText(getString(R.string.step1_first)); // You'll need to add this string
            step2Status.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Step 3: Test keyboard
        if (setupChecker.isKeyboardFullySetup()) {
            step3Check.setVisibility(View.VISIBLE);
            step3Status.setText("✅ " + getString(R.string.step3_status_ready));
            step3Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            step3Check.setVisibility(View.GONE);
            step3Status.setText(getString(R.string.complete_previous_steps)); // You'll need to add this string
            step3Status.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    private void openLanguageSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            // Fallback for older Android versions
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Go to Languages & Input → Virtual Keyboard", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(this, "Please go to Settings → Languages & Input → Virtual Keyboard", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openInputMethodSettings() {
        try {
            // Try to open the keyboard switcher directly
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showInputMethodPicker();
        } catch (Exception e) {
            // Fallback: Show instructions for manual switching
            Toast.makeText(this, "Long-press the space bar or notification area to switch keyboards", Toast.LENGTH_LONG).show();
        }
    }

    private void finishOnboarding() {
        finish(); // Return to MainActivity
    }

    private void switchLanguage(String languageCode) {
        // Save the language preference first
        saveLanguagePreference(languageCode);

        // Update TextView styles to show selection
        updateLanguageButtonStates(languageCode);


        // Apply locale change
        setLocale(languageCode);
    }

    private void updateLanguageButtonStates(String languageCode) {
        if (languageCode.equals("en")) {
            langEnglish.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            langEnglish.setTypeface(null, android.graphics.Typeface.BOLD);
            langVietnamese.setTextColor(getResources().getColor(android.R.color.darker_gray));
            langVietnamese.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            langVietnamese.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            langVietnamese.setTypeface(null, android.graphics.Typeface.BOLD);
            langEnglish.setTextColor(getResources().getColor(android.R.color.darker_gray));
            langEnglish.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Recreate activity to apply changes
        recreate();
    }

    // Save the language preference
    private void saveLanguagePreference(String languageCode) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString(PREF_LANGUAGE, languageCode)
                .apply();
    }

    // Get saved language preference
    private String getSavedLanguage() {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString(PREF_LANGUAGE, "vi"); // Default language is Vietnamese
    }



}