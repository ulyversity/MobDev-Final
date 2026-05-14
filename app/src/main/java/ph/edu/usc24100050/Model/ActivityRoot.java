package ph.edu.usc24100050.Model;

import java.util.List;

public class ActivityRoot {
    public String activity;
    public List<ActivityCategory> categories;

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public List<ActivityCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ActivityCategory> categories) {
        this.categories = categories;
    }
}
