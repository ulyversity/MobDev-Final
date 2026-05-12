package ph.edu.usc24100050;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class PlannerViewModel extends ViewModel {

    private MutableLiveData<List<Getaway>> getawaysLiveData;
    private List<Getaway> getawayList = new ArrayList<>();

    public LiveData<List<Getaway>> getGetaways() {
        if (getawaysLiveData == null) {
            getawaysLiveData = new MutableLiveData<>();
            loadSampleData();
        }
        return getawaysLiveData;
    }

    private void loadSampleData() {
        getawayList.add(new Getaway("Bantayan Island", "Fri, Oct 24 - Sun, Oct 26", "₱2,500", "Beach • Relax"));
        getawayList.add(new Getaway("Moalboal Sardine Run", "Sat, Nov 1 - Sun, Nov 2", "₱1,800", "Diving • Adventure"));
        getawayList.add(new Getaway("Sirao Garden & Temple of Leah", "Sun, Nov 9", "₱800", "Sightseeing • Photography"));

        getawaysLiveData.setValue(new ArrayList<>(getawayList));
    }

    public void addGetaway(Getaway getaway) {
        getawayList.add(0, getaway); // Add to the top
        getawaysLiveData.setValue(new ArrayList<>(getawayList));
    }
}
