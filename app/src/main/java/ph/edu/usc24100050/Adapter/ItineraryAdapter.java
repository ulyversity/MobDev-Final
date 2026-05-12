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
        this.items = list;
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
        holder.tvTime.setText(item.time);
        holder.tvTask.setText(item.task);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvTask;
        VH(View v) {
            super(v);
            tvTime = v.findViewById(R.id.tv_time);
            tvTask = v.findViewById(R.id.tv_task);
        }
    }
}