package my.edu.utar.RecycleGO.database;

public class CommunityComment {
    private String commentID;
    private String postID;
    private String authorUID;
    private String authorName;
    private String text;
    private String photoUrl; // Added photo support
    private long timestamp;

    public CommunityComment() {}

    public CommunityComment(String commentID, String postID, String authorUID, String authorName, String text) {
        this.commentID = commentID;
        this.postID = postID;
        this.authorUID = authorUID;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public String getCommentID() { return commentID; }
    public void setCommentID(String commentID) { this.commentID = commentID; }

    public String getPostID() { return postID; }
    public void setPostID(String postID) { this.postID = postID; }

    public String getAuthorUID() { return authorUID; }
    public void setAuthorUID(String authorUID) { this.authorUID = authorUID; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
