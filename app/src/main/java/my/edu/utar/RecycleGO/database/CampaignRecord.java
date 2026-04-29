package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CampaignRecord {
    private String id;
    private String title;
    private Timestamp date;
    private String location;
    private String imageUrl;
    private List<String> participants;
    private String createdBy;

    public CampaignRecord() {
        this.participants = new ArrayList<>();
    }

    public CampaignRecord(String id, String title, Timestamp date, String location) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.location = location;
        this.participants = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
