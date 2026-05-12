package ph.edu.usc24100050;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ph.edu.usc24100050.DB.DatabaseHelper;

public class CreateAccountActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private Button btnCreateCreateAccount;
    private EditText txtCreateFirstName, txtCreateLastName, txtCreateUsername, txtCreatePassword;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(CreateAccountActivity.this);

        btnCreateCreateAccount = findViewById(R.id.btnCreateCreateAccount);
        txtCreateFirstName = findViewById(R.id.txtCreateFirstName);
        txtCreateLastName = findViewById(R.id.txtCreateLastName);

        txtCreateUsername = findViewById(R.id.txtCreateUsername);
        txtCreatePassword = findViewById(R.id.txtCreatePassword);


        btnCreateCreateAccount.setOnClickListener(v -> {
            String firstname = txtCreateFirstName.getText().toString();
            String lastname = txtCreateLastName.getText().toString();
            String username = txtCreateUsername.getText().toString();
            String password = txtCreatePassword.getText().toString();

            if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty() || password.isEmpty())
            {
                Toast.makeText(CreateAccountActivity.this, "Enter all fields", Toast.LENGTH_LONG).show();
            }
            else {
                if (dbHelper.doesUserExist(username))
                {
                    Toast.makeText(CreateAccountActivity.this, "Username is already taken", Toast.LENGTH_LONG).show();
                }
                else if(password.length() < 7)
                {
                    Toast.makeText(CreateAccountActivity.this, "Password too short", Toast.LENGTH_LONG).show();
                }
                else {
                    dbHelper.insertUser(firstname, lastname, username, password);
                    Toast.makeText(CreateAccountActivity.this, "User created successfully", Toast.LENGTH_LONG).show();
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}