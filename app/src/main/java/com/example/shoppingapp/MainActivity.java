package com.example.shoppingapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // UI Elements
    private EditText emailField, passwordField;
    private Button loginButton, signInButton;
    private ProgressBar progressBar;
    private CheckBox rememberMeCheckBox;

    // Firebase
    private FirebaseFirestore db;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private SharedPreferences settingsPrefs;
    public static final String PREF_NAME = "UserPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_USER_SURNAME = "userSurname";
    public static final String KEY_REMEMBER_ME = "rememberMe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Dark mode ve dil ayarlarını kontrol et
        settingsPrefs = getSharedPreferences("settings_preferences", MODE_PRIVATE);
        applySettings();

        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences and check login status
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check remember me and login status
        if (checkRememberMeStatus() && checkLoginStatus()) {
            navigateToShoppingList();
            return;
        }

        setContentView(R.layout.activity_main);
        initializeViews();
        setupFirebase();
        setupClickListeners();
        loadRememberedData();
    }

    private void applySettings() {
        // Dark Mode kontrolü
        boolean isDarkMode = settingsPrefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Dil kontrolü
        String languageCode = settingsPrefs.getString("language", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void initializeViews() {
        emailField = findViewById(R.id.et_email);
        passwordField = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        signInButton = findViewById(R.id.btn_sign_in);
        progressBar = findViewById(R.id.progressBar);
        rememberMeCheckBox = findViewById(R.id.cb_remember_me);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> validateAndLogin());
        signInButton.setOnClickListener(v -> navigateToSignIn());
    }

    private void loadRememberedData() {
        if (checkRememberMeStatus()) {
            String savedEmail = sharedPreferences.getString(KEY_USER_EMAIL, "");
            emailField.setText(savedEmail);
            rememberMeCheckBox.setChecked(true);
        }
    }

    private boolean checkLoginStatus() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private boolean checkRememberMeStatus() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    private void validateAndLogin() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        showLoading(true);
        checkUserCredentials(email, password);
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast(getString(R.string.fill_all_fields));
            return false;
        }
        return true;
    }

    private void checkUserCredentials(String email, String password) {
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        handleSuccessfulLogin(task.getResult(), email);
                    } else {
                        handleFailedLogin(task.getException());
                    }
                });
    }

    private void handleSuccessfulLogin(QuerySnapshot result, String email) {
        if (result != null && !result.isEmpty()) {
            String userName = result.getDocuments().get(0).getString("name");
            String userSurname = result.getDocuments().get(0).getString("surname");

            saveUserData(email, userName, userSurname);
            showToast(getString(R.string.login_successful));
            navigateToShoppingList();
        } else {
            showError(getString(R.string.error), getString(R.string.invalid_credentials));
        }
    }

    private void handleFailedLogin(Exception exception) {
        showError(getString(R.string.error), getString(R.string.login_error) + exception.getMessage());
    }

    private void saveUserData(String email, String name, String surname) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_SURNAME, surname);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMeCheckBox.isChecked());
        editor.apply();

        Log.d("SharedPrefs", "Saved Data - Email: " + email + ", Name: " + name + ", Surname: " + surname);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        signInButton.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void navigateToShoppingList() {
        Intent intent = new Intent(MainActivity.this, ShoppingListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
    }

    public static void logout(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
        String savedEmail = prefs.getString(KEY_USER_EMAIL, "");

        editor.clear();

        if (rememberMe) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_USER_EMAIL, savedEmail);
        }

        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ayarları kontrol et
        applySettings();
    }
}