package ph.edu.usc24100050;

import android.animation.Animator;
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
    private Animator currentActionAnim;
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private ItineraryAdapter itineraryAdapter;
    private RecyclerView recyclerView;
    private RecyclerView rvItinerary;
    private View itineraryContainer;
    private View btnShowPlan;
    private TextView tvItineraryEmpty;
    private EditText inputField;
    private Button sendButton;
    private ProgressBar progressBar;
    private long lastMascotClickTime = 0;

    private final String[] suggestions = {
            "What can I do in Cebu?", "Best lechon spots",
            "Make me a 3-day itinerary", "How do I get to Kawasan Falls?"
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
        rvItinerary      = findViewById(R.id.rvItinerary);
        itineraryContainer = findViewById(R.id.itineraryContainer);
        btnShowPlan      = findViewById(R.id.btnShowPlan);
        tvItineraryEmpty = findViewById(R.id.tvItineraryEmpty);
        mascotView       = findViewById(R.id.mascotView);

        mascotView.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastMascotClickTime < 400) {
                playHappyAnim(); // Big wiggle + Scale (Double click)
            } else {
                playSmallWiggle(); // Small tilt (Single click)
            }
            lastMascotClickTime = now;
        });

        findViewById(R.id.btnMinimizeItinerary).setOnClickListener(v -> {
            itineraryContainer.setVisibility(View.GONE);
            btnShowPlan.setVisibility(View.VISIBLE);
            updateWelcomeVisibility();
        });

        findViewById(R.id.btnClearItinerary).setOnClickListener(v -> {
            viewModel.clearItinerary();
            itineraryContainer.setVisibility(View.GONE);
            btnShowPlan.setVisibility(View.GONE);
            updateWelcomeVisibility();
        });

        btnShowPlan.setOnClickListener(v -> {
            itineraryContainer.setVisibility(View.VISIBLE);
            btnShowPlan.setVisibility(View.GONE);
            updateWelcomeVisibility();
        });
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
            updateWelcomeVisibility();
            if (!messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        // Show Umika's introduction greeting on first open
        viewModel.showIntroIfNeeded();

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

        // Navigate to MapActivity when Umika sets a route automatically
        viewModel.navigateToMap.observe(this, routeStr -> {
            if (routeStr == null || routeStr.isEmpty()) return;
            String[] parts = routeStr.split("\\|", 2);
            Intent intent = new Intent(ChatActivity.this, MapActivity.class);
            intent.putExtra("from_chat", true);
            intent.putExtra("route_from", parts.length > 0 ? parts[0] : "");
            intent.putExtra("route_to",   parts.length > 1 ? parts[1] : "");
            startActivity(intent);
            viewModel.navigateToMap.setValue(null);
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
            boolean hasItems = items != null && !items.isEmpty();
            if (!hasItems) {
                itineraryContainer.setVisibility(View.GONE);
                btnShowPlan.setVisibility(View.GONE);
            } else {
                // Only force visible if it was previously completely empty/gone
                if (itineraryContainer.getVisibility() == View.GONE && btnShowPlan.getVisibility() == View.GONE) {
                    itineraryContainer.setVisibility(View.VISIBLE);
                }
                itineraryAdapter.setItems(items);
                // Don't play happy anim on every DB refresh, only when it's first populated
            }
            updateWelcomeVisibility();
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

    private void updateWelcomeVisibility() {
        boolean noMessages = viewModel.getMessages().getValue() == null || viewModel.getMessages().getValue().isEmpty();
        boolean itineraryHiddenOrEmpty = (itineraryContainer.getVisibility() != View.VISIBLE) 
                || (viewModel.itineraryItems.getValue() == null || viewModel.itineraryItems.getValue().isEmpty());

        if (noMessages && itineraryHiddenOrEmpty) {
            tvItineraryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvItineraryEmpty.setVisibility(View.GONE);
        }
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

    private void playSmallWiggle() {
        if (mascotView == null) return;
        if (idleAnim != null) idleAnim.cancel();
        if (currentActionAnim != null) currentActionAnim.cancel();

        mascotView.setImageResource(R.drawable.umika_1);

        ObjectAnimator wiggle = ObjectAnimator.ofFloat(mascotView, "rotation", 0f, -5f, 5f, -5f, 5f, 0f);
        wiggle.setDuration(400);
        wiggle.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (currentActionAnim == animation) {
                    mascotView.setImageResource(R.drawable.umika_urahime);
                    mascotView.post(() -> startIdleAnim());
                    currentActionAnim = null;
                }
            }
        });
        currentActionAnim = wiggle;
        wiggle.start();
    }

    private void playHappyAnim() {
        if (mascotView == null) return;
        if (idleAnim != null) idleAnim.cancel();
        if (currentActionAnim != null) currentActionAnim.cancel();

        mascotView.setImageResource(R.drawable.umika_2);

        ObjectAnimator wiggle = ObjectAnimator.ofFloat(mascotView, "rotation", 0f, -10f, 10f, -10f, 10f, -7f, 7f, 0f);
        wiggle.setDuration(1000);
        wiggle.setRepeatCount(3);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mascotView, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mascotView, "scaleY", 1f, 1.15f, 1f);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.setRepeatCount(3);
        scaleY.setRepeatCount(3);
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());

        AnimatorSet happy = new AnimatorSet();
        happy.playTogether(wiggle, scaleX, scaleY);
        happy.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (currentActionAnim == animation) {
                    mascotView.setImageResource(R.drawable.umika_urahime);
                    mascotView.post(() -> startIdleAnim());
                    currentActionAnim = null;
                }
            }
        });
        currentActionAnim = happy;
        happy.start();
    }
}
