package my.edu.utar.RecycleGO;

import android.graphics.Bitmap;
import android.net.Uri;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private Bitmap imageBitmap;
    private Uri imageUri;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public ChatMessage(String message, boolean isUser, Bitmap imageBitmap) {
        this.message = message;
        this.isUser = isUser;
        this.imageBitmap = imageBitmap;
    }

    public ChatMessage(String message, boolean isUser, Uri imageUri) {
        this.message = message;
        this.isUser = isUser;
        this.imageUri = imageUri;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public boolean hasImage() {
        return imageBitmap != null || imageUri != null;
    }
}