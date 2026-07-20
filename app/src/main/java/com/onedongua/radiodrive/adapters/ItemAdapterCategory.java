package com.onedongua.radiodrive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.onedongua.radiodrive.R;
import com.onedongua.radiodrive.data.DataCategory;

import java.util.List;

public class ItemAdapterCategory extends RecyclerView.Adapter<ItemAdapterCategory.CategoryViewHolder> {

    public interface CategoryClickListener {
        void onCategoryClick(DataCategory category);
    }

    private List<DataCategory> categoriesList;
    private int resourceId;
    private CategoryClickListener categoryClickListener;

    class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewName;
        TextView textViewCount;
        ImageView iconView;

        CategoryViewHolder(View itemView) {
            super(itemView);
            textViewName = (TextView) itemView.findViewById(R.id.textViewTop);
            textViewCount = (TextView) itemView.findViewById(R.id.textViewBottom);
            iconView = (ImageView) itemView.findViewById(R.id.iconCategoryViewIcon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (categoryClickListener != null && categoriesList != null) {
                int pos = getAdapterPosition();
                if (pos >= 0 && pos < categoriesList.size()) {
                    categoryClickListener.onCategoryClick(categoriesList.get(pos));
                }
            }
        }
    }

    public ItemAdapterCategory(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setCategoryClickListener(CategoryClickListener categoryClickListener) {
        this.categoryClickListener = categoryClickListener;
    }

    public void updateList(List<DataCategory> categoriesList) {
        this.categoriesList = categoriesList;

        notifyDataSetChanged();
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(resourceId, parent, false);

        return new CategoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        if (categoriesList == null || position >= categoriesList.size()) return;
        final DataCategory category = categoriesList.get(position);

        if (category.Label != null) {
            holder.textViewName.setText(category.Label);
        } else {
            holder.textViewName.setText(category.Name);
        }
        if (category.Icon != null) {
            holder.iconView.setImageDrawable(category.Icon);
        }
        // 仅当 UsedCount 看起来是合法的电台计数时才显示（蜻蜓FM用 UsedCount 存储 ID，不应显示）
        if (category.UsedCount > 0 && category.UsedCount < 100000) {
            holder.textViewCount.setText(String.valueOf(category.UsedCount));
            holder.textViewCount.setVisibility(View.VISIBLE);
        } else {
            holder.textViewCount.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return categoriesList != null ? categoriesList.size() : 0;
    }
}
