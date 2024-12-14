package com.example.shoppingapp;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileActivity extends AppCompatActivity {
    private EditText etName, etSurname, etEmail;
    private Button btnSave;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupToolbar();
        setupFirebase();
        loadUserData();
        setupClickListeners();
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void initializeViews() {
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progressBar);
        sharedPreferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.profile_settings));
        }
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserData() {
        String name = sharedPreferences.getString(MainActivity.KEY_USER_NAME, "");
        String surname = sharedPreferences.getString(MainActivity.KEY_USER_SURNAME, "");
        String email = sharedPreferences.getString(MainActivity.KEY_USER_EMAIL, "");

        Log.d(TAG, String.format("Loaded Data - Email: %s, Name: %s, Surname: %s", email, name, surname));

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
            showToast(getString(R.string.user_info_not_found));
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etName.setText(name);
        etSurname.setText(surname);
        etEmail.setText(email);
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String oldEmail = sharedPreferences.getString(MainActivity.KEY_USER_EMAIL, "");

        if (!validateInputs(name, surname, email)) {
            return;
        }

        showConfirmationDialog(name, surname, email, oldEmail);
    }

    private void showConfirmationDialog(String name, String surname, String email, String oldEmail) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirmation_title))
                .setMessage(getString(R.string.save_changes_confirmation))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    showLoading(true);
                    if (!email.equals(oldEmail)) {
                        checkEmailAvailability(email, name, surname);
                    } else {
                        updateUserData(name, surname, email);
                    }
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean validateInputs(String name, String surname, String email) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(email)) {
            showToast(getString(R.string.fill_all_fields));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.valid_email_prompt));
            return false;
        }

        return true;
    }

    private void checkEmailAvailability(String newEmail, String name, String surname) {
        db.collection("users")
                .whereEqualTo("email", newEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && result.isEmpty()) {
                            updateUserData(name, surname, newEmail);
                        } else {
                            showLoading(false);
                            showError(getString(R.string.error), getString(R.string.email_already_in_use));
                        }
                    } else {
                        showLoading(false);
                        showError(getString(R.string.error), getString(R.string.email_check_failed));
                    }
                });
    }

    private void updateUserData(String name, String surname, String email) {
        String oldEmail = sharedPreferences.getString(MainActivity.KEY_USER_EMAIL, "");

        db.collection("users")
                .whereEqualTo("email", oldEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        updateFirestoreAndLocal(task.getResult().getDocuments().get(0).getId(), name, surname, email);
                    } else {
                        showLoading(false);
                        showError(getString(R.string.error), getString(R.string.user_not_found));
                    }
                });
    }

    private void updateFirestoreAndLocal(String documentId, String name, String surname, String email) {
        db.collection("users").document(documentId)
                .update("name", name,
                        "surname", surname,
                        "email", email)
                .addOnSuccessListener(aVoid -> {
                    updateLocalData(name, surname, email);
                    showLoading(false);
                    showToast(getString(R.string.profile_update_success));
                    updateShoppingListAndNavigate();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError(getString(R.string.error), getString(R.string.profile_update_failed));
                });
    }

    private void updateLocalData(String name, String surname, String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MainActivity.KEY_USER_NAME, name);
        editor.putString(MainActivity.KEY_USER_SURNAME, surname);
        editor.putString(MainActivity.KEY_USER_EMAIL, email);
        editor.apply();
    }

    private void updateShoppingListAndNavigate() {
        Intent intent = new Intent(this, ShoppingListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}