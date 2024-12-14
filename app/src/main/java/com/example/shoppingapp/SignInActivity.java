package com.example.shoppingapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {
    private EditText nameField, surnameField, emailField, passwordField, passwordAgainField;
    private Button signInButton;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin_activity_main);

        initializeViews();
        setupFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        nameField = findViewById(R.id.et_name);
        surnameField = findViewById(R.id.et_surname);
        emailField = findViewById(R.id.et_email);
        passwordField = findViewById(R.id.et_password);
        passwordAgainField = findViewById(R.id.et_password_again);
        signInButton = findViewById(R.id.btn_sign_in);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> validateAndSignUp());
    }

    private void validateAndSignUp() {
        String name = nameField.getText().toString().trim();
        String surname = surnameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        String passwordAgain = passwordAgainField.getText().toString();

        if (!validateInputs(name, surname, email, password, passwordAgain)) {
            return;
        }

        showLoading(true);
        createUserInFirestore(name, surname, email, password);
    }

    private boolean validateInputs(String name, String surname, String email,
                                   String password, String passwordAgain) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(passwordAgain)) {
            showToast(getString(R.string.fill_all_fields));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.valid_email_prompt));
            return false;
        }

        if (password.length() < 6) {
            showToast(getString(R.string.password_length_error));
            return false;
        }

        if (!password.equals(passwordAgain)) {
            showError(getString(R.string.error), getString(R.string.passwords_mismatch));
            return false;
        }

        return true;
    }

    private void createUserInFirestore(String name, String surname, String email, String password) {
        Map<String, Object> user = createUserMap(name, surname, email, password);

        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Log.d(TAG, "User created with ID: " + documentReference.getId());
                    showToast(getString(R.string.signup_successful));
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.w(TAG, "Error creating user", e);
                    showError(getString(R.string.error),
                            getString(R.string.create_account_error, e.getMessage()));
                });
    }

    private Map<String, Object> createUserMap(String name, String surname, String email, String password) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("surname", surname);
        user.put("email", email);
        user.put("password", password); // Güvenlik için şifreleme eklenmeli
        user.put("createdAt", System.currentTimeMillis());
        return user;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}