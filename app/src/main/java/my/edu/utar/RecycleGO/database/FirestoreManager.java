package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class FirestoreManager {
    private final FirebaseFirestore db;
    private static final String COLLECTION_CENTERS = "centers";
    private static final String COLLECTION_REQUESTS = "recycle_requests";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_COMMUNITIES = "communities";
    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_CAMPAIGNS = "campaigns";

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================== Campaigns ====================
    public void getUpcomingCampaigns(OnListFetchListener<CampaignRecord> listener) {
        db.collection(COLLECTION_CAMPAIGNS)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CampaignRecord> list = queryDocumentSnapshots.toObjects(CampaignRecord.class);
                    listener.onListFetched(list);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void createCampaign(CampaignRecord campaign, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_CAMPAIGNS).document(campaign.getId()).set(campaign)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Centers ====================
    public CollectionReference getCentersCollection() {
        return db.collection(COLLECTION_CENTERS);
    }

    public void addRecycleCenter(RecycleCenter center, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_CENTERS).add(center)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Requests ====================
    public CollectionReference getRequestsCollection() {
        return db.collection(COLLECTION_REQUESTS);
    }

    public void submitRequest(RecycleRequest request, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateRecycleRequest(RecycleRequest request, OnTaskCompleteListener listener) {
        if (request.getId() == null) {
            listener.onFailure("Request ID is missing.");
            return;
        }
        db.collection(COLLECTION_REQUESTS).document(request.getId()).set(request)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateRequestStatus(String requestId, String newStatus, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteRecycleRequest(String requestId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId).delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public Query getRequestsByUser(String userId) {
        return db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING);
    }

    public Query getRequestsByCenters(List<String> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) {
            // Return a query that will be empty but valid
            return db.collection(COLLECTION_REQUESTS).whereEqualTo("centerId", "NON_EXISTENT");
        }
        return db.collection(COLLECTION_REQUESTS)
                .whereIn("centerId", centerIds)
                .orderBy("date", Query.Direction.DESCENDING);
    }

    // ==================== Users ====================
    public void getUser(String uid, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onUserFetched(documentSnapshot.toObject(UserRecord.class));
                    } else {
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserByEmail(String email, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        listener.onUserFetched(queryDocumentSnapshots.getDocuments().get(0).toObject(UserRecord.class));
                    } else {
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void signIn(String email, String password, String role, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        listener.onUserFetched(queryDocumentSnapshots.getDocuments().get(0).toObject(UserRecord.class));
                    } else {
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void saveUser(UserRecord user, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(user.getUid()).set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void joinRecycleCenter(String uid, String centerId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid)
                .update("joinedCenters", FieldValue.arrayUnion(centerId))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateUser(String uid, java.util.Map<String, Object> updates, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Points Methods ====================
    public void updateUserPoints(String uid, int newPoints, OnTaskCompleteListener listener) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("points", newPoints);

        db.collection(COLLECTION_USERS).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void addPoints(String uid, int pointsToAdd, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid)
                .update("points", FieldValue.increment(pointsToAdd))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserPoints(String uid, OnPointsFetchListener listener) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long points = documentSnapshot.getLong("points");
                        listener.onPointsFetched(points != null ? points.intValue() : 0);
                    } else {
                        listener.onPointsFetched(0);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Community ====================
    public void getAllCommunities(OnListFetchListener<CommunityModel> listener) {
        db.collection(COLLECTION_COMMUNITIES).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onListFetched(queryDocumentSnapshots.toObjects(CommunityModel.class));
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void addCommunity(CommunityModel community, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMUNITIES).add(community)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    documentReference.update("communityID", id);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void subscribeToCommunity(String uid, String communityID, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid)
                .update("subscribedCommunities", FieldValue.arrayUnion(communityID))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Posts ====================
    public void createPost(CommunityPost post, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_POSTS).document(post.getPostID()).set(post)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getFeedPosts(List<String> communityIDs, OnListFetchListener<CommunityPost> listener) {
        if (communityIDs == null || communityIDs.isEmpty()) {
            listener.onListFetched(new java.util.ArrayList<>());
            return;
        }
        db.collection(COLLECTION_POSTS)
                .whereIn("communityID", communityIDs)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onListFetched(queryDocumentSnapshots.toObjects(CommunityPost.class));
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserPosts(String uid, OnListFetchListener<CommunityPost> listener) {
        db.collection(COLLECTION_POSTS)
                .whereEqualTo("authorUID", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onListFetched(queryDocumentSnapshots.toObjects(CommunityPost.class));
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void toggleLike(String postID, boolean isLiked, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_POSTS).document(postID)
                .update("likes", FieldValue.increment(isLiked ? 1 : -1))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Comments ====================
    public void addComment(CommunityComment comment, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMENTS).add(comment)
                .addOnSuccessListener(documentReference -> {
                    db.collection(COLLECTION_POSTS).document(comment.getPostID())
                            .update("commentsCount", FieldValue.increment(1));
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getComments(String postID, OnListFetchListener<CommunityComment> listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("postID", postID)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onListFetched(queryDocumentSnapshots.toObjects(CommunityComment.class));
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Callback Interfaces ====================
    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnUserFetchListener {
        void onUserFetched(UserRecord user);
        void onFailure(String error);
    }

    public interface OnListFetchListener<T> {
        void onListFetched(java.util.List<T> list);
        void onFailure(String error);
    }

    public interface OnPointsFetchListener {
        void onPointsFetched(int points);
        void onFailure(String error);
    }
}
