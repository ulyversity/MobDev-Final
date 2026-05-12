package ph.edu.usc24100050;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText apiKeyInput = findViewById(R.id.apiKeyInput);
        Button saveButton = findViewById(R.id.saveButton);
        Button chatButton = findViewById(R.id.chatButton);
        Button plannerButton = findViewById(R.id.plannerButton);

        saveButton.setOnClickListener(v -> {
            String key = apiKeyInput.getText().toString().trim();
            if (!key.isEmpty()) {
                CebuAIService.storeApiKey(this, key);
                Toast.makeText(this, "API Key saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a key", Toast.LENGTH_SHORT).show();
            }
        });

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        });

        plannerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlannerActivity.class);
            startActivity(intent);
        });
    }
}
