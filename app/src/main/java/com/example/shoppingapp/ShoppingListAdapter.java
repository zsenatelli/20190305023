package com.example.shoppingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private List<ShoppingList> shoppingLists;
    private Context context;
    private OnItemClickListener listener;
    private FirebaseFirestore db;

    public interface OnItemClickListener {
        void onItemClick(ShoppingList list);
    }

    public ShoppingListAdapter(Context context, List<ShoppingList> shoppingLists, OnItemClickListener listener) {
        this.context = context;
        this.shoppingLists = shoppingLists;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingList list = shoppingLists.get(position);
        holder.nameTextView.setText(list.getName());
        holder.dateTextView.setText(list.getDate());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShoppingDetailActivity.class);
            intent.putExtra("document_id", list.getId());
            context.startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> showDeleteConfirmation(list, position));
    }

    private void showDeleteConfirmation(ShoppingList list, int position) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_confirmation_title))
                .setMessage(context.getString(R.string.delete_confirmation_message))
                .setPositiveButton(context.getString(R.string.yes), (dialog, which) -> deleteShoppingList(list, position))
                .setNegativeButton(context.getString(R.string.no), null)
                .show();
    }

    private void deleteShoppingList(ShoppingList list, int position) {
        db.collection("shopping").document(list.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    shoppingLists.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, context.getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, context.getString(R.string.delete_error), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public int getItemCount() {
        return shoppingLists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView dateTextView;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_list_name);
            dateTextView = itemView.findViewById(R.id.tv_list_date);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }


}
