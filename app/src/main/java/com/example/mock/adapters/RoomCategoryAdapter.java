package com.example.mock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mock.R;
import java.util.List;

public class RoomCategoryAdapter extends RecyclerView.Adapter<RoomCategoryAdapter.RoomCategoryViewHolder> {

    private List<String> roomCategories;

    public RoomCategoryAdapter(List<String> roomCategories) {
        this.roomCategories = roomCategories;
    }

    @NonNull
    @Override
    public RoomCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_category, parent, false);
        return new RoomCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomCategoryViewHolder holder, int position) {
        String category = roomCategories.get(position);
        holder.tvCategory.setText(category);
    }

    @Override
    public int getItemCount() {
        return roomCategories != null ? roomCategories.size() : 0;
    }

    public static class RoomCategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;

        public RoomCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
