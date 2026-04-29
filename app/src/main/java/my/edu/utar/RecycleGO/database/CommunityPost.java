package my.edu.utar.RecycleGO.database;

import java.util.ArrayList;
import java.util.List;

public class CommunityPost {
    private String postID;
    private String authorUID;
    private String authorName;
    private String content;
    private String photoUrl;
    private List<String> hashtags;
    private int likes;
    private List<String> likedBy; // List of UIDs who liked this post
    private int commentsCount;
    private long timestamp;
    private String communityID;

    public CommunityPost() {
        this.hashtags = new ArrayList<>();
        this.likedBy = new ArrayList<>();
    }

    public CommunityPost(String postID, String authorUID, String authorName, String content, String photoUrl, String communityID) {
        this(postID, authorUID, authorName, content, photoUrl, communityID, System.currentTimeMillis());
    }

    public CommunityPost(String postID, String authorUID, String authorName, String content, String photoUrl, String communityID, long timestamp) {
        this.postID = postID;
        this.authorUID = authorUID;
        this.authorName = authorName;
        this.content = content;
        this.photoUrl = photoUrl;
        this.communityID = communityID;
        this.timestamp = timestamp;
        this.hashtags = new ArrayList<>();
        this.likedBy = new ArrayList<>();
        this.likes = 0;
        this.commentsCount = 0;
    }

    public String getPostID() { return postID; }
    public void setPostID(String postID) { this.postID = postID; }

    public String getAuthorUID() { return authorUID; }
    public void setAuthorUID(String authorUID) { this.authorUID = authorUID; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCommunityID() { return communityID; }
    public void setCommunityID(String communityID) { this.communityID = communityID; }
}
