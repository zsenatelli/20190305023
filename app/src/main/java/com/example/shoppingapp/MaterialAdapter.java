package com.example.shoppingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    private ArrayList<Material> materialList;
    private Context context;

    public MaterialAdapter(ArrayList<Material> materialList) {
        this.materialList = materialList;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_material, parent, false);
        return new MaterialViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        Material material = materialList.get(position);
        // String resource kullanımı
        String materialText = context.getString(R.string.material_prefix) + material.getName();
        holder.nameTextView.setText(materialText);

        // Boş tarih metni için açıklayıcı bir string resource kullanımı
        holder.dateTextView.setText(context.getString(R.string.no_date_set));
    }

    @Override
    public int getItemCount() {
        return materialList.size();
    }

    public static class MaterialViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView dateTextView;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.materialName);
            dateTextView = itemView.findViewById(R.id.materialDate);
        }
    }
}