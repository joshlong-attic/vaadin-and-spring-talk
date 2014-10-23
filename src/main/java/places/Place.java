package places;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.social.facebook.api.Page;

import java.io.Serializable;
import java.util.Date;

@Document
public class Place implements Serializable {

    @Id
    private String id;

    @GeoSpatialIndexed(name = "position")
    private double[] position;
    private String city;
    private String country;
    private String description;
    private double latitude;
    private double longitude;
    private String state;
    private String street;
    private String zip;
    private String name;
    private String affilitation;
    private String category;
    private String about;
    private Date insertionDate;

    @Override
    public String toString() {
        return "Place{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", category='" + category + '\'' +
                ", insertionDate=" + insertionDate +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAffilitation(String affilitation) {
        this.affilitation = affilitation;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setInsertionDate(Date insertionDate) {
        this.insertionDate = insertionDate;
    }

    public String getCountry() {
        return country;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getState() {
        return state;
    }

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }

    public double[] getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getAffilitation() {
        return affilitation;
    }

    public String getCategory() {
        return category;
    }

    public String getAbout() {
        return about;
    }

    public Date getInsertionDate() {
        return insertionDate;
    }

    public Place(Page p) {
        this.affilitation = p.getAffiliation();
        this.id = p.getId();
        this.name = p.getName();
        this.category = p.getCategory();
        this.description = p.getDescription();
        this.about = p.getAbout();
        this.insertionDate = new Date();
        org.springframework.social.facebook.api.Location pageLocation = p.getLocation();
        this.city = pageLocation.getCity();
        this.country = pageLocation.getCountry();
        this.description = pageLocation.getDescription();
        this.latitude = pageLocation.getLatitude();
        this.longitude = pageLocation.getLongitude();
        this.state = pageLocation.getState();
        this.street = pageLocation.getStreet();
        this.zip = pageLocation.getZip();
        this.position = new double[]{this.longitude, this.latitude};
    }

    public Place() {
    }

    public Place(String id) {
        this.id = id;
    }


}
