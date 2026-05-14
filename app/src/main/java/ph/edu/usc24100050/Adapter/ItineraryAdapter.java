package ph.edu.usc24100050.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ph.edu.usc24100050.Model.ItineraryItem;
import ph.edu.usc24100050.R;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.VH> {

    private List<ItineraryItem> items = new ArrayList<>();

    public void setItems(List<ItineraryItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ItineraryItem item = items.get(position);

        // Time
        holder.tvTime.setText(item.getTime() != null ? item.getTime() : "");

        // Place name with type emoji prefix
        String emoji = getTypeEmoji(item.getPlaceType());
        String placeLine = emoji + " " + (item.getPlaceName() != null ? item.getPlaceName() : "");
        holder.tvTask.setText(placeLine.trim());

        // Notes (includes travel info appended by ChatViewModel)
        if (holder.tvNotes != null) {
            String notes = item.getNotes();
            if (notes != null && !notes.isEmpty()) {
                holder.tvNotes.setVisibility(View.VISIBLE);
                holder.tvNotes.setText(notes);
            } else {
                holder.tvNotes.setVisibility(View.GONE);
            }
        }

        // Duration
        if (holder.tvDuration != null) {
            int mins = item.getDurationMinutes();
            if (mins > 0) {
                holder.tvDuration.setVisibility(View.VISIBLE);
                holder.tvDuration.setText("⏱ " + mins + " min");
            } else {
                holder.tvDuration.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String getTypeEmoji(String type) {
        if (type == null) return "📍";
        switch (type.toUpperCase()) {
            case "HISTORICAL":  return "🏛️";
            case "BEACH":       return "🏖️";
            case "FOOD":        return "🍽️";
            case "NATURE":      return "🌿";
            case "SHOPPING":    return "🛍️";
            case "RELIGIOUS":   return "⛪";
            default:            return "📍";
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvTask, tvNotes, tvDuration;
        VH(View v) {
            super(v);
            tvTime     = v.findViewById(R.id.tv_time);
            tvTask     = v.findViewById(R.id.tv_task);
            // optional — may be null if not in layout
        }
    }
}
