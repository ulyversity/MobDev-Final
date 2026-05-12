package ph.edu.usc24100050;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import ph.edu.usc24100050.DB.DatabaseHelper;
import ph.edu.usc24100050.Model.PlaceRating;
import ph.edu.usc24100050.Model.PlaceType;
import ph.edu.usc24100050.Model.User;

public class LoginActivity extends AppCompatActivity {


    private DatabaseHelper dbHelper;
    private Button btnLogin;
    private TextView btnCreateAccount;
    private EditText txtUsername, txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        dbHelper = new DatabaseHelper(LoginActivity.this);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            String username = txtUsername.getText().toString();
            String password = txtPassword.getText().toString();
            if(username.isEmpty() || password.isEmpty())
            {
                Toast.makeText(LoginActivity.this, "Enter all fields", Toast.LENGTH_LONG).show();
            }
            else {
                User user = dbHelper.Login(username, password);
                if (user == null)
                {
                    Toast.makeText(LoginActivity.this, "Incorrect credentials", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent = new Intent(LoginActivity.this, Home.class);
                    intent.putExtra("user", user);
                    startActivity(intent);

                    txtUsername.setText("");
                    txtPassword.setText("");
                }
            }
        });

        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });

    }
}