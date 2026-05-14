package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.Model.ActivityCategory;
import ph.edu.usc24100050.R;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<ActivityCategory> categories;
    private Context context;
    private String activityName;
    public CategoryAdapter(Context context, List<ActivityCategory> categories, String activityName) {
        this.categories = categories;
        this.context = context;
        this.activityName = activityName;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Replace 'item_category' with your actual layout file name
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ActivityCategory category = categories.get(position);

        holder.tvCategoryName.setText(category.getCategoryName());

        // Parse the color string (expects format like "#FFFFFF" or "red")
        try {
            holder.itemView.setBackgroundColor(Color.parseColor(category.getBackgroundColor()));
        } catch (Exception e) {
            // Default color if the string is malformed
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        }

        ItemAdapter itemAdapter = new ItemAdapter(this.context, category.getCategoryList(), this.activityName);
        holder.rvItems.setLayoutManager(new LinearLayoutManager(this.context));
        holder.rvItems.setAdapter(itemAdapter);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        RecyclerView rvItems;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Replace with your actual IDs
            tvCategoryName = itemView.findViewById(R.id.txtCategoryName);
            rvItems = itemView.findViewById(R.id.rvActivityItems);
        }
    }
}
