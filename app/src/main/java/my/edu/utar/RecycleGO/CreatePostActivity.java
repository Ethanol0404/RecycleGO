package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import android.os.Environment;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private ImageView ivPreview;
    private Uri selectedImageUri; // From Gallery
    private Uri photoUri; // From Camera
    private FirestoreManager firestoreManager;
    private String userUid;
    private String username;
    private String communityID;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    photoUri = null; // Clear camera uri if gallery is picked
                    ivPreview.setImageURI(selectedImageUri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    // Photo is saved to photoUri!
                    selectedImageUri = null; // Clear gallery uri
                    ivPreview.setImageURI(photoUri);
                    ivPreview.setVisibility(View.VISIBLE);

                    // Notify the gallery to show it
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(photoUri);
                    sendBroadcast(mediaScanIntent);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        firestoreManager = new FirestoreManager();
        communityID = getIntent().getStringExtra("communityID");
        
        if (communityID == null || communityID.isEmpty()) {
            Toast.makeText(this, "Error: No community selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userUid = prefs.getString("loggedInUid", "");
        username = prefs.getString("loggedInUsername", "Anonymous");

        etContent = findViewById(R.id.et_content);
        ivPreview = findViewById(R.id.iv_preview);
        Button btnPost = findViewById(R.id.btn_post);
        Button btnAddPhoto = findViewById(R.id.btn_add_photo);
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        // Remove Spinner - User only posts to the specific community passed in
        View spinner = findViewById(R.id.spinner_community);
        if (spinner != null) spinner.setVisibility(View.GONE);
        View spinnerLabel = findViewById(R.id.spinner_community_label);
        if (spinnerLabel != null) spinnerLabel.setVisibility(View.GONE);

        btnAddPhoto.setOnClickListener(v -> {
            // Show options for Camera/Gallery?
            // For now, let's just stick to Camera as requested
            try {
                File photoFile = createImageFile();
                photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                takePicture.launch(photoUri);
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        });

        btnPost.setOnClickListener(v -> submitPost());
    }

    private File createImageFile() throws IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void submitPost() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = null;
        if (photoUri != null) {
            photoUrl = ImageManager.saveImageToInternalStorage(this, photoUri);
        } else if (selectedImageUri != null) {
            photoUrl = ImageManager.saveImageToInternalStorage(this, selectedImageUri);
        }

        String postID = java.util.UUID.randomUUID().toString();
        CommunityPost post = new CommunityPost(postID, userUid, username, content, photoUrl, communityID);

        firestoreManager.createPost(post, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(CreatePostActivity.this, "Post Created!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreatePostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
