package my.edu.utar.RecycleGO.database;

public class NewsRecord {
    private Object title;
    private Object color;
    private Object newsurl;
    private Object picurl;

    public NewsRecord() {
        // Required for Firestore
    }

    public NewsRecord(Object title, Object color, Object newsurl, Object picurl) {
        this.title = title;
        this.color = color;
        this.newsurl = newsurl;
        this.picurl = picurl;
    }

    public Object getTitle() { return title; }
    public void setTitle(Object title) { this.title = title; }

    public Object getColor() { return color; }
    public void setColor(Object color) { this.color = color; }

    public Object getNewsurl() { return newsurl; }
    public void setNewsurl(Object newsurl) { this.newsurl = newsurl; }

    public Object getPicurl() { return picurl; }
    public void setPicurl(Object picurl) { this.picurl = picurl; }

    // Safe getters to handle both String and ArrayList
    public String getTitleAsString() { return extractString(title); }
    public String getColorAsString() { return extractString(color); }
    public String getNewsurlAsString() { return extractString(newsurl); }
    public String getPicurlAsString() { return extractString(picurl); }

    private String extractString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof java.util.List && !((java.util.List<?>) value).isEmpty()) {
            Object first = ((java.util.List<?>) value).get(0);
            return first != null ? first.toString() : "";
        }
        return value != null ? value.toString() : "";
    }
}
