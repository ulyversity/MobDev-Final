package ph.edu.usc24100050.Model;

import java.util.List;

public class UserItineraryPreference {
    private String activityName;
    private List<String> cities;

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public List<String> getCities() {
        return cities;
    }

    public void setCities(List<String> cities) {
        this.cities = cities;
    }
}
