package com.example.shoppingapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShoppingDetailActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String documentId;
    private EditText nameEdit, dateEdit;
    private LinearLayout materialsContainer;
    private FloatingActionButton addButton, saveButton;
    private Map<String, Object> updates = new HashMap<>();
    private int materialCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_detail);
        setupToolbar();
        initializeData();
        initViews();
        setupListeners();
        loadShoppingData();
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeData() {
        documentId = getIntent().getStringExtra("document_id");
        db = FirebaseFirestore.getInstance();
    }

    private void initViews() {
        nameEdit = findViewById(R.id.et_name);
        dateEdit = findViewById(R.id.et_date);
        materialsContainer = findViewById(R.id.materials_container);
        addButton = findViewById(R.id.addButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupListeners() {
        addButton.setOnClickListener(v -> addNewMaterialField());
        saveButton.setOnClickListener(v -> saveChanges());
        setupNameEditListener();
        dateEdit.setOnClickListener(v -> showDatePicker());
    }

    private void setupNameEditListener() {
        nameEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                updates.put("name", s.toString());
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%d/%d/%d", day, month + 1, year);
            dateEdit.setText(date);
            updates.put("date", date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addNewMaterialField() {
        materialCount++;
        addMaterialView("", false, materialCount);
        updates.put("material" + materialCount, "");
        updates.put("isSelectedMaterial" + materialCount, false);
    }

    private void addMaterialView(String materialName, boolean isSelected, int index) {
        LinearLayout itemLayout = createItemLayout();
        CheckBox checkBox = createCheckBox(isSelected, index);
        EditText editText = createMaterialEditText(materialName, index);
        ImageButton deleteButton = createDeleteButton(itemLayout, index);

        itemLayout.addView(checkBox);
        itemLayout.addView(editText);
        itemLayout.addView(deleteButton);
        materialsContainer.addView(itemLayout);
    }

    private LinearLayout createItemLayout() {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        return itemLayout;
    }

    private CheckBox createCheckBox(boolean isSelected, int index) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(isSelected);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                updates.put("isSelectedMaterial" + index, isChecked));
        return checkBox;
    }

    private EditText createMaterialEditText(String materialName, int index) {
        EditText editText = new EditText(this);
        editText.setText(materialName);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        editText.setMinHeight(48);
        editText.setHint(getString(R.string.material_hint));
        setupMaterialEditTextListener(editText, index);
        return editText;
    }

    private void setupMaterialEditTextListener(EditText editText, int index) {
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                updates.put("material" + index, s.toString());
            }
        });
    }

    private ImageButton createDeleteButton(LinearLayout itemLayout, int index) {
        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        deleteButton.setContentDescription(getString(R.string.delete_material));
        deleteButton.setBackgroundResource(android.R.color.transparent);
        deleteButton.setOnClickListener(v -> deleteMaterial(itemLayout, index));
        return deleteButton;
    }

    private void deleteMaterial(LinearLayout itemLayout, int index) {
        materialsContainer.removeView(itemLayout);
        db.collection("shopping").document(documentId)
                .update("material" + index, FieldValue.delete(),
                        "isSelectedMaterial" + index, FieldValue.delete())
                .addOnSuccessListener(aVoid -> reorderMaterials());
    }

    private void reorderMaterials() {
        Map<String, Object> updates = new HashMap<>();
        int newIndex = 1;

        for (int i = 0; i < materialsContainer.getChildCount(); i++) {
            LinearLayout itemLayout = (LinearLayout) materialsContainer.getChildAt(i);
            EditText editText = (EditText) itemLayout.getChildAt(1);
            CheckBox checkBox = (CheckBox) itemLayout.getChildAt(0);

            updates.put("material" + newIndex, editText.getText().toString());
            updates.put("isSelectedMaterial" + newIndex, checkBox.isChecked());
            newIndex++;
        }

        db.collection("shopping").document(documentId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, getString(R.string.materials_reordered), Toast.LENGTH_SHORT).show());
    }

    private void loadShoppingData() {
        db.collection("shopping").document(documentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        processShoppingData(document.getData());
                    }
                });
    }

    private void processShoppingData(Map<String, Object> data) {
        if (data == null) return;

        nameEdit.setText((String) data.get("name"));
        dateEdit.setText((String) data.get("date"));

        List<Map.Entry<String, Object>> sortedEntries = getSortedMaterialEntries(data);
        createMaterialViews(data, sortedEntries);
    }

    private List<Map.Entry<String, Object>> getSortedMaterialEntries(Map<String, Object> data) {
        return data.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("material") && !entry.getKey().startsWith("isSelectedMaterial"))
                .sorted((e1, e2) -> {
                    int num1 = Integer.parseInt(e1.getKey().replace("material", ""));
                    int num2 = Integer.parseInt(e2.getKey().replace("material", ""));
                    return num1 - num2;
                })
                .collect(Collectors.toList());
    }

    private void createMaterialViews(Map<String, Object> data, List<Map.Entry<String, Object>> sortedEntries) {
        materialCount = 0;
        for (Map.Entry<String, Object> entry : sortedEntries) {
            materialCount++;
            String materialName = (String) entry.getValue();
            boolean isSelected = Boolean.TRUE.equals(data.get("isSelectedMaterial" + materialCount));
            addMaterialView(materialName, isSelected, materialCount);
        }
    }

    private void saveChanges() {
        if (!updates.isEmpty()) {
            db.collection("shopping").document(documentId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_SHORT).show();
                        updates.clear();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, getString(R.string.save_changes_error), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveChanges();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveChanges();
        super.onBackPressed();
    }
}