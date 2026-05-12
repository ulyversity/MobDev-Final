package ph.edu.usc24100050;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GetawayAdapter extends RecyclerView.Adapter<GetawayAdapter.GetawayViewHolder> {

    private List<Getaway> getawayList;
    private Context context;

    public GetawayAdapter(Context context, List<Getaway> getawayList) {
        this.context = context;
        this.getawayList = getawayList;
    }

    @NonNull
    @Override
    public GetawayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_getaway, parent, false);
        return new GetawayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GetawayViewHolder holder, int position) {
        Getaway getaway = getawayList.get(position);
        holder.tvDestination.setText(getaway.getDestination());
        holder.tvDates.setText(getaway.getDates());
        holder.tvFare.setText(getaway.getEstimatedFare());
        holder.tvTags.setText(getaway.getTags());

        // Intent-based data passing to a Details Activity
        holder.itemView.setOnClickListener(v -> {
            // Note: TripDetailsActivity was referenced but not found in the file list.
            // I will keep it as a comment or placeholder if it doesn't exist yet.
            // Intent intent = new Intent(context, TripDetailsActivity.class);
            // intent.putExtra("DESTINATION", getaway.getDestination());
            // intent.putExtra("FARE", getaway.getEstimatedFare());
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return getawayList != null ? getawayList.size() : 0;
    }

    public static class GetawayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDestination, tvDates, tvFare, tvTags;

        public GetawayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvFare = itemView.findViewById(R.id.tvFare);
            tvTags = itemView.findViewById(R.id.tvTags);
        }
    }
}
