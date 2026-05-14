package ph.edu.usc24100050.DB;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ph.edu.usc24100050.Model.ItineraryItem;

public class ItineraryParser {
    public static List<ItineraryItem> parse(String jsonText) {
        List<ItineraryItem> items = new ArrayList<>();
        try {
            String clean = jsonText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            JSONArray array = new JSONArray(clean);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ItineraryItem item = new ItineraryItem();
                item.setTime(obj.optString("time", ""));
                item.setTask(obj.optString("task", ""));
                if (!item.getTask().isEmpty()) items.add(item);
            }
        } catch (JSONException e) {
            Log.e("ItineraryParser", "Parse failed: " + e.getMessage());
        }
        return items;
    }
}