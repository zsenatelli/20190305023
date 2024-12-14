package com.example.shoppingapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NewListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialAdapter2 adapter;
    private ArrayList<Material> materialList = new ArrayList<>();
    private EditText nameInput, dateInput;
    private ImageButton addButton;
    private Button saveButton;
    private FirebaseFirestore db;
    private String userId;
    private static final String TAG = "NewListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_list);

        initializeFirebase();
        getUserId();
        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupSwipeToDelete();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        String userEmail = sharedPreferences.getString(MainActivity.KEY_USER_EMAIL, "");

        if (!TextUtils.isEmpty(userEmail)) {
            db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        } else {
                            showError(getString(R.string.error), getString(R.string.user_not_found));
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showError(getString(R.string.error), getString(R.string.user_id_fetch_error));
                        finish();
                    });
        } else {
            showError(getString(R.string.error), getString(R.string.user_not_found));
            finish();
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.materialRecyclerView);
        nameInput = findViewById(R.id.nameInput);
        dateInput = findViewById(R.id.dateInput);
        addButton = findViewById(R.id.addButton);
        saveButton = findViewById(R.id.saveButton);

        dateInput.setFocusable(false);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaterialAdapter2(materialList);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        dateInput.setOnClickListener(v -> showDatePickerDialog());
        addButton.setOnClickListener(v -> addNewMaterial());
        saveButton.setOnClickListener(v -> saveToFirebase());
    }

    private void addNewMaterial() {
        Material material = new Material("");
        materialList.add(material);
        adapter.notifyItemInserted(materialList.size() - 1);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                materialList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format("%d/%d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    dateInput.setText(selectedDate);
                },
                year,
                month,
                day
        );
        datePickerDialog.show();
    }

    private void saveToFirebase() {
        String name = nameInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(date)) {
            showError(getString(R.string.error), getString(R.string.fill_all_fields));
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            showError(getString(R.string.error), getString(R.string.user_not_found));
            return;
        }

        if (materialList.isEmpty()) {
            showError(getString(R.string.error), getString(R.string.no_materials_added));
            return;
        }

        String listId = UUID.randomUUID().toString();
        Map<String, Object> shoppingList = createShoppingListMap(name, date);

        db.collection("shopping")
                .document(listId)
                .set(shoppingList)
                .addOnSuccessListener(aVoid -> {
                    showToast(getString(R.string.save_success));
                    // ShoppingListActivity'yi yeniden başlat
                    Intent intent = new Intent(NewListActivity.this, ShoppingListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError(getString(R.string.error), getString(R.string.save_error));
                });
    }

    private Map<String, Object> createShoppingListMap(String name, String date) {
        Map<String, Object> shoppingList = new HashMap<>();
        shoppingList.put("name", name);
        shoppingList.put("date", date);
        shoppingList.put("userId", userId);
        shoppingList.put("createdAt", System.currentTimeMillis());

        for (int i = 0; i < materialList.size(); i++) {
            Material material = materialList.get(i);
            shoppingList.put("material" + (i + 1), material.getName());
            shoppingList.put("isSelectedMaterial" + (i + 1), false);
        }

        return shoppingList;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .show();
    }
}