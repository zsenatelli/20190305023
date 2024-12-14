package com.example.shoppingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat darkModeSwitch;
    private RadioGroup languageRadioGroup;
    private SharedPreferences sharedPreferences;
    private static final String SETTINGS_PREF = "settings_preferences";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String LANGUAGE_KEY = "language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupToolbar();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        darkModeSwitch = findViewById(R.id.switch_dark_mode);
        languageRadioGroup = findViewById(R.id.radio_group_language);
        sharedPreferences = getSharedPreferences(SETTINGS_PREF, MODE_PRIVATE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings));
        }
    }

    private void loadSettings() {
        // Load Dark Mode setting
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        darkModeSwitch.setChecked(isDarkMode);

        // Load Language setting
        String currentLanguage = sharedPreferences.getString(LANGUAGE_KEY, "en");
        if (currentLanguage.equals("en")) {
            ((RadioButton) findViewById(R.id.radio_english)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radio_turkish)).setChecked(true);
        }
    }

    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setDarkMode(isChecked);
        });

        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String languageCode = checkedId == R.id.radio_english ? "en" : "tr";
            setLanguage(languageCode);
        });
    }

    private void setDarkMode(boolean isDarkMode) {
        // Save preference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DARK_MODE_KEY, isDarkMode);
        editor.apply();

        // Apply dark mode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setLanguage(String languageCode) {
        // Save preference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();

        // Update locale
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Restart app to apply changes
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}