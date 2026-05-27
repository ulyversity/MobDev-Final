package ph.edu.usc24100050;

import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SuggestionAdapter extends ArrayAdapter<Address> {
    private final int resource;
    private final List<Address> addresses;

    public SuggestionAdapter(@NonNull Context context, int resource, @NonNull List<Address> objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.addresses = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);
        }

        Address addr = getItem(position);
        TextView titleView = convertView.findViewById(R.id.suggestionText);
        TextView subTextView = convertView.findViewById(R.id.suggestionSubText);

        if (addr != null) {
            String name = getCleanName(addr);
            titleView.setText(name);
            subTextView.setText(addr.getAddressLine(0));
        }

        return convertView;
    }

    public static String getCleanName(Address addr) {
        String feature = addr.getFeatureName();
        String fullAddress = addr.getAddressLine(0);

        // Hardcode Major Locations
        if (fullAddress != null) {
            String lowerAddr = fullAddress.toLowerCase();
            if (lowerAddr.contains("fuente osmeña") || lowerAddr.contains("fuente circle")) {
                return "Fuente Osmeña Circle";
            }
            if (lowerAddr.contains("ayala center cebu") || lowerAddr.contains("ayala centre cebu")) {
                return "Ayala Center Cebu";
            }
            if (lowerAddr.contains("sm city cebu")) {
                return "SM City Cebu";
            }
            if (lowerAddr.contains("sm seaside")) {
                return "SM Seaside City Cebu";
            }
            if (lowerAddr.contains("sm j mall") || lowerAddr.contains("j centre mall") || lowerAddr.contains("jmall")) {
                return "SM J Mall";
            }
            if (lowerAddr.contains("it park") || lowerAddr.contains("i.t. park")) {
                return "Cebu IT Park";
            }
            if (lowerAddr.contains("colon street")) {
                return "Colon Street";
            }
            if (lowerAddr.contains("magellan's cross")) {
                return "Magellan's Cross";
            }
            if (lowerAddr.contains("basilica del santo niño") || lowerAddr.contains("santo nino basilica")) {
                return "Basilica del Santo Niño";
            }
            if (lowerAddr.contains("plaza independencia")) {
                return "Plaza Independencia";
            }
            if (lowerAddr.contains("cebu provincial capitol")) {
                return "Cebu Provincial Capitol";
            }
            if (lowerAddr.contains("fort san pedro")) {
                return "Fort San Pedro";
            }
            if (lowerAddr.contains("taoist temple")) {
                return "Cebu Taoist Temple";
            }
            if (lowerAddr.contains("temple of leah")) {
                return "Temple of Leah";
            }
            if (lowerAddr.contains("sirao garden")) {
                return "Sirao Garden";
            }
            if (lowerAddr.contains("carbon market")) {
                return "Carbon Market";
            }
            if (lowerAddr.contains("taboan market")) {
                return "Taboan Public Market";
            }
            if (lowerAddr.contains("robinsons galleria")) {
                return "Robinsons Galleria Cebu";
            }
        }

        if (feature == null || feature.matches("\\d+") || feature.contains(",")) {
            if (fullAddress != null && fullAddress.contains(",")) {
                return fullAddress.split(",")[0].trim();
            }
            return fullAddress;
        }
        return feature;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof Address) {
                    return getCleanName((Address) resultValue);
                }
                return super.convertResultToString(resultValue);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = addresses;
                results.count = (addresses != null) ? addresses.size() : 0;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}
