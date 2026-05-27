package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.Model.ItineraryItemModel;
import ph.edu.usc24100050.R;

public class MyItineraryAdapter extends RecyclerView.Adapter<MyItineraryAdapter.MyViewHolder> {

    private Context ctx;
    private List<ItineraryItemModel> itineraries;
    public MyItineraryAdapter(Context context, List<ItineraryItemModel> itineraryItemModels)
    {
        ctx = context;
        itineraries = itineraryItemModels;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_item_itinerary, parent, false);
        return new MyItineraryAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ItineraryItemModel current = itineraries.get(position);

        holder.txtTime.setText(current.getTime());
        String taskAndLocation = current.getAction() + " @ " + current.getLocation();
        holder.txtTaskAndLocation.setText(taskAndLocation);
    }

    @Override
    public int getItemCount() {
        return itineraries.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime;
        TextView txtTaskAndLocation;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTime = itemView.findViewById(R.id.txtTime);
            txtTaskAndLocation = itemView.findViewById(R.id.txtTaskAndLocation);
        }
    }
}
