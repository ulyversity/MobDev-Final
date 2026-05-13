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
    private RecyclerView rvUmikoItinerary;
    private TextView tvUmikoBanner;
    private TextView tvUmikoEmpty;
    private GetawayAdapter adapter;
    private ItineraryAdapter itineraryAdapter;
    private PlannerViewModel viewModel;
    private ItineraryDao itineraryDao;

    // Which day tab is selected (0 = All)
    private int selectedDay = 0;
    private List<ItineraryItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_planner);

        rvGetaways        = findViewById(R.id.rvGetaways);
       

        rvGetaways.setLayoutManager(new LinearLayoutManager(this));
        rvUmikoItinerary.setLayoutManager(new LinearLayoutManager(this));

        // ── Getaways (user-created trips) ─────────────────────────────────────
        viewModel = new ViewModelProvider(this).get(PlannerViewModel.class);
        viewModel.getGetaways().observe(this, getaways -> {
            adapter = new GetawayAdapter(this, getaways);
            rvGetaways.setAdapter(adapter);
        });

        // ── Umiko AI Itinerary from Room DB ───────────────────────────────────
        itineraryAdapter = new ItineraryAdapter();
        rvUmikoItinerary.setAdapter(itineraryAdapter);
        itineraryDao = AppDatabase.getInstance(this).itineraryDao();
        itineraryDao.getAll().observe(this, items -> {
            allItems = items != null ? items : new ArrayList<>();
            if (allItems.isEmpty()) {
                rvUmikoItinerary.setVisibility(View.GONE);
                if (tvUmikoEmpty != null) tvUmikoEmpty.setVisibility(View.VISIBLE);
            } else {
                if (tvUmikoEmpty != null) tvUmikoEmpty.setVisibility(View.GONE);
                rvUmikoItinerary.setVisibility(View.VISIBLE);
                buildDayTabs(allItems);
                filterAndShowDay(selectedDay, allItems);
            }
        });

        // ── Banner: shown when navigated from chat ────────────────────────────
        boolean fromChat   = getIntent().getBooleanExtra("from_chat", false);
        int dayCount       = getIntent().getIntExtra("day_count", 0);
        if (tvUmikoBanner != null) {
            if (fromChat && dayCount > 0) {
                tvUmikoBanner.setVisibility(View.VISIBLE);
                tvUmikoBanner.setText("🌺 Umiko made you a " + dayCount + "-day itinerary! Enjoy Cebu!");
            } else {
                tvUmikoBanner.setVisibility(View.GONE);
            }
        }

        // ── FAB: add a custom trip ────────────────────────────────────────────
        FloatingActionButton fabAddTrip = findViewById(R.id.fabAddTrip);
        fabAddTrip.setOnClickListener(v -> {
            Getaway newTrip = new Getaway(
                    "My Custom Cebu Trip",
                    "Tap to edit date",
                    "₱0",
                    "Custom • My Plan"
            );
            viewModel.addGetaway(newTrip);
            Toast.makeText(this, "New trip added! Edit it to fit your plans 🌺", Toast.LENGTH_SHORT).show();
        });
    }

    /** Dynamically build day tabs based on how many days are in the itinerary. */
    private void buildDayTabs(List<ItineraryItem> items) {
        TabLayout tabLayout = findViewById(R.id.tabDays);
        if (tabLayout == null) return;

        int maxDay = 1;
        for (ItineraryItem item : items) {
            if (item.getDay() > maxDay) maxDay = item.getDay();
        }

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        for (int d = 1; d <= maxDay; d++) {
            tabLayout.addTab(tabLayout.newTab().setText("Day " + d));
        }

        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = tab.getPosition(); // 0 = All, 1 = Day 1, etc.
                filterAndShowDay(selectedDay, allItems);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /** Filter itinerary by selected day tab and refresh the adapter. */
    private void filterAndShowDay(int tabPos, List<ItineraryItem> items) {
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
