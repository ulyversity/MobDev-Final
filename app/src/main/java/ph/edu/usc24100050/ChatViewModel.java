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
    // Signal for MapActivity: carries "from|to" string
    public final MutableLiveData<String> navigateToMap = new MutableLiveData<>();

    private boolean introShown = false;

    // ── Triggers: user explicitly wants an itinerary GENERATED ──────────────
    private static final String[] ITINERARY_CREATE_TRIGGERS = {
            "make an itinerary", "create an itinerary", "generate an itinerary",
            "plan my trip", "plan a trip", "build an itinerary",
            "make me a", "create me a", "give me a", "set up a",
            "1-day itinerary", "3-day itinerary", "7-day itinerary", "one-week itinerary",
            "week-long itinerary", "one day itinerary", "three day itinerary",
            "make itinerary", "create itinerary", "generate itinerary"
    };

    // ── Triggers: user just asks about options/things to do (NO auto-fill) ──
    private static final String[] ITINERARY_SUGGEST_TRIGGERS = {
            "what can i do", "things to do", "places to visit", "where should i go",
            "what to do in cebu", "recommend places", "suggest places",
            "what are the best", "top spots", "must visit", "must see",
            "activities in cebu", "what to see", "sightseeing"
    };

    // ── Route triggers: user wants Umiko to SET the route for them ───────────
    private static final String[] ROUTE_SET_TRIGGERS = {
            "set route", "add route", "plan my route", "navigate me",
            "set navigation", "add to map", "open map to", "take me to"
    };

    // ── Route triggers: user just wants advice/alternatives ──────────────────
    private static final String[] ROUTE_INFO_TRIGGERS = {
            "how do i get to", "how to get to", "route to", "directions to",
            "best way to", "how to go to", "transport to", "how far is",
            "how long to", "get from", "travel from", "go from"
    };

    public ChatViewModel(Application application) {
        super(application);
        aiService = new CebuAIService(application);
        itineraryDao = AppDatabase.getInstance(application).itineraryDao();
        itineraryItems = itineraryDao.getAll();
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading()      { return isLoading; }

    /** Call this once when ChatActivity starts to show Umiko's greeting. */
    public void showIntroIfNeeded() {
        if (!introShown) {
            introShown = true;
            postMessage("assistant", CebuAIService.UMIKO_INTRO);
        }
    }

    // ─── Single unified sendMessage ────────────────────────────────────────────
    public void sendMessage(String userMessage) {
        postMessage("user", userMessage);
        isLoading.postValue(true);

        String lower = userMessage.toLowerCase();

        // 1. User explicitly asks Umiko to CREATE + FILL an itinerary
        if (matchesAny(lower, ITINERARY_CREATE_TRIGGERS)) {
            handleItineraryCreate(userMessage);
        }
        // 2. User just asks what they can do — suggest options, don't auto-fill planner
        else if (matchesAny(lower, ITINERARY_SUGGEST_TRIGGERS)) {
            handleItinerarySuggest(userMessage);
        }
        // 3. User wants Umiko to SET the route in the map automatically
        else if (matchesAny(lower, ROUTE_SET_TRIGGERS)) {
            handleRouteSet(userMessage);
        }
        // 4. User asks for route alternatives / info
        else if (matchesAny(lower, ROUTE_INFO_TRIGGERS)) {
            handleRouteInfo(userMessage);
        }
        // 5. Normal chat
        else {
            handleNormalChat(userMessage);
        }
    }

    // ─── 1. Auto-generate itinerary and push to planner ───────────────────────
    private void handleItineraryCreate(String userMessage) {
        // Determine duration label for the message
        String lower = userMessage.toLowerCase();
        String durationLabel = "3-day"; // default
        if (lower.contains("1-day") || lower.contains("one day") || lower.contains("one-day")) durationLabel = "1-day";
        else if (lower.contains("week") || lower.contains("7-day") || lower.contains("7 day")) durationLabel = "7-day";
        else if (lower.contains("3-day") || lower.contains("three day") || lower.contains("three-day")) durationLabel = "3-day";

        postMessage("assistant", "🗺️ Generating your " + durationLabel + " Cebu itinerary... one moment, bestie! ✨");

        aiService.generateItineraryJson(userMessage, new CebuAIService.AICallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                List<ItineraryItem> items = parseItineraryJson(jsonResponse);
                if (items != null && !items.isEmpty()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            itineraryDao.clearAll();
                            itineraryDao.insertAll(items);
                        } catch (Exception e) {
                            Log.e("ChatViewModel", "DB write failed: " + e.getMessage());
                        }
                    });
                    int maxDay = getMaxDay(items);
                    postMessage("assistant",
                            "✅ Done! Your " + maxDay + "-day Cebu itinerary is all set! " +
                                    "I've added everything to your Itinerary Planner — opening it now! 🌺");
                    navigateToPlanner.postValue(items);
                } else {
                    postMessage("assistant",
                            "⚠️ Ay, I had a little trouble formatting that. " +
                                    "Try saying: 'Make me a 3-day Cebu itinerary for beaches and food' 😊");
                }
                isLoading.postValue(false);
            }
            @Override
            public void onError(String errorMessage) {
                postMessage("assistant", getFriendlyError(errorMessage));
                isLoading.postValue(false);
            }
        });
    }

    // ─── 2. Suggest options without auto-filling planner ─────────────────────
    private void handleItinerarySuggest(String userMessage) {
        // Append an instruction so Umiko returns options, not a fixed plan
        String enrichedPrompt = userMessage +
                "\n\n[Give the user a list of possible activities and places they can do in Cebu, " +
                "with 2-3 alternatives per category. Do NOT create a fixed itinerary — " +
                "just present options so they can choose. Keep it fun and conversational!]";

        List<Message> history = messages.getValue() != null
                ? new ArrayList<>(messages.getValue()) : new ArrayList<>();

        aiService.chat(enrichedPrompt, history, new CebuAIService.AICallback() {
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

    // ─── 3. Route set: parse and open MapActivity with pre-filled points ──────
    private void handleRouteSet(String userMessage) {
        postMessage("assistant", "🗺️ Finding the best route for you and adding it to the map!");

        // Extract rough from/to from the message using the LLM, then signal navigation
        String enrichedPrompt = userMessage +
                "\n\n[Respond ONLY with JSON: {\"from\":\"place name\",\"to\":\"destination name\"}. " +
                "Extract the origin and destination from the user's message. " +
                "If origin isn't specified, use 'Current Location'.]";

        List<Message> empty = new ArrayList<>();
        aiService.chat(enrichedPrompt, empty, new CebuAIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    String clean = response.replaceAll("(?s)```json|```", "").trim();
                    JSONObject obj = new JSONObject(clean);
                    String from = obj.optString("from", "Current Location");
                    String to   = obj.optString("to", "");
                    if (!to.isEmpty()) {
                        postMessage("assistant",
                                "📍 Setting your route from **" + from + "** to **" + to + "** " +
                                        "and adding it to your map now! Amping! 🌺");
                        navigateToMap.postValue(from + "|" + to);
                    } else {
                        postMessage("assistant",
                                "Hmm, I couldn't figure out the destination. " +
                                        "Can you say it like 'Take me to Kawasan Falls'? 😊");
                    }
                } catch (Exception e) {
                    postMessage("assistant",
                            "I had trouble reading that route. Try: 'Take me to Magellan's Cross' 😊");
                }
                isLoading.postValue(false);
            }
            @Override
            public void onError(String errorMessage) {
                postMessage("assistant", getFriendlyError(errorMessage));
                isLoading.postValue(false);
            }
        });
    }

    // ─── 4. Route info: give alternatives, let user decide ───────────────────
    private void handleRouteInfo(String userMessage) {
        String enrichedPrompt = userMessage +
                "\n\n[Give the user 2-3 route alternatives to get there. " +
                "For each option include: transport type, estimated time, and fare in PHP. " +
                "Let the user decide which one they prefer. " +
                "At the end, offer to set the route in the map automatically if they want.]";

        List<Message> history = messages.getValue() != null
                ? new ArrayList<>(messages.getValue()) : new ArrayList<>();

        aiService.chat(enrichedPrompt, history, new CebuAIService.AICallback() {
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

    // ─── 5. Normal chat ───────────────────────────────────────────────────────
    private void handleNormalChat(String userMessage) {
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
    private boolean matchesAny(String lower, String[] triggers) {
        for (String trigger : triggers) {
            if (lower.contains(trigger)) return true;
        }
        return false;
    }

    private List<ItineraryItem> parseItineraryJson(String raw) {
        try {
            String clean = raw.replaceAll("(?s)```json|```", "").trim();
            JSONObject root = new JSONObject(clean);
            JSONObject itinerary = root.getJSONObject("itinerary");
            JSONArray daysArr = itinerary.getJSONArray("days");

            List<ItineraryItem> items = new ArrayList<>();
            for (int i = 0; i < daysArr.length(); i++) {
                JSONObject dayObj = daysArr.getJSONObject(i);
                int dayNumber = dayObj.optInt("day_number", i + 1);
                JSONArray activities = dayObj.getJSONArray("activities");

                for (int j = 0; j < activities.length(); j++) {
                    JSONObject act = activities.getJSONObject(j);
                    ItineraryItem item = new ItineraryItem();
                    item.setDay(dayNumber);

                    // Format start_time if it's HHMM
                    String startTime = act.optString("start_time", "");
                    if (startTime.length() == 4 && startTime.matches("\\d+")) {
                        int h = Integer.parseInt(startTime.substring(0, 2));
                        int m = Integer.parseInt(startTime.substring(2));
                        String ampm = (h >= 12) ? "PM" : "AM";
                        int h12 = (h % 12 == 0) ? 12 : h % 12;
                        startTime = String.format("%d:%02d %s", h12, m, ampm);
                    }
                    item.setTime(startTime);

                    item.setPlaceName(act.optString("venue", ""));
                    item.setPlaceType(act.optString("place_type", ""));
                    item.setDurationMinutes(act.optInt("duration_minutes", 60));

                    String notes = act.optString("notes", "");
                    String travel = act.optString("travel_from_previous", "");
                    String activityDesc = act.optString("activity", "");

                    // Combined notes + travel info for a richer description
                    String fullNotes = activityDesc;
                    if (!notes.isEmpty()) fullNotes += "\nNote: " + notes;
                    if (!travel.isEmpty()) fullNotes += "\n🚌 " + travel;

                    item.setNotes(fullNotes);
                    item.setLatitude(act.optDouble("latitude", 10.3157));
                    item.setLongitude(act.optDouble("longitude", 123.8854));
                    item.setTask(item.getPlaceName() + ": " + activityDesc);

                    items.add(item);
                }
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
        if (error == null) return "Ay, something went wrong! Let me try again 🐳";
        String lower = error.toLowerCase();
        if (lower.contains("429") || lower.contains("rate limit") || lower.contains("quota"))
            return "Ay, I'm a little overwhelmed right now — too many chats! Give me a moment 🐳";
        if (lower.contains("403") || lower.contains("unauthorized") || lower.contains("api key"))
            return "I'm having a tiny connection hiccup. Please try again shortly! 🐳";
        if (lower.contains("no internet") || lower.contains("unable to resolve")
                || lower.contains("network") || lower.contains("timeout"))
            return "Looks like there's no internet connection. Please check your signal! 📶";
        if (lower.contains("404") || lower.contains("not found"))
            return "Hmm, I couldn't find what you're looking for. Try asking differently! 🤔";
        if (lower.contains("500") || lower.contains("server"))
            return "The server is taking a quick break. Try again in a bit! 🙏";
        return "Something went wrong on my end. Please try again! 🐳";
    }

    private void postMessage(String role, String content) {
        List<Message> updated = new ArrayList<>(
                messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        updated.add(new Message(role, content));
        messages.postValue(updated);
    }
}
