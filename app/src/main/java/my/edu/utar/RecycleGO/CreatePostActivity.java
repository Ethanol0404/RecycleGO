package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private ImageView ivPreview;
    private Uri selectedImageUri; 
    private Uri cameraImageUri;
    private FirestoreManager firestoreManager;
    private String userUid;
    private String username;
    private String communityID;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    ivPreview.setImageURI(cameraImageUri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", photoFile);
            takePicture.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

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

        btnAddPhoto.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Option")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                launchCamera();
                            } else {
                                requestCameraPermission.launch(android.Manifest.permission.CAMERA);
                            }
                        } else {
                            getContent.launch("image/*");
                        }
                    }).show();
        });

        btnPost.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = null;
        if (selectedImageUri != null) {
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
