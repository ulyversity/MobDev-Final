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

import ph.edu.usc24100050.Adapter.CategoryAdapter;
import ph.edu.usc24100050.Adapter.CitiesAdapter;
import ph.edu.usc24100050.ItirenaryPlannerCore.Groq;
import ph.edu.usc24100050.ItirenaryPlannerCore.ItineraryPlanner;
import ph.edu.usc24100050.ItirenaryPlannerCore.LLMAPI;

public class ChoosePlaceActivity extends AppCompatActivity {

    TextView txtActiivtyName;
    RecyclerView rvCities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_place);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtActiivtyName = findViewById(R.id.tvActivityPlaceHolder);
        rvCities = findViewById(R.id.rvCities);

        rvCities.setLayoutManager(new LinearLayoutManager(this));
        String prompt = getIntent().getStringExtra("prompt");

        LLMAPI groq = new Groq();
        ItineraryPlanner planner = new ItineraryPlanner(groq);


        planner.createActivityRoot(prompt)
                .thenAccept(activityRoot -> {
                            try {
                                Log.d("HELLOWORLD", new ObjectMapper().writeValueAsString(activityRoot));
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                            CategoryAdapter adapter = new CategoryAdapter(ChoosePlaceActivity.this, activityRoot.getCategories(), activityRoot.getActivity());
                            runOnUiThread(() -> {
                                rvCities.setAdapter(adapter);
                                txtActiivtyName.setText(activityRoot.getActivity().toUpperCase());
                            });
                        });
    }
}