package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.ItineraryResultActivity;
import ph.edu.usc24100050.Model.DayActivity;
import ph.edu.usc24100050.Model.UserItineraryPreference;
import ph.edu.usc24100050.R;

public class CitiesAdapter extends RecyclerView.Adapter<CitiesAdapter.MyViewHolder> {


    Context ctx;
    List<String> cities;
    String activity;
    String previousPrompt;
    public CitiesAdapter(Context context, UserItineraryPreference userItineraryPreference, String previousPrompt)
    {
        this.cities = userItineraryPreference.getCities();
        activity = userItineraryPreference.getActivityName();
        ctx = context;
        this.previousPrompt = previousPrompt;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.place_card, parent, false);
        return new CitiesAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String city = cities.get(position);
        holder.txtCity.setText(city);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, ItineraryResultActivity.class);
            String myPrompt = String.format("%s at %s", previousPrompt, activity);
            intent.putExtra("prompt", myPrompt);
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView txtCity;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCity = itemView.findViewById(R.id.txtCity);
        }
    }
}
