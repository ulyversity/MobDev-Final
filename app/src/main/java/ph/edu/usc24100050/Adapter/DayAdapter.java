package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ph.edu.usc24100050.Model.DayPlan;
import ph.edu.usc24100050.Model.Itinerary;
import ph.edu.usc24100050.R;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.MyViewHolder> {

    private Context ctx;
    private Itinerary itinerary;
    private int dayCounter;
    public DayAdapter(Context context, Itinerary itinerary)
    {
        this.itinerary = itinerary;
        dayCounter = 1;
        ctx = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.days_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DayPlan dayPlan = itinerary.getDays().get(position);

        holder.lblDayCounter.setText("Day " + dayCounter);
        dayCounter++;

        holder.lblDate.setText(dayPlan.getDate());

        ActivityAdapter adapter = new ActivityAdapter(ctx, dayPlan.getActivities());
        holder.rvActivities.setLayoutManager(new LinearLayoutManager(ctx));
        holder.rvActivities.setAdapter(adapter);

        holder.btnAddActivity.setOnClickListener(v -> {
            Toast.makeText(ctx, "Activity Added!", Toast.LENGTH_LONG).show();
        });
    }


    @Override
    public int getItemCount() {
        return itinerary.getDays().size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView lblDayCounter, lblDate;
        Button btnAddActivity;
        RecyclerView rvActivities;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            lblDayCounter = itemView.findViewById(R.id.lblDayCounter);
            lblDate  = itemView.findViewById(R.id.lblDate);
            rvActivities = itemView.findViewById(R.id.rvActivities);
            btnAddActivity = itemView.findViewById(R.id.imgAddActivity);
        }
    }
}
