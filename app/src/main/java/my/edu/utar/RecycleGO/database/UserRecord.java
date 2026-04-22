package my.edu.utar.RecycleGO.database;

import java.util.ArrayList;
import java.util.List;

public class UserRecord {
    private String uid;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String role;
    private String profilePicUrl;
    private List<String> subscribedCommunities;
    private String recycleCenter; // New attribute for Admin role

    public UserRecord() {
        this.subscribedCommunities = new ArrayList<>();
    }

    public UserRecord(String uid, String username, String email, String password, String role) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = "";
        this.profilePicUrl = "";
        this.recycleCenter = "";
        this.subscribedCommunities = new ArrayList<>();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public List<String> getSubscribedCommunities() { return subscribedCommunities; }
    public void setSubscribedCommunities(List<String> subscribedCommunities) { this.subscribedCommunities = subscribedCommunities; }

    public String getRecycleCenter() { return recycleCenter; }
    public void setRecycleCenter(String recycleCenter) { this.recycleCenter = recycleCenter; }
}
