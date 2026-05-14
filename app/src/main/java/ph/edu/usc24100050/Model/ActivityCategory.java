package ph.edu.usc24100050.Model;

import java.util.List;

public class ActivityCategory {
    public String categoryName;

    public String backgroundColor;

    public List<ActivityItem> categoryList;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public List<ActivityItem> getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(List<ActivityItem> categoryList) {
        this.categoryList = categoryList;
    }
}
