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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import ph.edu.usc24100050.ItirenaryPlannerCore.Groq;
import ph.edu.usc24100050.ItirenaryPlannerCore.ItineraryPlanner;
import ph.edu.usc24100050.ItirenaryPlannerCore.LLMAPI;

public class BetterItinerary extends AppCompatActivity {

    TextView txtMarkdowntext;
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

        txtMarkdowntext = findViewById(R.id.txtMarkdownText);
        btnGoToMap = findViewById(R.id.btnGoToMap);

        LLMAPI groq = new Groq();
        ItineraryPlanner planner = new ItineraryPlanner(groq);

        String title = getIntent().getStringExtra("title");
        String location = getIntent().getStringExtra("location");
        String activity = getIntent().getStringExtra("activity");

        String prompt = String.format("Can you tell me more about %s like how do i reach %s from my location which is University of San Carlos Talamban Campus, if jeep ride is possible and more? tips and tricks and more", title, location);

        btnGoToMap.setOnClickListener(v -> {
            Intent intent = new Intent(BetterItinerary.this, MapActivity.class);
            intent.putExtra("location", location);
            startActivity(intent);
        });

        planner.createBetterItinerary(prompt, activity)
                .thenAccept(betterItinerary -> {
                    try {
                        Log.d("HELLOWORLD", betterItinerary);
                        Log.d("HELLOWORLD", new ObjectMapper().writeValueAsString(betterItinerary));

                        runOnUiThread(() -> {
                            Markwon markwon =  Markwon.builder(BetterItinerary.this)
                                    .usePlugin(TablePlugin.create(BetterItinerary.this))
                                    .build();
                            markwon.setMarkdown(txtMarkdowntext, betterItinerary);
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}