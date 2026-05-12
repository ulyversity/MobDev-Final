package ph.edu.usc24100050;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ph.edu.usc24100050.Adapter.ItineraryAdapter;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private ItineraryAdapter itineraryAdapter;   // NEW

    private RecyclerView recyclerView;
    private RecyclerView rvItinerary;            // NEW
    private TextView tvItineraryEmpty;           // NEW

    private EditText inputField;
    private Button sendButton;
    private ProgressBar progressBar;

    private final String[] suggestions = {
            "Best lechon spots", "Kawasan Falls tips",
            "3-day itinerary", "Beaches near Cebu City"
    };

    // NEW — these chips trigger itinerary generation instead of regular chat
    private final String[] itineraryTriggers = {
            "3-day itinerary", "1-day itinerary", "Weekend in Cebu"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        recyclerView     = findViewById(R.id.recyclerView);
        inputField       = findViewById(R.id.inputField);
        sendButton       = findViewById(R.id.sendButton);
        progressBar      = findViewById(R.id.progressBar);
        rvItinerary      = findViewById(R.id.rvItinerary);       // NEW
        tvItineraryEmpty = findViewById(R.id.tvItineraryEmpty);  // NEW

        // Chat RecyclerView (existing)
        messageAdapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Itinerary RecyclerView (NEW)
        itineraryAdapter = new ItineraryAdapter();
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvItinerary.setAdapter(itineraryAdapter);

        setupChips();

        // Observe chat messages (unchanged)
        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        // Observe loading state (unchanged)
        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!loading);
        });

        // NEW — observe itinerary items from Room
        viewModel.itineraryItems.observe(this, items -> {
            if (items == null || items.isEmpty()) {
                rvItinerary.setVisibility(View.GONE);
                tvItineraryEmpty.setVisibility(View.VISIBLE);
            } else {
                tvItineraryEmpty.setVisibility(View.GONE);
                rvItinerary.setVisibility(View.VISIBLE);
                itineraryAdapter.setItems(items);
            }
        });

        // Send button (unchanged)
        sendButton.setOnClickListener(v -> sendMessage());

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        // NEW — if the message sounds like an itinerary request, generate structured plan
        if (isItineraryRequest(text)) {
            viewModel.generateItinerary(text);
        } else {
            viewModel.sendMessage(text);
        }
        inputField.setText("");
    }

    // NEW — detect itinerary intent keywords
    private boolean isItineraryRequest(String text) {
        String lower = text.toLowerCase();
        return lower.contains("itinerary") || lower.contains("plan my")
                || lower.contains("day trip") || lower.contains("schedule");
    }

    private void setupChips() {
        LinearLayout chipContainer = findViewById(R.id.chipContainer);
        for (String suggestion : suggestions) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(this);
            chip.setText(suggestion);
            chip.setOnClickListener(v -> {
                // NEW — route itinerary chips to generateItinerary()
                if (isItineraryRequest(suggestion)) {
                    viewModel.generateItinerary(suggestion);
                } else {
                    viewModel.sendMessage(suggestion);
                }
            });
            chipContainer.addView(chip);
        }
    }
}