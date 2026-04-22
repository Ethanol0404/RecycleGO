package my.edu.utar.RecycleGO.database;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;

public class FirestoreManager {
    private final FirebaseFirestore db;
    private static final String COLLECTION_CENTERS = "centers";
    private static final String COLLECTION_REQUESTS = "recycle_requests";

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Centers
    public CollectionReference getCentersCollection() {
        return db.collection(COLLECTION_CENTERS);
    }

    // Requests
    public CollectionReference getRequestsCollection() {
        return db.collection(COLLECTION_REQUESTS);
    }

    public void submitRequest(RecycleRequest request, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).add(request)
            .addOnSuccessListener(documentReference -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateRequestStatus(String requestId, String newStatus, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public Query getRequestsByUser(String userId) {
        return db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING);
    }

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}
