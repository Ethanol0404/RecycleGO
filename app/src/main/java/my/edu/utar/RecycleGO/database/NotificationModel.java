package my.edu.utar.RecycleGO.database;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;
    private String type; // e.g., "Request", "Campaign", "System", "Like", "Comment"
    private String targetID; // ID of the Post or Campaign to navigate to

    public NotificationModel() {
        // Required for Firestore
    }

    public NotificationModel(String title, String message, long timestamp, String type, String targetID) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.targetID = targetID;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetID() { return targetID; }
    public void setTargetID(String targetID) { this.targetID = targetID; }
}
