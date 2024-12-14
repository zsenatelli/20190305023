package com.example.shoppingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShoppingListActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private TextView welcomeText;
    private SharedPreferences sharedPreferences;
    private SharedPreferences settingsPrefs;
    private FloatingActionButton btnNewList;
    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<ShoppingList> shoppingLists = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration shoppingListListener;
    private static final String TAG = "ShoppingListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsPrefs = getSharedPreferences("settings_preferences", MODE_PRIVATE);
        applySettings();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        initializeSharedPreferences();
        initializeFirebase();
        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerView();
        setupClickListeners();
        getUserId();
    }

    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.tv_welcome);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnNewList = findViewById(R.id.btn_new_list);
        recyclerView = findViewById(R.id.recycler_shopping_lists);
        setWelcomeMessage();
    }

    private void setWelcomeMessage() {
        String name = sharedPreferences.getString(MainActivity.KEY_USER_NAME, "");
        String surname = sharedPreferences.getString(MainActivity.KEY_USER_SURNAME, "");
        String welcomeMessage = getString(R.string.welcome_message, name, surname);
        welcomeText.setText(welcomeMessage);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);
    }

    private void setupRecyclerView() {
        adapter = new ShoppingListAdapter(this, shoppingLists, list -> {
            Intent intent = new Intent(this, ShoppingDetailActivity.class);
            intent.putExtra("document_id", list.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnNewList.setOnClickListener(v -> openNewListActivity());
    }

    private void getUserId() {
        String userEmail = sharedPreferences.getString(MainActivity.KEY_USER_EMAIL, "");
        if (!TextUtils.isEmpty(userEmail)) {
            db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            fetchShoppingLists();
                        } else {
                            showError(getString(R.string.error), getString(R.string.user_not_found));
                            logout();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showError(getString(R.string.error), getString(R.string.user_id_fetch_error));
                        logout();
                    });
        } else {
            showError(getString(R.string.error), getString(R.string.user_not_found));
            logout();
        }
    }

    private void fetchShoppingLists() {
        if (TextUtils.isEmpty(userId)) {
            showError(getString(R.string.error), getString(R.string.user_not_found));
            return;
        }

        if (shoppingListListener != null) {
            shoppingListListener.remove();
        }

        shoppingListListener = db.collection("shopping")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    shoppingLists.clear();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            ShoppingList list = createShoppingListFromDocument(doc);
                            shoppingLists.add(list);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private ShoppingList createShoppingListFromDocument(QueryDocumentSnapshot doc) {
        ShoppingList list = new ShoppingList();
        list.setId(doc.getId());
        list.setName(doc.getString("name"));
        list.setDate(doc.getString("date"));

        Map<String, Object> data = doc.getData();
        for (String key : data.keySet()) {
            if (key.startsWith("material")) {
                if (!key.startsWith("isSelectedMaterial")) {
                    list.getMaterials().put(key, (String) data.get(key));
                } else {
                    list.getMaterialSelections().put(
                            key.replace("isSelected", ""),
                            (Boolean) data.get(key)
                    );
                }
            }
        }
        return list;
    }

    private boolean handleNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.menu_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openNewListActivity() {
        Intent intent = new Intent(this, NewListActivity.class);
        startActivity(intent);
    }

    private void applySettings() {
        boolean isDarkMode = settingsPrefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        String languageCode = settingsPrefs.getString("language", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void logout() {
        MainActivity.logout(sharedPreferences);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySettings();
        setWelcomeMessage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shoppingListListener != null) {
            shoppingListListener.remove();
        }
    }
}