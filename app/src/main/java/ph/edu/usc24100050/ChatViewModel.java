package ph.edu.usc24100050;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;
import ph.edu.usc24100050.DB.AppDatabase;
import ph.edu.usc24100050.DB.ItineraryDao;
import ph.edu.usc24100050.DB.ItineraryParser;
import ph.edu.usc24100050.Model.ItineraryItem;

public class ChatViewModel extends AndroidViewModel {

    private final CebuAIService aiService;
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // --- NEW: itinerary fields ---
    private final ItineraryDao itineraryDao;
    public final LiveData<List<ItineraryItem>> itineraryItems;

    public ChatViewModel(Application application) {
        super(application);
        aiService = new CebuAIService(application);

        // NEW
        itineraryDao = AppDatabase.getInstance(application).itineraryDao();
        itineraryItems = itineraryDao.getAll();
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading()      { return isLoading; }

    // --- EXISTING: unchanged ---
    public void sendMessage(String userInput) {
        List<Message> currentHistory = messages.getValue() != null
                ? new ArrayList<>(messages.getValue())
                : new ArrayList<>();

        currentHistory.add(new Message("user", userInput));
        messages.setValue(new ArrayList<>(currentHistory));
        isLoading.setValue(true);

        final List<Message> historySnapshot =
                new ArrayList<>(currentHistory.subList(0, currentHistory.size() - 1));

        aiService.chat(userInput, historySnapshot, new CebuAIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                List<Message> updated = new ArrayList<>(
                        messages.getValue() != null ? messages.getValue() : new ArrayList<>()
                );
                updated.add(new Message("assistant", response));
                messages.setValue(updated);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                List<Message> updated = new ArrayList<>(
                        messages.getValue() != null ? messages.getValue() : new ArrayList<>()
                );
                updated.add(new Message("assistant",
                        "Sorry, I had trouble connecting. Please try again!"));
                messages.setValue(updated);
                isLoading.setValue(false);
            }
        });
    }

    // --- EXISTING: unchanged ---
    public void generateItinerary(int days, List<String> interests) {
        isLoading.setValue(true);
        aiService.generateItinerary(days, interests, new CebuAIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                List<Message> updated = new ArrayList<>(
                        messages.getValue() != null ? messages.getValue() : new ArrayList<>()
                );
                updated.add(new Message("assistant", response));
                messages.setValue(updated);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
            }
        });
    }

    // --- NEW: fixed version ---
    public void generateItinerary(String userMessage) {
        isLoading.setValue(true);

        String prompt = "You are a Cebu, Philippines travel itinerary planner. " +
                "Suggest activities using real Cebu locations such as: " +
                "Larsian BBQ, House of Lechon, Tops Lookout, Temple of Leah, " +
                "Sirao Flower Garden, SM Seaside, Ayala Center Cebu, Carbon Market, " +
                "Anjo World, Cebu IT Park, Abellana Sports Complex, and similar places. " +
                "Respond ONLY with a valid JSON array, no markdown, no extra text. " +
                "Format exactly: [{\"time\":\"HH:MM\",\"task\":\"description\"}]\n\n" +
                "User request: " + userMessage;

        aiService.chat(prompt, new ArrayList<>(), new CebuAIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                // Save parsed items to Room DB
                List<ItineraryItem> items = ItineraryParser.parse(response);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    itineraryDao.clearAll();
                    itineraryDao.insertAll(items);
                });

                // Also show the raw response in chat as confirmation
                List<Message> updated = new ArrayList<>(
                        messages.getValue() != null ? messages.getValue() : new ArrayList<>()
                );
                updated.add(new Message("assistant",
                        "Itinerary generated! Check the planner tab."));
                messages.setValue(updated);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                List<Message> updated = new ArrayList<>(
                        messages.getValue() != null ? messages.getValue() : new ArrayList<>()
                );
                updated.add(new Message("assistant",
                        "Sorry, couldn't generate the itinerary. Please try again!"));
                messages.setValue(updated);
                isLoading.setValue(false);
            }
        });
    }
}