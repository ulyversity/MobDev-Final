package ph.edu.usc24100050;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import ph.edu.usc24100050.Model.User;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        LinearLayout cardAiGuide = findViewById(R.id.cardAiGuide);
        cardAiGuide.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ChatActivity.class);
            User user = getIntent().getParcelableExtra("user");
            intent.putExtra("user", user);
            startActivity(intent);
        });

        LinearLayout test =  findViewById(R.id.cardTravelPlanner);
        test.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, MapActivity.class);
            startActivity(intent);
        });

        LinearLayout cardSites =  findViewById(R.id.cardItineraryPlanner);
        cardSites.setOnClickListener(v -> {

            Intent intent = new Intent(Home.this, ItineraryPlannerActivity.class);
            User user = getIntent().getParcelableExtra("user");
            intent.putExtra("user", user);
            startActivity(intent);
        });
    }
}