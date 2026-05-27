package ph.edu.usc24100050.Model;

import java.util.List;

public class ItineraryResponseModel {
    public List<ItineraryItemModel> itinerary;

    public List<ItineraryItemModel> getItinerary() {
        return itinerary;
    }

    public void setItinerary(List<ItineraryItemModel> itinerary) {
        this.itinerary = itinerary;
    }
}
