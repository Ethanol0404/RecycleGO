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

import my.edu.utar.RecycleGO.R;

public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final String IMAGE_DIR = "app_images";

    public static String saveImageToInternalStorage(Context context, Uri imageUri) {
        if (imageUri == null) return null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            String fileName = "IMG_" + UUID.randomUUID().toString() + ".jpg";
            File directory = new File(context.getFilesDir(), IMAGE_DIR);
            if (!directory.exists()) directory.mkdirs();
            
            File file = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    public static void loadImage(Context context, String fileName, android.widget.ImageView imageView) {
        // Use R.drawable.request as the original default photo for most cases
        loadImage(context, fileName, imageView, R.drawable.request);
    }

    public static void loadImage(Context context, String fileName, android.widget.ImageView imageView, int defaultResId) {
        if (fileName == null || fileName.isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                    .load(defaultResId)
                    .into(imageView);
            return;
        }
        
        // Check if it's a URL
        if (fileName.startsWith("http")) {
            com.bumptech.glide.Glide.with(context)
                    .load(fileName)
                    .placeholder(defaultResId)
                    .error(defaultResId)
                    .into(imageView);
            return;
        }

        File directory = new File(context.getFilesDir(), IMAGE_DIR);
        File file = new File(directory, fileName);
        if (file.exists()) {
            com.bumptech.glide.Glide.with(context)
                    .load(file)
                    .placeholder(defaultResId)
                    .error(defaultResId)
                    .into(imageView);
        } else {
            com.bumptech.glide.Glide.with(context)
                    .load(defaultResId)
                    .into(imageView);
        }
    }
}
