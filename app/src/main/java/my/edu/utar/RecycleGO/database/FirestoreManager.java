package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    private final FirebaseFirestore db;
    private static final String COLLECTION_CENTERS = "centers";
    private static final String COLLECTION_REQUESTS = "recycle_requests";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_COMMUNITIES = "communities";
    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_CAMPAIGNS = "campaigns";
    private static final String COLLECTION_POINT_RECORDS = "point_record";
    private static final String COLLECTION_QUIZ_RECORDS = "quiz_records";
    private static final String COLLECTION_RECYCLE_MATERIAL = "recycle_material";

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

    public void acceptRequest(String requestId, String centerId, String centerName, OnTaskCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Accepted");
        updates.put("centerId", centerId);
        updates.put("centerName", centerName);
        updates.put("targetCenterIds", java.util.Collections.singletonList(centerId));

        db.collection(COLLECTION_REQUESTS).document(requestId).update(updates)
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

    public void getCompletedRequestsByUser(String userId, OnListFetchListener<RecycleRequest> listener) {
        db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Completed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RecycleRequest> list = queryDocumentSnapshots.toObjects(RecycleRequest.class);
                    listener.onListFetched(list);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void sendMessage(String requestId, DirectMessage message, OnTaskCompleteListener listener) {
        // 1. Add message to sub-collection
        db.collection(COLLECTION_REQUESTS).document(requestId)
                .collection("messages").add(message)
                .addOnSuccessListener(documentReference -> {
                    // 2. Update parent doc with last message time
                    db.collection(COLLECTION_REQUESTS).document(requestId)
                            .update("lastMessageTime", com.google.firebase.Timestamp.now());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateLastRead(String requestId, String role, OnTaskCompleteListener listener) {
        String field = "Admin".equals(role) ? "lastReadAdmin" : "lastReadUser";
        db.collection(COLLECTION_REQUESTS).document(requestId)
                .update(field, com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> { if(listener!=null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if(listener!=null) listener.onFailure(e.getMessage()); });
    }

    public com.google.firebase.firestore.Query getChatMessages(String requestId) {
        return db.collection(COLLECTION_REQUESTS).document(requestId)
                .collection("messages").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING);
    }

    public void submitReport(String requestId, String userId, String reason, OnTaskCompleteListener listener) {
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("requestId", requestId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("timestamp", com.google.firebase.Timestamp.now());
        report.put("status", "Pending");

        db.collection("reports").add(report)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public Query getRequestsByCenters(List<String> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) {
            return db.collection(COLLECTION_REQUESTS).whereEqualTo("centerId", "NON_EXISTENT");
        }
        // Use targetCenterIds array to find requests eligible for these centers
        return db.collection(COLLECTION_REQUESTS)
                .whereArrayContainsAny("targetCenterIds", centerIds)
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

    public ListenerRegistration listenToUser(String uid, OnUserFetchListener listener) {
        return db.collection(COLLECTION_USERS).document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                listener.onFailure(e.getMessage());
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                listener.onUserFetched(snapshot.toObject(UserRecord.class));
            } else {
                listener.onUserFetched(null);
            }
        });
    }

    public void getAllUsers(OnListFetchListener<UserRecord> listener) {
        db.collection(COLLECTION_USERS)
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserRecord> list = queryDocumentSnapshots.toObjects(UserRecord.class);
                    listener.onListFetched(list);
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

    public void addPoints(String uid, int pointsToAdd, String activityType, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid)
                .update("points", FieldValue.increment(pointsToAdd))
                .addOnSuccessListener(aVoid -> {
                    addPointRecord(uid, pointsToAdd, activityType, listener);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
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

    // ==================== Point Records ====================
    private void addPointRecord(String userId, int points, String activityType, OnTaskCompleteListener listener) {
        DocumentReference ref = db.collection(COLLECTION_POINT_RECORDS).document(userId);
        ref.get().addOnSuccessListener(doc -> {
            List<String> activities = new ArrayList<>();
            List<Long> pts = new ArrayList<>();
            List<Timestamp> timestamps = new ArrayList<>();

            if (doc.exists()) {
                if (doc.get("activity") != null) activities = (List<String>) doc.get("activity");
                if (doc.get("point") != null) pts = (List<Long>) doc.get("point");
                if (doc.get("timestamp") != null) timestamps = (List<Timestamp>) doc.get("timestamp");
            }

            activities.add(activityType);
            pts.add((long) points);
            timestamps.add(Timestamp.now());

            Map<String, Object> data = new HashMap<>();
            data.put("uid", userId);
            data.put("activity", activities);
            data.put("point", pts);
            data.put("timestamp", timestamps);

            ref.set(data)
                    .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e.getMessage()); });
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onFailure(e.getMessage());
        });
    }

    public void getPointHistory(String userId, OnPointsHistoryFetchListener listener) {
        db.collection(COLLECTION_POINT_RECORDS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> activities = (List<String>) documentSnapshot.get("activity");
                        List<Long> points = (List<Long>) documentSnapshot.get("point");
                        List<Timestamp> timestamps = (List<Timestamp>) documentSnapshot.get("timestamp");

                        if (activities == null) activities = new java.util.ArrayList<>();
                        if (points == null) points = new java.util.ArrayList<>();
                        if (timestamps == null) timestamps = new java.util.ArrayList<>();

                        listener.onHistoryFetched(activities, points, timestamps);
                    } else {
                        listener.onHistoryFetched(new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void saveQuizRecord(String userId, int points, OnTaskCompleteListener listener) {
        Map<String, Object> quizRecord = new HashMap<>();
        quizRecord.put("userId", userId);
        quizRecord.put("points", points);
        quizRecord.put("timestamp", Timestamp.now());

        db.collection(COLLECTION_QUIZ_RECORDS).add(quizRecord)
                .addOnSuccessListener(documentReference -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
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

    public void updateCommunity(CommunityModel community, OnTaskCompleteListener listener) {
        if (community.getCommunityID() == null) {
            listener.onFailure("Community ID is missing.");
            return;
        }
        db.collection(COLLECTION_COMMUNITIES).document(community.getCommunityID()).set(community)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteCommunity(String communityId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMUNITIES).document(communityId).delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
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

    // ==================== Material Stats ====================
    public void incrementMaterialCount(String userId, String category, OnTaskCompleteListener listener) {
        DocumentReference ref = db.collection(COLLECTION_RECYCLE_MATERIAL).document(userId);
        ref.get().addOnSuccessListener(doc -> {
            List<String> materials = new ArrayList<>();
            List<Long> counts = new ArrayList<>();

            if (doc.exists()) {
                materials = (List<String>) doc.get("materials");
                counts = (List<Long>) doc.get("counts");
            }
            if (materials == null) materials = new ArrayList<>();
            if (counts == null) counts = new ArrayList<>();

            int index = materials.indexOf(category);
            if (index != -1) {
                counts.set(index, counts.get(index) + 1);
            } else {
                materials.add(category);
                counts.add(1L);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("materials", materials);
            data.put("counts", counts);

            ref.set(data)
                    .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e.getMessage()); });
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onFailure(e.getMessage());
        });
    }

    public void getMaterialStats(String userId, OnMaterialStatsFetchListener listener) {
        db.collection(COLLECTION_RECYCLE_MATERIAL).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> materials = (List<String>) doc.get("materials");
                        List<Long> counts = (List<Long>) doc.get("counts");
                        listener.onStatsFetched(materials, counts);
                    } else {
                        listener.onStatsFetched(new ArrayList<>(), new ArrayList<>());
                    }
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

    public interface OnPointsHistoryFetchListener {
        void onHistoryFetched(List<String> activities, List<Long> points, List<Timestamp> timestamps);
        void onFailure(String error);
    }

    public interface OnMaterialStatsFetchListener {
        void onStatsFetched(List<String> materials, List<Long> counts);
        void onFailure(String error);
    }
}
