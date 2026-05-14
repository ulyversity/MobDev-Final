package ph.edu.usc24100050.DB;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ph.edu.usc24100050.Model.ItineraryItem;

@Database(entities = {ItineraryItem.class}, version = 2) // Incremented version
public abstract class AppDatabase extends RoomDatabase {
    public abstract ItineraryDao itineraryDao();

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(2);

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "itinerary_db"
                    )
                    .fallbackToDestructiveMigration() // Wipe and recreate on schema change
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}