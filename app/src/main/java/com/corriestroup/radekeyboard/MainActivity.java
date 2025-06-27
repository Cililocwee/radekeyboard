package com.corriestroup.radekeyboard;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private KeyboardSetupChecker setupChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupChecker = new KeyboardSetupChecker(this);

        // Check if keyboard is properly set up
        if (setupChecker.isKeyboardFullySetup()) {
            // Show testing interface
            showTestingInterface();
        } else {
            // Show onboarding
            startOnboarding();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-check setup status when returning to app
        if (setupChecker.isKeyboardFullySetup()) {
            showTestingInterface();
        }
    }

    private void showTestingInterface() {
        setContentView(R.layout.activity_main_testing);

        TextView title = findViewById(R.id.testing_title);
        title.setText("🎉 Rade Keyboard is Ready!");

        TextView subtitle = findViewById(R.id.testing_subtitle);
        subtitle.setText("Test your Vietnamese keyboard below:");

        Button setupButton = findViewById(R.id.setup_button);
        setupButton.setText("Re-run Setup");
        setupButton.setOnClickListener(v -> startOnboarding());
    }

    private void startOnboarding() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
    }
}