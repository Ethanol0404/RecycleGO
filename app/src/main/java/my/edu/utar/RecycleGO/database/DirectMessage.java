package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;

public class DirectMessage {
    private String senderId;
    private String senderName;
    private String message;
    private Timestamp timestamp;

    public DirectMessage() {} // Required for Firestore

    public DirectMessage(String senderId, String senderName, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = Timestamp.now();
    }

    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
}