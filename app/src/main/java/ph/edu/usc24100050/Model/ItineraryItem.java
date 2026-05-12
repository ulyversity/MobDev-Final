package ph.edu.usc24100050.Model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "itinerary_items")
public class ItineraryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String time;
    public String task;
    public String date;
}