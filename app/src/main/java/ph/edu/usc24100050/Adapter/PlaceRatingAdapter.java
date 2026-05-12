package ph.edu.usc24100050.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.Model.PlaceRating;
import ph.edu.usc24100050.R;

public class PlaceRatingAdapter extends RecyclerView.Adapter<PlaceRatingAdapter.MyViewHolder> {

    private Context context;
    private List<PlaceRating> placeRatingList;
    private int currentCount;

    public PlaceRatingAdapter(Context context, List<PlaceRating> placeRatingList)
    {
        this.placeRatingList = placeRatingList;
        this.context = context;

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.place_rating_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PlaceRating placeRating = placeRatingList.get(position);

        holder.lblPlaceName.setText(placeRating.getName());
        holder.lblPlaceRating.setText(placeRating.getRating()+"/5.0");
        holder.lblPlaceType.setText(placeRating.getPlaceTypeName());
    }

    @Override
    public int getItemCount() {
        return placeRatingList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView lblPlaceName, lblPlaceRating, lblPlaceType;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            lblPlaceName = itemView.findViewById(R.id.lblPlaceName);
            lblPlaceRating = itemView.findViewById(R.id.lblPlaceRating);
            lblPlaceType = itemView.findViewById(R.id.lblPlaceType);

        }
    }
}
