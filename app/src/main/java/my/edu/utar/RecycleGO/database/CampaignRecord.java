package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;

public class CampaignRecord {
    private String id;
    private String title;
    private Timestamp date;
    private String location;


    public CampaignRecord() {}

    public CampaignRecord(String id, String title, Timestamp date, String location, String imageUrl) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
