package ph.edu.usc24100050;

public class Getaway {
    private String destination;
    private String dates;
    private String estimatedFare;
    private String tags; // e.g., "Nature, Beach, Historical"

    public Getaway(String destination, String dates, String estimatedFare, String tags) {
        this.destination = destination;
        this.dates = dates;
        this.estimatedFare = estimatedFare;
        this.tags = tags;
    }

    public String getDestination() { return destination; }
    public String getDates() { return dates; }
    public String getEstimatedFare() { return estimatedFare; }
    public String getTags() { return tags; }
}
