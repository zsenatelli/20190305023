package com.example.shoppingapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MaterialAdapter2 extends RecyclerView.Adapter<MaterialAdapter2.MaterialViewHolder> {
    private List<Material> materials;

    public MaterialAdapter2(List<Material> materials) {
        this.materials = materials;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_material, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        Material material = materials.get(position);
        holder.materialInput.setText(material.getName());

        holder.materialInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                material.setName(s.toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return materials.size();
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        EditText materialInput;

        MaterialViewHolder(View itemView) {
            super(itemView);
            materialInput = itemView.findViewById(R.id.materialInput);
        }
    }
}