package ph.edu.usc24100050.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DayActivity {
    private String activity;
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("stop_time")
    private String stopTime;
    private String venue;
    
    // Richer fields to sync with Umiko's rich itinerary format
    @JsonProperty("place_type")
    private String placeType;
    @JsonProperty("duration_minutes")
    private int durationMinutes;
    private String notes;
    @JsonProperty("travel_from_previous")
    private String travelFromPrevious;
    private double latitude;
    private double longitude;

    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getStopTime() { return stopTime; }
    public void setStopTime(String stopTime) { this.stopTime = stopTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getPlaceType() { return placeType; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTravelFromPrevious() { return travelFromPrevious; }
    public void setTravelFromPrevious(String travelFromPrevious) { this.travelFromPrevious = travelFromPrevious; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
