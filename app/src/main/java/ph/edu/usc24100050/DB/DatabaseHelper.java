package ph.edu.usc24100050.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ph.edu.usc24100050.Model.Place;
import ph.edu.usc24100050.Model.PlaceRating;
import ph.edu.usc24100050.Model.PlaceType;
import ph.edu.usc24100050.Model.User;
import ph.edu.usc24100050.Model.UserPlaceDetail;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mobdev";
    private static final int DB_VERSION = 1;
    private static final String USER_TABLE_NAME = "users";
    private static final String USER_COL_ID = "id";
    private static final String USER_COL_FIRSTNAME = "firstname";
    private static final String USER_COL_LASTNAME = "lastname";
    private static final String USER_COL_USERNAME = "username";
    private static final String USER_COL_PASSWORD = "password";
    private static final String USER_COL_ISACTIVE = "isactive";

    private static final String USERPLACES_TABLE_NAME = "userplaces";
    private static final String USERPLACES_COL_ID = "id";
    private static final String USERPLACES_COL_USERID = "userid";
    private static final String USERPLACES_COL_PLACEID = "placeid";
    private static final String USERPLACES_COL_REMARKS = "remarks";
    private static final String USERPLACES_COL_RATINGS = "ratings";
    private static final String USERPLACES_COL_DATE = "date";


    private static final String PLACE_TABLE_NAME = "place";
    private static final String PLACE_COL_ID = "id";
    private static final String PLACE_COL_PLACETYPEID = "placetypeid";
    private static final String PLACE_COL_NAME = "name";
    private static final String PLACE_COL_DESCRIPTION = "description";
    private static final String PLACE_COL_LONGITUDE = "longitude";
    private static final String PLACE_COL_LATITUDE = "latitude";

    private static final String PLACETYPE_TABLE_NAME = "placetype";
    private static final String PLACETYPE_COL_ID = "id";
    private static final String PLACETYPE_COL_NAME = "name";

    private static final String CREATE_USER_TABLE_QUERY = "CREATE TABLE " + USER_TABLE_NAME + " ("
            + USER_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USER_COL_FIRSTNAME + " TEXT, "
            + USER_COL_LASTNAME + " TEXT, "
            + USER_COL_USERNAME + " TEXT, "
            + USER_COL_PASSWORD + " TEXT, "
            + USER_COL_ISACTIVE + " INTEGER);";

    private static final String CREATE_PLACETYPE_TABLE_QUERY = "CREATE TABLE " + PLACETYPE_TABLE_NAME + " ("
            + PLACETYPE_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + PLACETYPE_COL_NAME + " TEXT);";

    private static final String CREATE_PLACE_TABLE_QUERY = "CREATE TABLE " + PLACE_TABLE_NAME  + " ("
            + PLACE_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + PLACE_COL_PLACETYPEID + " INTEGER, "
            + PLACE_COL_NAME + " TEXT, "
            + PLACE_COL_DESCRIPTION + " TEXT, "
            + PLACE_COL_LONGITUDE + " REAL, "
            + PLACE_COL_LATITUDE + " REAL, " +
            " FOREIGN KEY("+ PLACE_COL_PLACETYPEID +") REFERENCES " + PLACETYPE_TABLE_NAME + "("+ PLACETYPE_COL_ID +"));";

    private static final String CREATE_USERPLACES_TABLE_QUERY = "CREATE TABLE " + USERPLACES_TABLE_NAME + " ("
            + USERPLACES_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USERPLACES_COL_USERID + " INTEGER, "
            + USERPLACES_COL_PLACEID + " INTEGER, "
            + USERPLACES_COL_REMARKS + " TEXT, "
            + USERPLACES_COL_RATINGS + " REAL NOT NULL, "
            + USERPLACES_COL_DATE + " TEXT DEFAULT CURRENT_TIMESTAMP, "
            + " FOREIGN KEY("+ USERPLACES_COL_USERID +") REFERENCES " + USER_TABLE_NAME + "(" + USER_COL_ID +"), "
            + " FOREIGN KEY("+ USERPLACES_COL_PLACEID +") REFERENCES " + PLACE_TABLE_NAME + "(" + PLACE_COL_ID +"));";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE_QUERY);
        db.execSQL(CREATE_PLACETYPE_TABLE_QUERY);
        db.execSQL(CREATE_PLACE_TABLE_QUERY);
        db.execSQL(CREATE_USERPLACES_TABLE_QUERY);

        initializeSeed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        String dropUserPlacesTableQuery = "DROP TABLE IF EXISTS " + USERPLACES_TABLE_NAME;
        String dropUserTableQuery = "DROP TABLE IF EXISTS " + USER_TABLE_NAME;
        String dropPlaceTableQuery = "DROP TABLE IF EXISTS " + PLACE_TABLE_NAME;
        String dropPlaceTypeTableQuery = "DROP TABLE IF EXISTS " + PLACETYPE_TABLE_NAME;

        db.execSQL(dropUserPlacesTableQuery);
        db.execSQL(dropUserTableQuery);
        db.execSQL(dropPlaceTableQuery);
        db.execSQL(dropPlaceTypeTableQuery);

        onCreate(db);
    }

    private void initializeSeed(SQLiteDatabase db) {
        initializeUserSeed(db);
        initializePlaceTypeSeed(db);
        initializePlaceSeed(db);
        initializeUserPlacesSeed(db);
    }

    private void initializeUserSeed(SQLiteDatabase db)
    {
        this.insertUser(db, "John", "Doe", "johndoe", "123456789");
        this.insertUser(db, "Jane", "Smith", "janesmith", "password123");
        this.insertUser(db, "Michael", "Brown", "mikebrown", "securePass9");
        this.insertUser(db, "Emily", "Davis", "emilyd", "emDavis2024");
        this.insertUser(db, "Chris", "Wilson", "chrisw", "wilsonPass88");
        this.insertUser(db, "Sophia", "Taylor", "sophiat", "SophiaT123");
    }

    private void initializePlaceTypeSeed(SQLiteDatabase db) {
        this.insertPlaceType(db, "Food");
        this.insertPlaceType(db, "Relax");
        this.insertPlaceType(db, "Entertainment");
        this.insertPlaceType(db, "Shopping");
        this.insertPlaceType(db, "Fitness");
    }

    private void initializePlaceSeed(SQLiteDatabase db)
    {
        insertPlace(db, 1, "Larsian BBQ", "Famous open-air barbecue spot in Cebu with affordable grilled food.");
        insertPlace(db, 1, "House of Lechon", "Popular restaurant known for Cebu’s signature roasted pig.");
        insertPlace(db, 1, "Pungko-Pungko sa Fuente", "Street-style fried food where you eat with bare hands.");
        insertPlace(db, 1, "Cafe Laguna", "Filipino comfort food with a modern restaurant setting.");
        insertPlace(db, 1, "STK ta Bay!", "Seafood restaurant known for sutukil-style cooking.");
        insertPlace(db, 2, "Tops Lookout", "Scenic viewpoint overlooking Cebu City, best during sunset.");
        insertPlace(db, 2, "Temple of Leah", "Roman-inspired temple with a peaceful atmosphere and city views.");
        insertPlace(db, 2, "Sirao Flower Garden", "Colorful flower garden often called Cebu’s Little Amsterdam.");
        insertPlace(db, 2, "Mountain View Nature Park", "Quiet mountain resort with relaxing views and fresh air.");
        insertPlace(db, 2, "Il Corso Seaside", "Relaxing seaside area with open spaces and ocean breeze.");
        insertPlace(db, 3, "SM Seaside Sky Park", "Rooftop park with playgrounds and scenic views.");
        insertPlace(db, 3, "Waterfront Cebu Casino", "Entertainment complex with gaming and live shows.");
        insertPlace(db, 3, "Anjo World Theme Park", "Theme park with rides and attractions near Cebu.");
        insertPlace(db, 3, "Escape Cebu", "Escape room experience for groups and friends.");
        insertPlace(db, 3, "Parkmall Events Area", "Hosts events, shows, and live entertainment.");
        insertPlace(db, 4, "SM City Cebu", "One of the largest malls with shops, dining, and entertainment.");
        insertPlace(db, 4, "Ayala Center Cebu", "Upscale mall with a garden and wide range of stores.");
        insertPlace(db, 4, "Carbon Market", "Traditional public market with local goods and street finds.");
        insertPlace(db, 4, "Gaisano Country Mall", "Budget-friendly mall with everyday essentials.");
        insertPlace(db, 4, "Robinsons Galleria Cebu", "Modern mall with retail stores and restaurants.");
        insertPlace(db, 5, "Cebu IT Park Jogging Area", "Popular area for jogging and outdoor exercise.");
        insertPlace(db, 5, "Abellana Sports Complex", "Main sports facility with track and fitness areas.");
        insertPlace(db, 5, "Anytime Fitness Cebu", "24/7 gym with complete workout equipment.");
        insertPlace(db, 5, "CrossFit Pintados", "High-intensity functional training gym.");
        insertPlace(db, 5, "Cebu City Sports Center", "Public facility for running, workouts, and sports.");
    }

    private void initializeUserPlacesSeed(SQLiteDatabase db)
    {
        //john
        insertUserPlace(db, 1, 1, "Great BBQ experience", 4.5);
        insertUserPlace(db, 1, 2, null, 5.0);
        insertUserPlace(db, 1, 6, "Nice view at night", 4.2);
        insertUserPlace(db, 1, 11, null, 3.8);

        //jane
        insertUserPlace(db, 2, 3, "Affordable and tasty", 4.0);
        insertUserPlace(db, 2, 7, "Very relaxing place", 4.7);
        insertUserPlace(db, 2, 12, null, 3.5);
        insertUserPlace(db, 2, 16, "Loved shopping here", 4.3);

        //michael
        insertUserPlace(db, 3, 4, null, 3.9);
        insertUserPlace(db, 3, 8, "Beautiful flowers!", 4.8);
        insertUserPlace(db, 3, 13, "Fun rides", 4.1);
        insertUserPlace(db, 3, 21, null, 4.0);

        //emily
        insertUserPlace(db, 4, 5, "Seafood was fresh", 4.6);
        insertUserPlace(db, 4, 9, null, 4.2);
        insertUserPlace(db, 4, 14, "Challenging escape room", 4.9);
        insertUserPlace(db, 4, 22, "Good gym equipment", 4.3);

        //chris
        insertUserPlace(db, 5, 1, null, 4.1);
        insertUserPlace(db, 5, 10, "Nice seaside breeze", 4.4);
        insertUserPlace(db, 5, 15, null, 3.7);
        insertUserPlace(db, 5, 23, "Great for jogging", 4.5);

        //sophia
        insertUserPlace(db, 6, 2, "Lechon was amazing!", 5.0);
        insertUserPlace(db, 6, 6, null, 4.3);
        insertUserPlace(db, 6, 18, "Crowded but good", 3.9);
        insertUserPlace(db, 6, 24, null, 4.2);
    }

    public boolean insertUserPlace(int userId, int placeId, String remarks, double rating)
    {
        SQLiteDatabase db = getWritableDatabase();
        return insertUserPlace(db, userId, placeId, remarks, rating);

    }

    public boolean insertUserPlace(SQLiteDatabase db, int userId, int placeId, String remarks, double rating)
    {
        ContentValues values = new ContentValues();

        values.put(USERPLACES_COL_USERID, userId);
        values.put(USERPLACES_COL_PLACEID, placeId);
        values.put(USERPLACES_COL_REMARKS, remarks);
        if (rating < 1.0 || rating > 5.0)
            rating = 5.0; // can throw later
        values.put(USERPLACES_COL_RATINGS, rating);

        long result = db.insert(USERPLACES_TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean insertPlace(int placetypeId, String name, String description)
    {
        SQLiteDatabase db = getWritableDatabase();
        return insertPlace(db, placetypeId, name, description);
    }

    public boolean insertPlace(SQLiteDatabase db, int placetypeId, String name, String description)
    {
        ContentValues values = new ContentValues();

        values.put(PLACE_COL_PLACETYPEID, placetypeId);
        values.put(PLACE_COL_NAME, name);
        values.put(PLACE_COL_DESCRIPTION, description);
        values.put(PLACE_COL_LONGITUDE, 0);
        values.put(PLACE_COL_LATITUDE, 0);

        long result = db.insert(PLACE_TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean insertUser(String firstname, String lastname, String username, String password)
    {
        SQLiteDatabase db = getWritableDatabase();
        return insertUser(db, firstname, lastname, username, password);
    }
    public boolean insertUser(SQLiteDatabase db, String firstname, String lastname, String username, String password)
    {
        ContentValues values = new ContentValues();

        values.put(USER_COL_FIRSTNAME, firstname);
        values.put(USER_COL_LASTNAME, lastname);
        values.put(USER_COL_USERNAME, username);
        values.put(USER_COL_PASSWORD, password);
        values.put(USER_COL_ISACTIVE, 1);

        long result = db.insert(USER_TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean insertPlaceType(String name)
    {
        SQLiteDatabase db = getWritableDatabase();
        return insertPlaceType(db, name);
    }

    public boolean insertPlaceType(SQLiteDatabase db, String name)
    {
        ContentValues values = new ContentValues();

        values.put(PLACETYPE_COL_NAME, name);

        long result = db.insert(PLACETYPE_TABLE_NAME, null, values);
        return result != -1;
    }


    public User Login(String username, String password)
    {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + USER_TABLE_NAME + " WHERE " + USER_COL_USERNAME + " = ? AND " + USER_COL_PASSWORD+ " = ?";
        String[] params = new String[] {username, password};

        Cursor cursor = db.rawQuery(query, params);
        User user = null;
        if(cursor.moveToFirst())
        {
            user = new User();
            user.setID(cursor.getInt(0));
            user.setFirstName(cursor.getString(1));
            user.setLastName(cursor.getString(2));
            user.setUsername(cursor.getString(3));
            user.setPassword(cursor.getString(4));
        }
        cursor.close();
        return user;
    }

    public List<PlaceType> getAllPlaceType()
    {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + PLACETYPE_TABLE_NAME;

        Cursor cursor = db.rawQuery(query, null);
        List<PlaceType> placeTypeList = new ArrayList<>();
        while (cursor.moveToNext())
        {
            PlaceType placeType = new PlaceType();
            placeType.setID(cursor.getInt(0));
            placeType.setName(cursor.getString(1));
            placeTypeList.add(placeType);
        }

        cursor.close();
        return placeTypeList;

    }

    public List<PlaceRating> getAllPlaceWithRating()
    {
        SQLiteDatabase db = getReadableDatabase();

        // must include all ratings of that place
        String query = "SELECT p.*, AVG(up." + USERPLACES_COL_RATINGS+ "), pt.name FROM " + PLACE_TABLE_NAME
                + " AS p LEFT JOIN " + USERPLACES_TABLE_NAME + " AS up" +
                " ON p." + PLACE_COL_ID +" = up." + USERPLACES_COL_PLACEID +
                " INNER JOIN " + PLACETYPE_TABLE_NAME + " AS pt " +
                " ON p." + PLACE_COL_PLACETYPEID + " = pt." + PLACETYPE_COL_ID +
                " GROUP BY p." + PLACE_COL_ID + ", p." + PLACE_COL_PLACETYPEID + ", p." + PLACE_COL_NAME +", p." + PLACE_COL_DESCRIPTION +" ;";

        Cursor cursor = db.rawQuery(query, null);

        List<PlaceRating> placeRatings = new ArrayList<>();

        while(cursor.moveToNext())
        {
            PlaceRating placeRating = new PlaceRating();
            placeRating.setID(cursor.getInt(0));
            placeRating.setPlaceTypeID(cursor.getInt(1));
            placeRating.setName(cursor.getString(2));
            placeRating.setDescription(cursor.getString(3));
            placeRating.setRating(cursor.getDouble(6));
            placeRating.setPlaceTypeName(cursor.getString(7));
            placeRatings.add(placeRating);

        }
        cursor.close();

        return placeRatings;
    }
    public boolean doesUserExist(String username)
    {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + USER_TABLE_NAME + " WHERE " + USER_COL_USERNAME + " = ?";
        String[] params = new String[] {username};
        Cursor cursor = db.rawQuery(query, params);
        return cursor.moveToFirst();
    }

//    public Place getPlaceById(int id)
//    {
//
//    }
//
//    public List<UserPlaceDetail> getAllRatedPlaceByUser(int userId)
//    {
//        // must get all the visited place by a specific user
//    }
}
