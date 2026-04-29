package my.edu.utar.RecycleGO;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private ImageView ivPreview;
    private Spinner spinnerCommunity;
    private Uri selectedImageUri; 
    private Uri cameraImageUri;
    private String manualPhotoUrl;
    private FirestoreManager firestoreManager;
    private String userUid;
    private String username;
    private String communityID;
    private List<CommunityModel> subscribedCommunities = new ArrayList<>();

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    manualPhotoUrl = null;
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
                    manualPhotoUrl = null;
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

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userUid = prefs.getString("loggedInUid", "");
        username = prefs.getString("loggedInUsername", "Anonymous");

        etContent = findViewById(R.id.et_content);
        ivPreview = findViewById(R.id.iv_preview);
        spinnerCommunity = findViewById(R.id.spinner_community);
        Button btnPost = findViewById(R.id.btn_post);
        Button btnAddPhoto = findViewById(R.id.btn_add_photo);
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        loadCommunities();

        btnAddPhoto.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery", "Insert Image URL"};
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
                        } else if (which == 1) {
                            getContent.launch("image/*");
                        } else {
                            showUrlInputDialog();
                        }
                    }).show();
        });

        btnPost.setOnClickListener(v -> submitPost());

        applyCustomTheme();
    }

    private void applyCustomTheme() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String bgColorCode = prefs.getString("theme_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bgColor = Color.parseColor(bgColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        View root = findViewById(android.R.id.content).getRootView();
        if (root != null) root.setBackgroundColor(bgColor);

        Button btnPost = findViewById(R.id.btn_post);
        if (btnPost != null) {
            btnPost.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            btnPost.setTextColor(Color.WHITE); // Set word to white as requested
        }

        updateTextColorsRecursively((ViewGroup) findViewById(android.R.id.content), bottomColor);
    }

    private void updateTextColorsRecursively(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateTextColorsRecursively((ViewGroup) child, color);
            } else if (child instanceof TextView && !(child instanceof EditText) && child.getId() != R.id.btn_post) {
                ((TextView) child).setTextColor(color);
            }
        }
    }

    private void loadCommunities() {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                subscribedCommunities = list; // For simplicity, list all or filter by subscription
                setupCommunitySpinner();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreatePostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCommunitySpinner() {
        CommunitySpinnerAdapter adapter = new CommunitySpinnerAdapter(this, subscribedCommunities);
        spinnerCommunity.setAdapter(adapter);

        if (communityID != null) {
            for (int i = 0; i < subscribedCommunities.size(); i++) {
                if (subscribedCommunities.get(i).getCommunityID().equals(communityID)) {
                    spinnerCommunity.setSelection(i);
                    break;
                }
            }
        }

        spinnerCommunity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                communityID = subscribedCommunities.get(position).getCommunityID();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showUrlInputDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_url_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String headerColorCode = prefs.getString("header_color", "#def9ac");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int headerColor = Color.parseColor(headerColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        dialogView.setBackgroundColor(headerColor);
        
        TextView title = dialogView.findViewById(R.id.dialog_title);
        EditText input = dialogView.findViewById(R.id.dialog_input);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        if (title != null) title.setTextColor(bottomColor);
        if (input != null) {
            input.setTextColor(bottomColor);
            input.setHintTextColor(Color.argb(128, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor)));
        }
        if (btnOk != null) {
            btnOk.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            btnOk.setTextColor(Color.WHITE); // Ensure white word
        }
        if (btnCancel != null) btnCancel.setTextColor(bottomColor);

        btnOk.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                manualPhotoUrl = url;
                selectedImageUri = null;
                ivPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(url).into(ivPreview);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void submitPost() {
        if (communityID == null || communityID.isEmpty()) {
            Toast.makeText(this, "Please select a community", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = null;
        if (selectedImageUri != null) {
            photoUrl = ImageManager.saveImageToInternalStorage(this, selectedImageUri);
        } else if (manualPhotoUrl != null) {
            photoUrl = manualPhotoUrl;
        }

        String postID = java.util.UUID.randomUUID().toString();
        CommunityPost post = new CommunityPost(postID, userUid, username, content, photoUrl, communityID, System.currentTimeMillis());

        firestoreManager.createPost(post, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(CreatePostActivity.this, "Post submitted!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreatePostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
