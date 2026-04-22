package my.edu.utar.RecycleGO.database;

public class CommunityModel {
    private String communityID;
    private String name;
    private String iconUrl;

    public CommunityModel() {}

    public CommunityModel(String communityID, String name, String iconUrl) {
        this.communityID = communityID;
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public String getCommunityID() { return communityID; }
    public void setCommunityID(String communityID) { this.communityID = communityID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
