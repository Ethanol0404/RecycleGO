package my.edu.utar.RecycleGO;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import my.edu.utar.RecycleGO.database.CampaignRecord;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class CreateCampaignActivity extends Fragment {

    private EditText etEventName, etLocation, etDateTime;
    private ImageView ivPreview;
    private Button btnSubmit;
    private Calendar calendar;
    private FirestoreManager firestoreManager;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                    ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
        
        view.findViewById(R.id.cvSelectImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> submitCampaign());
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

        String id = UUID.randomUUID().toString();
        Timestamp timestamp = new Timestamp(calendar.getTime());

        String imageUrl = null;
        if (selectedImageUri != null) {
            imageUrl = ImageManager.saveImageToInternalStorage(requireContext(), selectedImageUri);
        }

        CampaignRecord campaign = new CampaignRecord(id, name, timestamp, loc);
        campaign.setImageUrl(imageUrl);

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
                Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
