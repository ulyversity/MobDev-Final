package ph.edu.usc24100050;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import ph.edu.usc24100050.Adapter.MyItineraryAdapter;
import ph.edu.usc24100050.ItirenaryPlannerCore.Groq;
import ph.edu.usc24100050.ItirenaryPlannerCore.ItineraryPlanner;
import ph.edu.usc24100050.ItirenaryPlannerCore.LLMAPI;
import ph.edu.usc24100050.Model.ItineraryItemModel;

public class BetterItinerary extends AppCompatActivity {

    RecyclerView rvItinerary;
    Button btnGoToMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_better_itinerary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvItinerary = findViewById(R.id.rvItinerary);
        rvItinerary.setLayoutManager(new LinearLayoutManager(BetterItinerary.this));
        btnGoToMap = findViewById(R.id.btnGoToMap);

        LLMAPI groq = new Groq();
        ItineraryPlanner planner = new ItineraryPlanner(groq);

        String title = getIntent().getStringExtra("title");
        String location = getIntent().getStringExtra("location");
        String activity = getIntent().getStringExtra("activity");

        String prompt = String.format("Create a realistic Cebu travel itinerary. Destination Name: %s. Destination Address: %s. Starting Point: University of San Carlos Talamban Campus. Planned Activity: %s. Generate commuting steps and activities in chronological order.", title, location, activity);

        btnGoToMap.setOnClickListener(v -> {
            Intent intent = new Intent(BetterItinerary.this, MapActivity.class);
            intent.putExtra("location", location);
            startActivity(intent);
        });

        planner.createBetterItinerary(prompt, activity)
                .thenAccept(itineraryResponse -> {
                    Log.d("HELLOWORLD", itineraryResponse.getItinerary().size() + "");

                    MyItineraryAdapter adapter = new MyItineraryAdapter(BetterItinerary.this, itineraryResponse.itinerary);

                    runOnUiThread(() -> {
                        rvItinerary.setAdapter(adapter);

                        for (ItineraryItemModel item :
                                itineraryResponse.getItinerary()) {

                            Log.d(
                                    "ITINERARY",
                                    item.getTime()
                                            + " "
                                            + item.getAction()
                                            + " @ "
                                            + item.getLocation()
                            );
                        }

                    });

                });
    }
}