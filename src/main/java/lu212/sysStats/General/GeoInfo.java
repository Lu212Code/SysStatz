package lu212.sysStats.General;

public class GeoInfo {
    public String city;
    public String country;
    public double lat;
    public double lon;

    public GeoInfo() {
        // Standard-Konstruktor für Jackson
    }

    public GeoInfo(String city, String country, double lat, double lon) {
        this.city = city;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
    }
}
