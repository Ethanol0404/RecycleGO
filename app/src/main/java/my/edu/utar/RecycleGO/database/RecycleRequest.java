package my.edu.utar.RecycleGO.database;

import com.google.firebase.firestore.DocumentId;

public class RecycleRequest implements java.io.Serializable {
    @DocumentId
    private String id;
    private String userId;
    private String category;
    private String date;
    private String contact;
    private String remarks;
    private String centerId;
    private String centerName;
    private String status; // "Requesting", "Accepted", "Completed"
    private String photoUrl; // Added for storing image reference

    public RecycleRequest() {
        // Required for Firestore
    }

    public RecycleRequest(String userId, String category, String date, String contact, String remarks) {
        this.userId = userId;
        this.category = category;
        this.date = date;
        this.contact = contact;
        this.remarks = remarks;
        this.status = "Requesting";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getCenterId() { return centerId; }
    public void setCenterId(String centerId) { this.centerId = centerId; }

    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
