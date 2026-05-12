package ph.edu.usc24100050.Model;

import java.util.List;

public class DayPlan {
    private String date;
    private List<DayActivity> activities;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<DayActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<DayActivity> activities) {
        this.activities = activities;
    }
}
