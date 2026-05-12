package ph.edu.usc24100050.DB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ph.edu.usc24100050.Model.ItineraryItem;

@Dao
public interface ItineraryDao {
    @Insert
    void insertAll(List<ItineraryItem> items);

    @Query("SELECT * FROM itinerary_items ORDER BY time ASC")
    LiveData<List<ItineraryItem>> getAll();

    @Query("DELETE FROM itinerary_items")
    void clearAll();
}