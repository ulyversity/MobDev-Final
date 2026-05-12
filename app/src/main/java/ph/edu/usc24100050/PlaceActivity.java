package ph.edu.usc24100050;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.edu.usc24100050.Adapter.PlaceRatingAdapter;
import ph.edu.usc24100050.DB.DatabaseHelper;
import ph.edu.usc24100050.Model.PlaceRating;
import ph.edu.usc24100050.Model.PlaceType;
import ph.edu.usc24100050.Model.User;

public class PlaceActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    TextView txtWelcome;
    Spinner spinPlaceType;
    RecyclerView rvPlaceRatings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_place);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(PlaceActivity.this);

        txtWelcome = findViewById(R.id.txtWelcome);
        spinPlaceType = findViewById(R.id.spinPlaceType);
        rvPlaceRatings = findViewById(R.id.rvPlaceRatings);


        User user = getIntent().getParcelableExtra("user");
        txtWelcome.setText("Welcome " + user.getFirstName() + " " + user.getLastName());

        List<PlaceType> placeTypeList = dbHelper.getAllPlaceType();

        PlaceType placeholder = new PlaceType();
        placeholder.setID(0);
        placeholder.setName("Filter Place");
        placeTypeList.add(0, placeholder);


        ArrayAdapter adapter = new ArrayAdapter(PlaceActivity.this, android.R.layout.simple_spinner_item, placeTypeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinPlaceType.setAdapter(adapter);

        List<PlaceRating> placeRatingList = dbHelper.getAllPlaceWithRating();

        PlaceRatingAdapter placeRatingAdapter = new PlaceRatingAdapter(PlaceActivity.this, placeRatingList);

        rvPlaceRatings.setLayoutManager(new LinearLayoutManager(PlaceActivity.this));
        rvPlaceRatings.setAdapter(placeRatingAdapter);

    }
}