package my.edu.utar.RecycleGO.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final String IMAGE_DIR = "community_images";

    /**
     * Copies an image from a Uri (gallery/camera) to app's internal storage.
     * @param context Application context
     * @param imageUri Uri of the selected image
     * @return The filename of the saved image, or null if failed.
     */
    public static String saveImageToInternalStorage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            String fileName = "IMG_" + UUID.randomUUID().toString() + ".jpg";
            File directory = new File(context.getFilesDir(), IMAGE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            File file = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    /**
     * Gets the File object for an image stored in internal storage.
     * @param context Application context
     * @param fileName Filename of the image
     * @return File object
     */
    public static File getImageFile(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        File directory = new File(context.getFilesDir(), IMAGE_DIR);
        return new File(directory, fileName);
    }
}
