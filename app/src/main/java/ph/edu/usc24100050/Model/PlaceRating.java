package ph.edu.usc24100050.Model;

public class PlaceRating {

    private int ID;
    private int placeTypeID;
    private String placeTypeName;

    public String getPlaceTypeName() {
        return placeTypeName;
    }

    public void setPlaceTypeName(String placeTypeName) {
        this.placeTypeName = placeTypeName;
    }

    private String name;
    private String description;
    private double rating;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getPlaceTypeID() {
        return placeTypeID;
    }

    public void setPlaceTypeID(int placeTypeID) {
        this.placeTypeID = placeTypeID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
