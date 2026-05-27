package ph.edu.usc24100050.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DayPlan {
    private String date;
    @JsonProperty("day_number")
    private int dayNumber;
    private List<DayActivity> activities;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public List<DayActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<DayActivity> activities) {
        this.activities = activities;
    }
}
