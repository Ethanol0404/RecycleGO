package my.edu.utar.RecycleGO.database;

import com.google.firebase.firestore.DocumentId;

public class RecycleCenter {
    @DocumentId
    public String id;
    public String name;
    public String address;
    public double latitude;
    public double longitude;
    public String operatingHours;
    public String supportedServices;
    public String phoneNumber;
    
    // Distance field (not persisted in Firestore)
    public transient float distance = 0f;

    public RecycleCenter() {
        // Required for Firestore
    }
}
