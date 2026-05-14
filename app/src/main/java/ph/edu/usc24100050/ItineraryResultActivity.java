package ph.edu.usc24100050;

import android.os.Bundle;
import android.util.Log;
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

import ph.edu.usc24100050.Adapter.DayAdapter;
import ph.edu.usc24100050.ItirenaryPlannerCore.Groq;
import ph.edu.usc24100050.ItirenaryPlannerCore.ItineraryPlanner;
import ph.edu.usc24100050.ItirenaryPlannerCore.LLMAPI;

public class ItineraryResultActivity extends AppCompatActivity {

    TextView txtStartStopDate, txtTitle;
    RecyclerView rvDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtStartStopDate =  findViewById(R.id.txtStartStopDate);
        txtTitle = findViewById(R.id.txtTitle);
        rvDays = findViewById(R.id.rvDays);
        rvDays.setLayoutManager(new LinearLayoutManager(ItineraryResultActivity.this));

        String prompt = getIntent().getStringExtra("prompt");
        LLMAPI groq = new Groq();
        ItineraryPlanner planner = new ItineraryPlanner(groq);

        planner.createPlan(prompt)
                .thenAccept(itinerary -> {
                    try {
                        Log.d("HELLOWORLD", new ObjectMapper().writeValueAsString(itinerary));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("HELLOWORLD", "CHAT IS IT WORKING CHAT?");

                    String startStopDate = itinerary.getStartDate() + " - " + itinerary.getStopDate();
                    DayAdapter adapter = new DayAdapter(ItineraryResultActivity.this, itinerary);

                    runOnUiThread(() -> {
                        rvDays.setAdapter(adapter);
                        txtStartStopDate.setText(startStopDate);
                    });
                });
    }


}