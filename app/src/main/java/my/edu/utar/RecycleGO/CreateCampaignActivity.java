package my.edu.utar.RecycleGO;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import my.edu.utar.RecycleGO.database.CampaignRecord;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class CreateCampaignActivity extends Fragment {

    private EditText etEventName, etLocation, etDateTime;
    private ImageView ivPreview;
    private Button btnSubmit;
    private Calendar calendar;
    private FirestoreManager firestoreManager;
    private Uri selectedImageUri = null;
    private Uri cameraImageUri = null;
    private String manualImageUrl = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    manualImageUrl = null;
                    ivPreview.setImageURI(uri);
                    ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivPreview.setImageTintList(null);
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    selectedImageUri = cameraImageUri;
                    manualImageUrl = null;
                    ivPreview.setImageURI(selectedImageUri);
                    ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivPreview.setImageTintList(null);
                }
            }
    );

    public CreateCampaignActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_create_campaign, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreManager = new FirestoreManager();
        calendar = Calendar.getInstance();

        etEventName = view.findViewById(R.id.etEventName);
        etLocation = view.findViewById(R.id.etLocation);
        etDateTime = view.findViewById(R.id.etDateTime);
        ivPreview = view.findViewById(R.id.ivPreview);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        etDateTime.setOnClickListener(v -> showDateTimePicker());
        
        view.findViewById(R.id.cvSelectImage).setOnClickListener(v -> showImageSourceOptions());

        btnSubmit.setOnClickListener(v -> submitCampaign());

        applyCustomTheme(view);
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bottomColor = Color.parseColor(bottomColorCode);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);

        if (tvTitle != null) tvTitle.setTextColor(bottomColor);
        if (btnBack != null) btnBack.setColorFilter(bottomColor);
        if (btnSubmit != null) {
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
            btnSubmit.setTextColor(Color.WHITE); // Ensure white text
        }

        updateTextColorsRecursively((ViewGroup) view, bottomColor);
    }

    private void updateTextColorsRecursively(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateTextColorsRecursively((ViewGroup) child, color);
            } else if (child instanceof TextView && !(child instanceof EditText) && !(child instanceof Button)) {
                ((TextView) child).setTextColor(color);
            }
        }
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Input Image URL"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        showUrlInputDialog();
                    }
                })
                .show();
    }

    private void showUrlInputDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_url_input, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Theme Support
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
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
            btnOk.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
            btnOk.setTextColor(Color.WHITE); // Ensure white word
        }
        if (btnCancel != null) btnCancel.setTextColor(bottomColor);

        btnOk.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                manualImageUrl = url;
                selectedImageUri = null;
                ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivPreview.setImageTintList(null);
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

    private void launchCamera() {
        File tempFile = new File(requireContext().getCacheDir(), "temp_campaign_image.jpg");
        cameraImageUri = FileProvider.getUriForFile(requireContext(), 
                requireContext().getPackageName() + ".fileprovider", tempFile);
        takePictureLauncher.launch(cameraImageUri);
    }

    private void showDateTimePicker() {
        if (getContext() == null) return;

        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(getContext(), (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());
                etDateTime.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitCampaign() {
        String name = etEventName.getText().toString().trim();
        String loc = etLocation.getText().toString().trim();
        String dateStr = etDateTime.getText().toString().trim();

        if (name.isEmpty() || loc.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Publishing...");

        if (selectedImageUri != null) {
            uploadImageAndCreateCampaign(name, loc);
        } else if (manualImageUrl != null) {
            saveCampaignToFirestore(name, loc, manualImageUrl);
        } else {
            saveCampaignToFirestore(name, loc, null);
        }
    }

    private void uploadImageAndCreateCampaign(String name, String loc) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("campaign_images/" + UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveCampaignToFirestore(name, loc, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Publish Campaign");
                    Toast.makeText(getContext(), "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCampaignToFirestore(String name, String loc, String imageUrl) {
        String id = UUID.randomUUID().toString();
        Timestamp timestamp = new Timestamp(calendar.getTime());

        CampaignRecord campaign = new CampaignRecord(id, name, timestamp, loc);
        campaign.setImageUrl(imageUrl);
        
        // Retrieve current logged in user ID to set as creator
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String currentUid = sharedPreferences.getString("loggedInUid", "");
        campaign.setCreatedBy(currentUid);

        firestoreManager.createCampaign(campaign, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Campaign Published!", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String error) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Publish Campaign");
                Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
