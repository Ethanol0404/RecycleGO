package my.edu.utar.RecycleGO.database;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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
    private static final String COLLECTION_NEWS = "news";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_JOIN_REQUESTS = "request_join";

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    private void safeSuccess(OnTaskCompleteListener listener) {
        if (listener != null) listener.onSuccess();
    }

    private void safeFailure(OnTaskCompleteListener listener, String error) {
        if (listener != null) listener.onFailure(error);
    }

    // ==================== Join Requests ====================
    public void submitJoinRequest(String userId, String username, String centerId, String centerName, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(userId).get().addOnSuccessListener(userDoc -> {
            String contact = userDoc.getString("phone");
            if (contact == null || contact.isEmpty()) contact = userDoc.getString("email");
            
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("username", username);
            request.put("centerId", centerId);
            request.put("centerName", centerName);
            request.put("status", "Pending");
            request.put("timestamp", Timestamp.now());
            request.put("contactInfo", contact != null ? contact : "No contact info");

            WriteBatch batch = db.batch();
            DocumentReference requestRef = db.collection(COLLECTION_JOIN_REQUESTS).document();
            batch.set(requestRef, request);
            
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            batch.update(userRef, "requestedCenters", FieldValue.arrayUnion(centerId));

            batch.commit()
                    .addOnSuccessListener(aVoid -> safeSuccess(listener))
                    .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
        }).addOnFailureListener(e -> safeFailure(listener, "Failed to fetch user contact: " + e.getMessage()));
    }

    public Query getJoinRequestsForCenter(String centerId) {
        return db.collection(COLLECTION_JOIN_REQUESTS)
                .whereEqualTo("centerId", centerId)
                .orderBy("status", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public void processJoinRequest(String requestId, String userId, String centerId, boolean approve, OnTaskCompleteListener listener) {
        String newStatus = approve ? "Approved" : "Rejected";
        WriteBatch batch = db.batch();
        
        DocumentReference requestRef = db.collection(COLLECTION_JOIN_REQUESTS).document(requestId);
        batch.update(requestRef, "status", newStatus);
        
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        batch.update(userRef, "requestedCenters", FieldValue.arrayRemove(centerId));
        
        if (approve) {
            batch.update(userRef, "joinedCenters", FieldValue.arrayUnion(centerId));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    sendNotification(userId, "Join Request " + newStatus, 
                        "Your request to join the center has been " + newStatus.toLowerCase() + ".", "JoinRequest", centerId);
                    safeSuccess(listener);
                })
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    // ==================== News ====================
    public void getNews(OnListFetchListener<NewsRecord> listener) {
        db.collection(COLLECTION_NEWS).get()
                .addOnSuccessListener(queryDocumentSnapshots -> listener.onListFetched(queryDocumentSnapshots.toObjects(NewsRecord.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Campaigns ====================
    public void getUpcomingCampaigns(OnListFetchListener<CampaignRecord> listener) {
        db.collection(COLLECTION_CAMPAIGNS).orderBy("date", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> listener.onListFetched(queryDocumentSnapshots.toObjects(CampaignRecord.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void createCampaign(CampaignRecord campaign, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_CAMPAIGNS).document(campaign.getId()).set(campaign)
                .addOnSuccessListener(aVoid -> {
                    sendNotificationToAll("New Campaign", "A new campaign '" + campaign.getTitle() + "' has been published!", "Campaign", campaign.getId());
                    safeSuccess(listener);
                })
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void joinCampaign(String campaignId, String userId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_CAMPAIGNS).document(campaignId)
                .update("participants", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    // ==================== Centers ====================
    public CollectionReference getCentersCollection() { return db.collection(COLLECTION_CENTERS); }

    public void addRecycleCenter(RecycleCenter center, OnTaskCompleteListener listener) {
        DocumentReference ref;
        if (center.id != null && !center.id.isEmpty()) {
            ref = db.collection(COLLECTION_CENTERS).document(center.id);
        } else {
            ref = db.collection(COLLECTION_CENTERS).document();
            center.id = ref.getId();
        }
        ref.set(center)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void getCentersByIds(List<String> centerIds, OnListFetchListener<RecycleCenter> listener) {
        if (centerIds == null || centerIds.isEmpty()) {
            listener.onListFetched(new ArrayList<>());
            return;
        }
        db.collection(COLLECTION_CENTERS)
                .whereIn(FieldPath.documentId(), centerIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RecycleCenter> centers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        RecycleCenter center = doc.toObject(RecycleCenter.class);
                        center.id = doc.getId();
                        centers.add(center);
                    }
                    listener.onListFetched(centers);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Requests ====================
    public CollectionReference getRequestsCollection() { return db.collection(COLLECTION_REQUESTS); }

    public void submitRequest(RecycleRequest request, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(documentReference -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void updateRecycleRequest(RecycleRequest request, OnTaskCompleteListener listener) {
        if (request.getId() == null) {
            safeFailure(listener, "ID missing");
            return;
        }
        db.collection(COLLECTION_REQUESTS).document(request.getId()).set(request)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void updateRequestStatus(String requestId, String newStatus, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId).update("status", newStatus)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void acceptRequest(String requestId, String adminId, String centerId, String centerName, OnTaskCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Accepted");
        updates.put("centerId", centerId);
        updates.put("centerName", centerName);
        updates.put("acceptedAdminId", adminId);
        updates.put("targetCenterIds", java.util.Collections.singletonList(centerId));
        db.collection(COLLECTION_REQUESTS).document(requestId).update(updates)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void deleteRecycleRequest(String requestId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId).delete()
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public Query getRequestsByUser(String userId) {
        return db.collection(COLLECTION_REQUESTS).whereEqualTo("userId", userId).orderBy("date", Query.Direction.DESCENDING);
    }

    public void sendMessage(String requestId, DirectMessage message, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_REQUESTS).document(requestId).collection("messages").add(message)
                .addOnSuccessListener(documentReference -> {
                    db.collection(COLLECTION_REQUESTS).document(requestId).update("lastMessageTime", Timestamp.now());
                    safeSuccess(listener);
                })
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void updateLastRead(String requestId, String role, OnTaskCompleteListener listener) {
        String field = "Admin".equals(role) ? "lastReadAdmin" : "lastReadUser";
        db.collection(COLLECTION_REQUESTS).document(requestId).update(field, Timestamp.now())
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public Query getChatMessages(String requestId) {
        return db.collection(COLLECTION_REQUESTS).document(requestId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public void submitReport(String requestId, String userId, String reason, OnTaskCompleteListener listener) {
        Map<String, Object> report = new HashMap<>();
        report.put("requestId", requestId);
        report.put("userId", userId);
        report.put("reason", reason);
        report.put("timestamp", Timestamp.now());
        report.put("status", "Pending");
        db.collection("reports").add(report)
                .addOnSuccessListener(ref -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public Query getRequestsByCenters(List<String> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) return db.collection(COLLECTION_REQUESTS).whereEqualTo("centerId", "NONE");
        return db.collection(COLLECTION_REQUESTS).whereArrayContainsAny("targetCenterIds", centerIds).orderBy("date", Query.Direction.DESCENDING);
    }

    // ==================== Users ====================
    public void getUser(String uid, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserRecord user = doc.toObject(UserRecord.class);
                        if (user != null) user.setUid(doc.getId());
                        listener.onUserFetched(user);
                    } else {
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public ListenerRegistration listenToUser(String uid, OnUserFetchListener listener) {
        return db.collection(COLLECTION_USERS).document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null) { listener.onFailure(e.getMessage()); return; }
            if (snapshot != null && snapshot.exists()) {
                UserRecord user = snapshot.toObject(UserRecord.class);
                if (user != null) user.setUid(snapshot.getId());
                listener.onUserFetched(user);
            } else {
                listener.onUserFetched(null);
            }
        });
    }

    public void signIn(String email, String password, String role, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS).whereEqualTo("email", email).whereEqualTo("password", password).whereEqualTo("role", role).get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snaps.getDocuments().get(0);
                        UserRecord user = doc.toObject(UserRecord.class);
                        user.setUid(doc.getId());
                        listener.onUserFetched(user);
                    } else {
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void saveUser(UserRecord user, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(user.getUid()).set(user)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void joinRecycleCenter(String uid, String centerId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update("joinedCenters", FieldValue.arrayUnion(centerId))
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void updateUser(String uid, Map<String, Object> updates, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void addPoints(String uid, int points, String type, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update("points", FieldValue.increment(points))
                .addOnSuccessListener(aVoid -> addPointRecord(uid, points, type, listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void getAllUsers(OnListFetchListener<UserRecord> listener) {
        // Removed .whereEqualTo("role", "User") to avoid requiring a Composite Index.
        // We will filter the roles in the code below.
        db.collection(COLLECTION_USERS)
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<UserRecord> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snaps) {
                        UserRecord user = doc.toObject(UserRecord.class);
                        if (user != null && "User".equals(user.getRole())) {
                            user.setUid(doc.getId());
                            users.add(user);
                        }
                    }
                    listener.onListFetched(users);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserByEmail(String email, OnUserFetchListener listener) {
        db.collection(COLLECTION_USERS).whereEqualTo("email", email).get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snaps.getDocuments().get(0);
                        UserRecord user = doc.toObject(UserRecord.class);
                        user.setUid(doc.getId());
                        listener.onUserFetched(user);
                    } else {
                        listener.onUserFetched(null);
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
            activities.add(activityType); pts.add((long) points); timestamps.add(Timestamp.now());
            Map<String, Object> data = new HashMap<>();
            data.put("uid", userId); data.put("activity", activities); data.put("point", pts); data.put("timestamp", timestamps);
            ref.set(data).addOnSuccessListener(aVoid -> safeSuccess(listener));
        });
    }

    public void getPointHistory(String userId, OnPointsHistoryFetchListener listener) {
        db.collection(COLLECTION_POINT_RECORDS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    List<String> activities = new ArrayList<>();
                    List<Long> pts = new ArrayList<>();
                    List<Timestamp> timestamps = new ArrayList<>();
                    if (doc.exists()) {
                        if (doc.get("activity") != null) activities = (List<String>) doc.get("activity");
                        if (doc.get("point") != null) pts = (List<Long>) doc.get("point");
                        if (doc.get("timestamp") != null) timestamps = (List<Timestamp>) doc.get("timestamp");
                    }
                    listener.onHistoryFetched(activities, pts, timestamps);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getAllCommunities(OnListFetchListener<CommunityModel> listener) {
        db.collection(COLLECTION_COMMUNITIES).get()
                .addOnSuccessListener(snaps -> listener.onListFetched(snaps.toObjects(CommunityModel.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void addCommunity(CommunityModel community, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMUNITIES).add(community).addOnSuccessListener(ref -> {
            ref.update("communityID", ref.getId());
            safeSuccess(listener);
        }).addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void updateCommunity(CommunityModel community, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMUNITIES).document(community.getCommunityID()).set(community)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void deleteCommunity(String communityId, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMUNITIES).document(communityId).delete()
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void subscribeToCommunity(String uid, String communityID, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update("subscribedCommunities", FieldValue.arrayUnion(communityID))
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void unsubscribeFromCommunity(String uid, String communityID, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(uid).update("subscribedCommunities", FieldValue.arrayRemove(communityID))
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void createPost(CommunityPost post, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_POSTS).document(post.getPostID()).set(post)
                .addOnSuccessListener(aVoid -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void toggleLike(String postID, boolean isLiked, String userId, OnTaskCompleteListener listener) {
        WriteBatch batch = db.batch();
        DocumentReference postRef = db.collection(COLLECTION_POSTS).document(postID);

        batch.update(postRef, "likes", FieldValue.increment(isLiked ? 1 : -1));
        if (isLiked) {
            batch.update(postRef, "likedBy", FieldValue.arrayUnion(userId));
        } else {
            batch.update(postRef, "likedBy", FieldValue.arrayRemove(userId));
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            if (isLiked) {
                postRef.get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String authorId = doc.getString("authorUID");
                        if (authorId != null && !authorId.equals(userId)) {
                            sendNotification(authorId, "New Like", "Someone liked your post!", "Like", postID);
                        }
                    }
                });
            }
            safeSuccess(listener);
        }).addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void addComment(CommunityComment comment, OnTaskCompleteListener listener) {
        db.collection(COLLECTION_COMMENTS).add(comment).addOnSuccessListener(ref -> {
            db.collection(COLLECTION_POSTS).document(comment.getPostID()).update("commentsCount", FieldValue.increment(1));
            
            db.collection(COLLECTION_POSTS).document(comment.getPostID()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String authorId = doc.getString("authorUID");
                    if (authorId != null && !authorId.equals(comment.getAuthorUID())) {
                        sendNotification(authorId, "New Comment", "Someone commented on your post: " + comment.getText(), "Comment", comment.getPostID());
                    }
                }
            });
            
            safeSuccess(listener);
        }).addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    public void getComments(String postID, OnListFetchListener<CommunityComment> listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("postID", postID)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> listener.onListFetched(queryDocumentSnapshots.toObjects(CommunityComment.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getFeedPosts(List<String> communityIds, OnListFetchListener<CommunityPost> listener) {
        if (communityIds == null || communityIds.isEmpty()) {
            listener.onListFetched(new ArrayList<>());
            return;
        }
        db.collection(COLLECTION_POSTS)
                .whereIn("communityID", communityIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> listener.onListFetched(snaps.toObjects(CommunityPost.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserPosts(String userId, OnListFetchListener<CommunityPost> listener) {
        db.collection(COLLECTION_POSTS)
                .whereEqualTo("authorUID", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> listener.onListFetched(snaps.toObjects(CommunityPost.class)))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getPost(String postID, OnPostFetchListener listener) {
        db.collection(COLLECTION_POSTS).document(postID).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        CommunityPost post = doc.toObject(CommunityPost.class);
                        listener.onPostFetched(post);
                    } else {
                        listener.onPostFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Quiz Records ====================
    public void saveQuizRecord(String userId, int points, OnTaskCompleteListener listener) {
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("score", points);
        record.put("timestamp", Timestamp.now());
        db.collection(COLLECTION_QUIZ_RECORDS).add(record)
                .addOnSuccessListener(ref -> safeSuccess(listener))
                .addOnFailureListener(e -> safeFailure(listener, e.getMessage()));
    }

    // ==================== Material Stats ====================
    public void incrementMaterialCount(String userId, String category, OnTaskCompleteListener listener) {
        DocumentReference ref = db.collection(COLLECTION_RECYCLE_MATERIAL).document(userId);
        ref.get().addOnSuccessListener(doc -> {
            List<String> materials = new ArrayList<>(); List<Long> counts = new ArrayList<>();
            if (doc.exists()) {
                materials = (List<String>) doc.get("materials"); counts = (List<Long>) doc.get("counts");
            }
            if (materials == null) materials = new ArrayList<>(); if (counts == null) counts = new ArrayList<>();
            int idx = materials.indexOf(category);
            if (idx != -1) counts.set(idx, counts.get(idx) + 1);
            else { materials.add(category); counts.add(1L); }
            Map<String, Object> data = new HashMap<>(); data.put("userId", userId); data.put("materials", materials); data.put("counts", counts);
            ref.set(data).addOnSuccessListener(aVoid -> safeSuccess(listener));
        });
    }

    public void getMaterialStats(String userId, OnMaterialStatsFetchListener listener) {
        db.collection(COLLECTION_RECYCLE_MATERIAL).document(userId).get()
                .addOnSuccessListener(doc -> {
                    List<String> materials = new ArrayList<>();
                    List<Long> counts = new ArrayList<>();
                    if (doc.exists()) {
                        if (doc.get("materials") != null) materials = (List<String>) doc.get("materials");
                        if (doc.get("counts") != null) counts = (List<Long>) doc.get("counts");
                    }
                    listener.onStatsFetched(materials, counts);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ==================== Real-time Notifications ====================
    public void sendNotification(String userId, String title, String message, String type, String targetID) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("type", type);
        notif.put("targetID", targetID);

        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).add(notif);
    }

    public void sendNotificationToAll(String title, String message, String type, String targetID) {
        db.collection(COLLECTION_USERS).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                DocumentReference userRef = doc.getReference();
                Map<String, Object> notif = new HashMap<>();
                notif.put("title", title);
                notif.put("message", message);
                notif.put("timestamp", System.currentTimeMillis());
                notif.put("read", false);
                notif.put("type", type);
                notif.put("targetID", targetID);
                userRef.collection(COLLECTION_NOTIFICATIONS).add(notif);
            }
        });
    }

    public ListenerRegistration listenToNotifications(String userId, OnListFetchListener<NotificationModel> listener) {
        return db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { listener.onFailure(e.getMessage()); return; }
                    if (snapshots != null) {
                        List<NotificationModel> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            NotificationModel model = doc.toObject(NotificationModel.class);
                            model.setId(doc.getId());
                            list.add(model);
                        }
                        listener.onListFetched(list);
                    }
                });
    }

    public void markNotificationsAsRead(String userId) {
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    public void markNotificationAsRead(String userId, String notificationId) {
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).document(notificationId)
                .update("read", true);
    }

    public interface OnTaskCompleteListener { void onSuccess(); void onFailure(String error); }
    public interface OnUserFetchListener { void onUserFetched(UserRecord user); void onFailure(String error); }
    public interface OnPostFetchListener { void onPostFetched(CommunityPost post); void onFailure(String error); }
    public interface OnListFetchListener<T> { void onListFetched(List<T> list); void onFailure(String error); }
    public interface OnPointsFetchListener { void onPointsFetched(int points); void onFailure(String error); }
    public interface OnPointsHistoryFetchListener { void onHistoryFetched(List<String> act, List<Long> pts, List<Timestamp> ts); void onFailure(String error); }
    public interface OnMaterialStatsFetchListener { void onStatsFetched(List<String> mat, List<Long> cnt); void onFailure(String error); }
}
