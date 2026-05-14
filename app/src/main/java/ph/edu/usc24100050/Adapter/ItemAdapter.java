package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.BetterItinerary;
import ph.edu.usc24100050.Model.ActivityItem;
import ph.edu.usc24100050.R;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<ActivityItem> activityItems;
    private Context context;
    private String activityName;

    public ItemAdapter(Context context, List<ActivityItem> activityItems, String activityName) {
        this.activityItems = activityItems;
        this.context = context;
        this.activityName = activityName;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ActivityItem item = activityItems.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.tvLocation.setText(item.getLocation());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this.context, BetterItinerary.class);
            intent.putExtra("title", item.getTitle());
            intent.putExtra("location", item.getLocation());
            intent.putExtra("activity", activityName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return activityItems != null ? activityItems.size() : 0;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvLocation;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.txtItemTitle);
            tvDescription = itemView.findViewById(R.id.txtItemDescription);
            tvLocation = itemView.findViewById(R.id.txtItemLocation);
        }
    }
}