package ph.edu.usc24100050;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ph.edu.usc24100050.DB.AppDatabase;
import ph.edu.usc24100050.DB.ItineraryDao;
import ph.edu.usc24100050.Model.ItineraryItem;

public class ChatViewModel extends AndroidViewModel {

    private final CebuAIService aiService;
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ItineraryDao itineraryDao;
    public final LiveData<List<ItineraryItem>> itineraryItems;
    public final MutableLiveData<List<ItineraryItem>> navigateToPlanner = new MutableLiveData<>();

    private static final String[] ITINERARY_TRIGGERS = {
            "itinerary", "plan my trip", "travel plan", "day trip",
            "generate itinerary", "make an itinerary", "plan a trip",
            "places to visit", "schedule my", "trip to cebu"
    };

    public ChatViewModel(Application application) {
        super(application);
        aiService = new CebuAIService(application);
        itineraryDao = AppDatabase.getInstance(application).itineraryDao();
        itineraryItems = itineraryDao.getAll();
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading()      { return isLoading; }

    // ─── Single unified sendMessage ────────────────────────────────────────────
    public void sendMessage(String userMessage) {
        postMessage("user", userMessage);
        isLoading.postValue(true);

        if (isItineraryRequest(userMessage)) {
            postMessage("assistant", "🗺️ Generating your Cebu itinerary... one moment!");

            aiService.generateItineraryJson(userMessage, new CebuAIService.AICallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    List<ItineraryItem> items = parseItineraryJson(jsonResponse);
                    if (items != null && !items.isEmpty()) {
                        // ✅ Write to DB on background thread
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                itineraryDao.clearAll();
                                itineraryDao.insertAll(items);
                            } catch (Exception e) {
                                Log.e("ChatViewModel", "DB transaction failed: " + e.getMessage());
                            }
                        });
                        postMessage("assistant",
                                "✅ Done! Your " + getMaxDay(items) + "-day itinerary is ready. " +
                                        "Opening your planner now...");
                        navigateToPlanner.postValue(items);
                    } else {
                        postMessage("assistant",
                                "⚠️ I had trouble formatting the itinerary. " +
                                        "Try: 'Plan a 3-day Cebu trip for beaches and food'");
                    }
                    isLoading.postValue(false);
                }

                @Override
                public void onError(String errorMessage) {
                    postMessage("assistant", getFriendlyError(errorMessage));
                    isLoading.postValue(false);
                }
            });

        } else {
            // Normal chat flow
            List<Message> history = messages.getValue() != null
                    ? new ArrayList<>(messages.getValue()) : new ArrayList<>();

            aiService.chat(userMessage, history, new CebuAIService.AICallback() {
                @Override
                public void onSuccess(String response) {
                    postMessage("assistant", response);
                    isLoading.postValue(false);
                }

                @Override
                public void onError(String errorMessage) {
                    postMessage("assistant", getFriendlyError(errorMessage));
                    isLoading.postValue(false);
                }
            });
        }
    }

    public void clearItinerary() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                itineraryDao.clearAll();
            } catch (Exception e) {
                Log.e("ChatViewModel", "Failed to clear itinerary: " + e.getMessage());
            }
        });
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────
    private boolean isItineraryRequest(String message) {
        String lower = message.toLowerCase();
        for (String trigger : ITINERARY_TRIGGERS) {
            if (lower.contains(trigger)) return true;
        }
        return false;
    }

    private List<ItineraryItem> parseItineraryJson(String raw) {
        try {
            String clean = raw.replaceAll("(?s)```json|```", "").trim();
            JSONArray arr = new JSONArray(clean);
            List<ItineraryItem> items = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ItineraryItem item = new ItineraryItem();
                item.setDay(obj.optInt("day", 1));
                item.setTime(obj.optString("time", ""));
                item.setPlaceName(obj.optString("place_name", ""));
                item.setPlaceType(obj.optString("place_type", ""));
                item.setDurationMinutes(obj.optInt("duration_minutes", 60));
                item.setNotes(obj.optString("notes", ""));
                item.setLatitude(obj.optDouble("latitude", 10.3157));
                item.setLongitude(obj.optDouble("longitude", 123.8854));
                
                // Set task for compatibility with ItineraryAdapter
                item.setTask(item.getPlaceName() + (item.getNotes().isEmpty() ? "" : ": " + item.getNotes()));
                
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            Log.e("ChatViewModel", "JSON parse failed: " + e.getMessage());
            return null;
        }
    }

    private int getMaxDay(List<ItineraryItem> items) {
        int max = 1;
        for (ItineraryItem item : items) {
            if (item.getDay() > max) max = item.getDay();
        }
        return max;
    }

    private String getFriendlyError(String error) {
        if (error == null) return "Something went wrong. Please try again! 🐳";
        String lower = error.toLowerCase();
        if (lower.contains("429") || lower.contains("rate limit") || lower.contains("quota"))
            return "I'm a little overwhelmed right now — too many requests! Give me a moment. 🐳";
        if (lower.contains("403") || lower.contains("unauthorized") || lower.contains("api key"))
            return "I'm having trouble with my connection. Please try again shortly! 🐳";
        if (lower.contains("no internet") || lower.contains("unable to resolve")
                || lower.contains("network") || lower.contains("timeout"))
            return "Looks like there's no internet connection. Please check your network! 📶";
        if (lower.contains("404") || lower.contains("not found"))
            return "Hmm, I couldn't find what you're looking for. Try asking differently! 🤔";
        if (lower.contains("500") || lower.contains("server"))
            return "The server is having a moment. Please try again in a bit! 🙏";
        return "Something went wrong on my end. Please try again! 🐳";
    }

    // ✅ postMessage does NOT touch isLoading — caller controls it
    private void postMessage(String role, String content) {
        List<Message> updated = new ArrayList<>(
                messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        updated.add(new Message(role, content));
        messages.postValue(updated);
    }
}
