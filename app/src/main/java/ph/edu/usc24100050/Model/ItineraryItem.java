package ph.edu.usc24100050.Model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "itinerary_items")
public class ItineraryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    private int day;
    private String time;
    private String placeName;
    private String placeType;
    private int durationMinutes;
    private String notes;
    private double latitude;
    private String task; // Added back for backward compatibility
    private double longitude;

    // Getters and Setters
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getPlaceType() { return placeType; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
}
