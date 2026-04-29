package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import my.edu.utar.RecycleGO.database.CommunityComment;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class CommentBottomSheet extends BottomSheetDialogFragment {

    private String postID;
    private String currentUserName = "Anonymous";
    private String currentUserUID;
    private RecyclerView rvComments;
    private EditText etComment;
    private ImageView ivPreview;
    private AdapterComment adapter;
    private FirestoreManager firestoreManager;
    
    private Uri selectedImageUri;
    private Uri cameraImageUri;

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
                    Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    public CommentBottomSheet() {
        // Required empty constructor
    }

    public CommentBottomSheet(String postID) {
        this.postID = postID;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_comment, container, false);

        firestoreManager = new FirestoreManager();
        currentUserUID = FirebaseAuth.getInstance().getUid();
        
        if (currentUserUID != null) {
            firestoreManager.getUser(currentUserUID, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        currentUserName = user.getUsername();
                    }
                }

                @Override
                public void onFailure(String error) {}
            });
        }

        rvComments = view.findViewById(R.id.rv_comments);
        etComment = view.findViewById(R.id.et_comment);
        ivPreview = view.findViewById(R.id.iv_comment_preview);
        ImageButton btnSend = view.findViewById(R.id.btn_send_comment);
        ImageButton btnAddPhoto = view.findViewById(R.id.btn_add_image);

        adapter = new AdapterComment(new ArrayList<>());
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(adapter);

        loadComments();

        btnAddPhoto.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Option")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) 
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

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty() || selectedImageUri != null) {
                addComment(text);
            }
        });

        return view;
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePicture.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void loadComments() {
        firestoreManager.getComments(postID, new FirestoreManager.OnListFetchListener<CommunityComment>() {
            @Override
            public void onListFetched(List<CommunityComment> list) {
                adapter.updateList(list);
            }

            @Override
            public void onFailure(String error) {
                if (isAdded())
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addComment(String text) {
        if (currentUserUID == null) {
            Toast.makeText(getContext(), "You must be logged in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentID = UUID.randomUUID().toString();
        String photoUrl = null;
        if (selectedImageUri != null) {
            photoUrl = ImageManager.saveImageToInternalStorage(requireContext(), selectedImageUri);
        }

        CommunityComment comment = new CommunityComment(commentID, postID, currentUserUID, currentUserName, text);
        comment.setPhotoUrl(photoUrl);

        firestoreManager.addComment(comment, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                etComment.setText("");
                selectedImageUri = null;
                ivPreview.setVisibility(View.GONE);
                loadComments();
            }

            @Override
            public void onFailure(String error) {
                if (isAdded())
                    Toast.makeText(getContext(), "Error adding comment: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
