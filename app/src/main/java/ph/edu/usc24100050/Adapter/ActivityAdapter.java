package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ph.edu.usc24100050.Model.DayActivity;
import ph.edu.usc24100050.R;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.MyViewHolder> {

    Context ctx;
    List<DayActivity> activities;
    public ActivityAdapter(Context context, List<DayActivity> activities)
    {
        this.activities = activities;
        ctx = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activities_card, parent, false);
        return new ActivityAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DayActivity activity = activities.get(position);

        holder.lblVenue.setText(activity.getVenue());
        holder.lblActivity.setText(activity.getActivity());
        holder.lblTime.setText(convertMilitaryToStandardTime(activity.getStartTime()));

        holder.imgRemove.setOnClickListener(v -> {
            Toast.makeText(ctx, "Item Removed", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView imgRemove;
        TextView lblTime, lblActivity, lblVenue;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imgRemove = itemView.findViewById(R.id.imgRemove);
            lblTime = itemView.findViewById(R.id.lblTime);
            lblActivity = itemView.findViewById(R.id.lblActivity);
            lblVenue = itemView.findViewById(R.id.lblVenue);
        }
    }


    // other had an sdk min issue so manual for now
    public String convertMilitaryToStandardTime(String militaryTime)
    {
        int hours = Integer.parseInt(militaryTime.substring(0, 2));
        String minutes = militaryTime.substring(2);

        String suffix = (hours >= 12) ? "PM" : "AM";

        int displayHour = (hours % 12 == 0) ? 12 : hours % 12;

        return String.format("%d:%s %s%n", displayHour, minutes, suffix);
    }
}
