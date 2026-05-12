package ph.edu.usc24100050;

// CebuSpot.java
public class CebuSpot {
    private String name;
    private String category;
    private String location;
    private String description;
    private String tips;
    private String priceRange;

    public CebuSpot(String name, String category, String location,
                    String description, String tips, String priceRange) {
        this.name = name;
        this.category = category;
        this.location = location;
        this.description = description;
        this.tips = tips;
        this.priceRange = priceRange;
    }

    // Getters
    public String getName()        { return name; }
    public String getCategory()    { return category; }
    public String getLocation()    { return location; }
    public String getDescription() { return description; }
    public String getTips()        { return tips; }
    public String getPriceRange()  { return priceRange; }
}