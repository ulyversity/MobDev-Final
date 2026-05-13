package ph.edu.usc24100050;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ph.edu.usc24100050.Adapter.ItineraryAdapter;
import ph.edu.usc24100050.Model.ItineraryItem;

public class ChatActivity extends AppCompatActivity {

    private ImageView mascotView;
    private ObjectAnimator idleAnim;
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private ItineraryAdapter itineraryAdapter;
    private RecyclerView recyclerView;
    private RecyclerView rvItinerary;
    private TextView tvItineraryEmpty;
    private EditText inputField;
    private Button sendButton;
    private ProgressBar progressBar;

    private final String[] suggestions = {
            "Best lechon spots", "Kawasan Falls tips",
            "3-day itinerary", "Beaches near Cebu City"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        mascotView       = findViewById(R.id.mascotView);
        recyclerView     = findViewById(R.id.recyclerView);
        inputField       = findViewById(R.id.inputField);
        sendButton       = findViewById(R.id.sendButton);
        progressBar      = findViewById(R.id.progressBar);
        rvItinerary      = findViewById(R.id.rvItinerary);
        tvItineraryEmpty = findViewById(R.id.tvItineraryEmpty);

        messageAdapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        itineraryAdapter = new ItineraryAdapter();
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvItinerary.setAdapter(itineraryAdapter);

        // waits for layout to finish before animating
        mascotView.post(this::startIdleAnim);

        setupChips();

        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.navigateToPlanner.observe(this, items -> {
            if (items == null || items.isEmpty()) return;

            Intent intent = new Intent(ChatActivity.this, PlannerActivity.class);
            intent.putExtra("from_chat", true);

            // Calculate max day safely
            int maxDay = 1;
            for (ItineraryItem item : items) {
                if (item.getDay() > maxDay) maxDay = item.getDay();
            }
            intent.putExtra("day_count", maxDay);

            startActivity(intent);
            // Reset so re-observe doesn't re-trigger navigation
            viewModel.navigateToPlanner.setValue(null);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!loading);
            if (loading) {
                playThinkingAnim();
            } else {
                mascotView.post(this::startIdleAnim);
            }
        });

        viewModel.itineraryItems.observe(this, items -> {
            if (items == null || items.isEmpty()) {
                rvItinerary.setVisibility(View.GONE);
                tvItineraryEmpty.setVisibility(View.VISIBLE);
            } else {
                tvItineraryEmpty.setVisibility(View.GONE);
                rvItinerary.setVisibility(View.VISIBLE);
                itineraryAdapter.setItems(items);
                playHappyAnim();
            }
        });

        sendButton.setOnClickListener(v -> sendMessage());

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        // Unified sendMessage handles both chat and itinerary logic
        viewModel.sendMessage(text);
        inputField.setText("");
    }

    private void setupChips() {
        LinearLayout chipContainer = findViewById(R.id.chipContainer);
        if (chipContainer == null) return;

        for (String suggestion : suggestions) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(this);
            chip.setText(suggestion);
            chip.setOnClickListener(v -> viewModel.sendMessage(suggestion));
            chipContainer.addView(chip);
        }
    }

    private void startIdleAnim() {
        if (mascotView == null) return;
        if (idleAnim != null) idleAnim.cancel();
        idleAnim = ObjectAnimator.ofFloat(mascotView, "translationY", 0f, -14f);
        idleAnim.setDuration(1600);
        idleAnim.setRepeatMode(ValueAnimator.REVERSE);
        idleAnim.setRepeatCount(ValueAnimator.INFINITE);
        idleAnim.setInterpolator(new FastOutSlowInInterpolator());
        idleAnim.start();
    }

    private void playThinkingAnim() {
        if (mascotView == null) return;
        if (idleAnim != null) idleAnim.cancel();
        idleAnim = ObjectAnimator.ofFloat(mascotView, "translationY", 0f, -6f);
        idleAnim.setDuration(2200);
        idleAnim.setRepeatMode(ValueAnimator.REVERSE);
        idleAnim.setRepeatCount(ValueAnimator.INFINITE);
        idleAnim.setInterpolator(new FastOutSlowInInterpolator());
        idleAnim.start();
    }

    private void playHappyAnim() {
        if (mascotView == null) return;
        if (idleAnim != null) idleAnim.cancel();

        ObjectAnimator wiggle = ObjectAnimator.ofFloat(mascotView, "rotation", 0f, -10f, 10f, -7f, 7f, 0f);
        wiggle.setDuration(600);
        wiggle.setRepeatCount(2);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mascotView, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mascotView, "scaleY", 1f, 1.15f, 1f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setRepeatCount(2);
        scaleY.setRepeatCount(2);
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());

        AnimatorSet happy = new AnimatorSet();
        happy.playTogether(wiggle, scaleX, scaleY);
        happy.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                mascotView.post(() -> startIdleAnim());
            }
        });
        happy.start();
    }
}
