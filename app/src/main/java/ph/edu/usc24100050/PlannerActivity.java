package ph.edu.usc24100050;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PlannerActivity extends AppCompatActivity {

    private RecyclerView rvGetaways;
    private GetawayAdapter adapter;
    private PlannerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_planner);

        rvGetaways = findViewById(R.id.rvGetaways);
        rvGetaways.setLayoutManager(new LinearLayoutManager(this));

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PlannerViewModel.class);

        // Observe Data
        viewModel.getGetaways().observe(this, getaways -> {
            adapter = new GetawayAdapter(this, getaways);
            rvGetaways.setAdapter(adapter);
        });

        // Add Button Logic
        FloatingActionButton fabAddTrip = findViewById(R.id.fabAddTrip);
        fabAddTrip.setOnClickListener(v -> {
            // For now, let's add a sample "Custom Trip" to show it works
            Getaway newTrip = new Getaway(
                    "Osmeña Peak Hike",
                    "Sat, Dec 5",
                    "₱500",
                    "Hiking • Mountains"
            );
            viewModel.addGetaway(newTrip);
            
            Toast.makeText(this, "Added new planned trip!", Toast.LENGTH_SHORT).show();
        });
    }
}
