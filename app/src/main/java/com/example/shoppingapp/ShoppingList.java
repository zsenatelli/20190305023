package com.example.shoppingapp;

import java.util.HashMap;
import java.util.Map;

public class ShoppingList {
    private String id;
    private String name;
    private String date;
    private Map<String, String> materials;
    private Map<String, Boolean> materialSelections;

    public ShoppingList() {
        materials = new HashMap<>();
        materialSelections = new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, String> getMaterials() { return materials; }
    public void setMaterials(Map<String, String> materials) { this.materials = materials; }

    public Map<String, Boolean> getMaterialSelections() { return materialSelections; }
    public void setMaterialSelections(Map<String, Boolean> materialSelections) {
        this.materialSelections = materialSelections;
    }
}
