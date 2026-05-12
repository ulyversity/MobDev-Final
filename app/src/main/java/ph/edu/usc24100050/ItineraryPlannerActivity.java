package ph.edu.usc24100050;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ph.edu.usc24100050.ItirenaryPlannerCore.Groq;
import ph.edu.usc24100050.ItirenaryPlannerCore.ItineraryPlanner;
import ph.edu.usc24100050.ItirenaryPlannerCore.LLMAPI;
import ph.edu.usc24100050.Model.User;

public class ItineraryPlannerActivity extends AppCompatActivity {

    EditText txtPrompt;
    TextView lblName;
    Button btnCreatePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_planner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });




        txtPrompt = findViewById(R.id.txtPrompt);
        btnCreatePlan = findViewById(R.id.btnCreatePlan);
        lblName = findViewById(R.id.lblName);

        User user = getIntent().getParcelableExtra("user");
        lblName.setText(user.getFirstName() + " " + user.getLastName());

        btnCreatePlan.setOnClickListener(v -> {
            String prompt = txtPrompt.getText().toString();

            Intent intent = new Intent(ItineraryPlannerActivity.this, ChoosePlaceActivity.class);
            intent.putExtra("prompt", prompt);
            startActivity(intent);
        });
    }
}