package ph.edu.usc24100050;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import ph.edu.usc24100050.Adapter.ItineraryAdapter;
import ph.edu.usc24100050.DB.AppDatabase;
import ph.edu.usc24100050.DB.ItineraryDao;
import ph.edu.usc24100050.Model.ItineraryItem;

public class PlannerActivity extends AppCompatActivity {

    private RecyclerView rvGetaways;
    private RecyclerView rvUmikaItinerary;
    private TextView tvUmikaBanner;
    private TextView tvUmikaEmpty;
    private GetawayAdapter adapter;
    private ItineraryAdapter itineraryAdapter;
    private PlannerViewModel viewModel;
    private ItineraryDao itineraryDao;

    // Which day tab is selected (0 = All)
    private int selectedDay = 0;
    private List<ItineraryItem> allItems = new ArrayList<>();

    // Track if tabs are already built to avoid re-adding listeners on DB updates
    private boolean tabsBuilt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_planner);

        // ── findViewById for ALL views before any use ─────────────────────────
        rvGetaways       = findViewById(R.id.rvGetaways);
        rvUmikaItinerary = findViewById(R.id.rvUmikaItinerary);  // was missing!
        tvUmikaBanner    = findViewById(R.id.tvUmikaBanner);      // was missing!
        tvUmikaEmpty     = findViewById(R.id.tvUmikaEmpty);       // was missing!

        // Guard: if the layout doesn't have the Umiko section yet, skip safely
        if (rvGetaways != null) {
            rvGetaways.setLayoutManager(new LinearLayoutManager(this));
        }
        if (rvUmikaItinerary != null) {
            rvUmikaItinerary.setLayoutManager(new LinearLayoutManager(this));
        }

        // ── Getaways (user-created trips) ─────────────────────────────────────
        viewModel = new ViewModelProvider(this).get(PlannerViewModel.class);
        viewModel.getGetaways().observe(this, getaways -> {
            if (rvGetaways == null) return;
            adapter = new GetawayAdapter(this, getaways);
            rvGetaways.setAdapter(adapter);
        });

        // ── Umika AI Itinerary from Room DB ───────────────────────────────────
        if (rvUmikaItinerary != null) {
            itineraryAdapter = new ItineraryAdapter();
            rvUmikaItinerary.setAdapter(itineraryAdapter);
            itineraryDao = AppDatabase.getInstance(this).itineraryDao();
            itineraryDao.getAll().observe(this, items -> {
                allItems = items != null ? items : new ArrayList<>();
                if (allItems.isEmpty()) {
                    rvUmikaItinerary.setVisibility(View.GONE);
                    if (tvUmikaEmpty != null) tvUmikaEmpty.setVisibility(View.VISIBLE);
                } else {
                    if (tvUmikaEmpty != null) tvUmikaEmpty.setVisibility(View.GONE);
                    rvUmikaItinerary.setVisibility(View.VISIBLE);
                    // Only rebuild tabs when the day count actually changes
                    buildDayTabs(allItems);
                    filterAndShowDay(selectedDay, allItems);
                }
            });
        }

        // ── Banner: shown when navigated from Umika chat ──────────────────────
        boolean fromChat = getIntent().getBooleanExtra("from_chat", false);
        int dayCount     = getIntent().getIntExtra("day_count", 0);
        if (tvUmikaBanner != null) {
            if (fromChat && dayCount > 0) {
                tvUmikaBanner.setVisibility(View.VISIBLE);
                tvUmikaBanner.setText("🌺 Umika made you a " + dayCount + "-day itinerary! Enjoy Cebu!");
            } else {
                tvUmikaBanner.setVisibility(View.GONE);
            }
        }

        // ── FAB: add a custom trip ────────────────────────────────────────────
        FloatingActionButton fabAddTrip = findViewById(R.id.fabAddTrip);
        if (fabAddTrip != null) {
            fabAddTrip.setOnClickListener(v -> {
                Getaway newTrip = new Getaway(
                        "My Custom Cebu Trip",
                        "Tap to edit date",
                        "₱0",
                        "Custom • My Plan"
                );
                viewModel.addGetaway(newTrip);
                Toast.makeText(this, "New trip added! Edit it to fit your plans 🌺",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Dynamically build day tabs based on how many days are in the itinerary.
     * Guards against re-adding the listener every time the DB emits an update.
     */
    private void buildDayTabs(List<ItineraryItem> items) {
        TabLayout tabLayout = findViewById(R.id.tabDays);
        if (tabLayout == null) return;

        int maxDay = 1;
        for (ItineraryItem item : items) {
            if (item.getDay() > maxDay) maxDay = item.getDay();
        }

        // Rebuild tabs only when needed (avoids duplicate listener bug)
        int currentTabCount = tabLayout.getTabCount();
        int expectedTabCount = maxDay + 1; // "All" + Day 1..N
        if (tabsBuilt && currentTabCount == expectedTabCount) return;

        tabsBuilt = false;
        tabLayout.removeAllTabs();
        tabLayout.clearOnTabSelectedListeners();

        tabLayout.addTab(tabLayout.newTab().setText("All"));
        for (int d = 1; d <= maxDay; d++) {
            tabLayout.addTab(tabLayout.newTab().setText("Day " + d));
        }

        // Restore the previously selected tab after rebuild
        if (selectedDay < tabLayout.getTabCount()) {
            TabLayout.Tab tab = tabLayout.getTabAt(selectedDay);
            if (tab != null) tab.select();
        }

        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = tab.getPosition(); // 0 = All, 1 = Day 1, etc.
                filterAndShowDay(selectedDay, allItems);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        tabsBuilt = true;
    }

    /** Filter itinerary by selected day tab and refresh the adapter. */
    private void filterAndShowDay(int tabPos, List<ItineraryItem> items) {
        if (itineraryAdapter == null) return;
        if (tabPos == 0) {
            itineraryAdapter.setItems(items);
        } else {
            List<ItineraryItem> filtered = new ArrayList<>();
            for (ItineraryItem item : items) {
                if (item.getDay() == tabPos) filtered.add(item);
            }
            itineraryAdapter.setItems(filtered);
        }
    }
}