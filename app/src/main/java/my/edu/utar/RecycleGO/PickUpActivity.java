package my.edu.utar.RecycleGO;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleRequest;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class PickUpActivity extends Fragment {

    private TextInputEditText etCategory, etDate, etContact, etAddress, etRemarks, etCenter;
    private ImageButton btnPhoto;
    private Button btnSubmit;

    private String flow = "STATUS_TO_FORM";
    private ArrayList<String> selectedCenterIds = new ArrayList<>();
    private ArrayList<String> selectedCenterNames = new ArrayList<>();
    
    private FirestoreManager firestoreManager;
    private RecycleRequest editRequest = null;
    private boolean isEditMode = false;
    
    private Uri cameraImageUri;
    private Uri selectedImageUri;
    private String savedImageFileName = null;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (isAdded() && btnPhoto != null) {
                        com.bumptech.glide.Glide.with(this)
                                .load(uri)
                                .centerCrop()
                                .into(btnPhoto);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    if (isAdded() && btnPhoto != null) {
                        com.bumptech.glide.Glide.with(this)
                                .load(cameraImageUri)
                                .centerCrop()
                                .into(btnPhoto);
                    }
                } else {
                    if (isAdded()) Toast.makeText(getContext(), "Camera cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    if (isAdded()) Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile == null) {
                if (isAdded()) Toast.makeText(getContext(), "Failed to create storage for photo", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraImageUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePicture.launch(cameraImageUri);
        } catch (Exception e) {
            Log.e("PickUpActivity", "Error launching camera", e);
            if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
        
        if (savedInstanceState != null) {
            cameraImageUri = savedInstanceState.getParcelable("cameraImageUri");
            selectedImageUri = savedInstanceState.getParcelable("selectedImageUri");
        }

        if (getArguments() != null) {
            flow = getArguments().getString("flow", "STATUS_TO_FORM");
            
            // Handle multiple centers from Map
            if (getArguments().containsKey("selectedCenterIds")) {
                selectedCenterIds = getArguments().getStringArrayList("selectedCenterIds");
            } else if (getArguments().containsKey("centerId")) {
                selectedCenterIds = new ArrayList<>();
                selectedCenterIds.add(getArguments().getString("centerId"));
            }
            
            if (getArguments().containsKey("selectedCenterNames")) {
                selectedCenterNames = getArguments().getStringArrayList("selectedCenterNames");
            } else if (getArguments().containsKey("centerName")) {
                selectedCenterNames = new ArrayList<>();
                selectedCenterNames.add(getArguments().getString("centerName"));
            }

            if (getArguments().containsKey("edit_request")) {
                editRequest = (RecycleRequest) getArguments().getSerializable("edit_request");
                isEditMode = (editRequest != null);
                
                // If we just entered edit mode and have no selection yet, load from request
                if (isEditMode && selectedCenterIds.isEmpty()) {
                    if (editRequest.getTargetCenterIds() != null && !editRequest.getTargetCenterIds().isEmpty()) {
                        selectedCenterIds = new ArrayList<>(editRequest.getTargetCenterIds());
                        selectedCenterNames = new ArrayList<>();
                        //centerName might be a summary string if multiple.
                        selectedCenterNames.add(editRequest.getCenterName()); 
                    } else if (editRequest.getCenterId() != null) {
                        selectedCenterIds.add(editRequest.getCenterId());
                        selectedCenterNames.add(editRequest.getCenterName());
                    }
                }
            }
        }
    }

    private void navigateToMap(RecycleRequest draft) {
        Fragment nextFragment = new Map();
        Bundle args = new Bundle();
        
        // Preserve flow and edit mode
        if (isEditMode) {
            args.putString("flow", "MAP_TO_FORM");
            args.putSerializable("edit_request", editRequest);
        } else {
            args.putString("flow", "STATUS_TO_FORM");
        }

        args.putString("category", draft.getCategory());
        args.putString("date", draft.getDate());
        args.putString("contact", draft.getContact());
        args.putString("address", draft.getAddress());
        args.putString("remarks", draft.getRemarks());
        args.putString("userId", draft.getUserId());
        
        // Pass current selection so Map can highlight them
        args.putStringArrayList("selectedCenterIds", selectedCenterIds);
        
        nextFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit();
    }

    private int calculatePoints(String category, double estimatedWeight) {
        int basePoints = 0;
        if (category.contains("Plastic")) basePoints = 10;
        else if (category.contains("Metal")) basePoints = 15;
        else if (category.contains("Paper")) basePoints = 5;
        else if (category.contains("Glass")) basePoints = 8;
        int weightPoints = (int) (estimatedWeight * 5);
        return basePoints + weightPoints;
    }

    private void submitToFirestore(RecycleRequest request) {
        request.setStatus("Requesting");

        if (selectedImageUri != null) {
            savedImageFileName = ImageManager.saveImageToInternalStorage(requireContext(), selectedImageUri);
            request.setPhotoUrl(savedImageFileName);
        }

        firestoreManager.submitRequest(request, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) addPointsForRecycle(request);
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addPointsForRecycle(RecycleRequest request) {
        if (!isAdded()) return;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String currentUid = prefs.getString("loggedInUid", "");

        if (currentUid.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        double estimatedWeight = 2.0; 
        int earnedPoints = calculatePoints(request.getCategory(), estimatedWeight);

        firestoreManager.addPoints(currentUid, earnedPoints, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Request Submitted! +" + earnedPoints + " points!", Toast.LENGTH_LONG).show();

                firestoreManager.getUser(currentUid, new FirestoreManager.OnUserFetchListener() {
                    @Override
                    public void onUserFetched(UserRecord user) {
                        if (user != null && isAdded()) {
                            int newTotal = user.getTotalRecycled() + 1;
                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("totalRecycled", newTotal);
                            firestoreManager.updateUser(currentUid, updates, null);
                        }
                    }

                    @Override
                    public void onFailure(String error) {}
                });

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RecycleStatus())
                        .commit();
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Points update failed: " + error, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RecycleStatus())
                        .commit();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        View view = inflater.inflate(R.layout.activity_pick_up, container, false);

        btnPhoto = view.findViewById(R.id.btnPhoto);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        etContact = view.findViewById(R.id.etContact);
        etAddress = view.findViewById(R.id.etAddress);
        etRemarks = view.findViewById(R.id.etRemarks);
        etCenter = view.findViewById(R.id.etCenter);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        if (selectedImageUri != null) {
            com.bumptech.glide.Glide.with(this).load(selectedImageUri).centerCrop().into(btnPhoto);
        } else if (isEditMode && editRequest.getPhotoUrl() != null) {
            File file = new File(requireContext().getFilesDir(), editRequest.getPhotoUrl());
            if (file.exists()) {
                com.bumptech.glide.Glide.with(this).load(file).centerCrop().into(btnPhoto);
            }
        }

        updateCenterDisplayText();

        if (isEditMode) {
            etCategory.setText(editRequest.getCategory());
            etDate.setText(editRequest.getDate());
            etContact.setText(editRequest.getContact());
            etAddress.setText(editRequest.getAddress());
            etRemarks.setText(editRequest.getRemarks());
            btnSubmit.setText("Update Request");
        }

        etCenter.setOnClickListener(v -> {
            RecycleRequest currentDraft = new RecycleRequest();
            currentDraft.setCategory(etCategory.getText().toString());
            currentDraft.setDate(etDate.getText().toString());
            currentDraft.setContact(etContact.getText().toString());
            currentDraft.setAddress(etAddress.getText().toString());
            currentDraft.setRemarks(etRemarks.getText().toString());
            currentDraft.setUserId(isEditMode ? editRequest.getUserId() : "");
            navigateToMap(currentDraft);
        });

        btnPhoto.setOnClickListener(v -> {
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

        String[] categories = {"Plastic", "Metal", "Paper", "Glass", "Others"};
        boolean[] checkedItems = {false, false, false, false};

        etCategory.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Categories")
                    .setMultiChoiceItems(categories, checkedItems, (dialog, which, isChecked) -> {
                        checkedItems[which] = isChecked;
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < categories.length; i++) {
                            if (checkedItems[i]) {
                                if (builder.length() > 0) builder.append(", ");
                                builder.append(categories[i]);
                            }
                        }
                        etCategory.setText(builder.toString());
                    })
                    .show();
        });

        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Pick Up Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                etDate.setText(datePicker.getHeaderText());
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        btnSubmit.setOnClickListener(v -> {
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String remarks = etRemarks.getText().toString().trim();

            if (category.isEmpty() || date.isEmpty() || contact.isEmpty() || address.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedCenterIds.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one target center", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");

            if (isEditMode) {
                editRequest.setCategory(category);
                editRequest.setDate(date);
                editRequest.setContact(contact);
                editRequest.setAddress(address);
                editRequest.setRemarks(remarks);
                
                applySelectionToRequest(editRequest);
                
                if (selectedImageUri != null) {
                    savedImageFileName = ImageManager.saveImageToInternalStorage(requireContext(), selectedImageUri);
                    editRequest.setPhotoUrl(savedImageFileName);
                }
                firestoreManager.updateRecycleRequest(editRequest, new FirestoreManager.OnTaskCompleteListener() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Request Updated!", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new RecycleStatus())
                                    .commit();
                        }
                    }
                    @Override
                    public void onFailure(String error) {
                        if (isAdded()) Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                RecycleRequest request = new RecycleRequest(
                        currentUid,
                        category, date, contact, remarks
                );
                request.setAddress(address);
                applySelectionToRequest(request);
                submitToFirestore(request);
            }
        });

        return view;
    }

    private void applySelectionToRequest(RecycleRequest request) {
        request.setTargetCenterIds(new ArrayList<>(selectedCenterIds));
        if (selectedCenterIds.size() == 1) {
            request.setCenterId(selectedCenterIds.get(0));
            request.setCenterName(selectedCenterNames.get(0));
        } else {
            request.setCenterId("MULTIPLE");
            request.setCenterName("Multiple Centers (" + selectedCenterIds.size() + ")");
        }
    }

    private void updateCenterDisplayText() {
        if (selectedCenterNames.isEmpty()) {
            etCenter.setText("");
        } else if (selectedCenterNames.size() == 1) {
            etCenter.setText(selectedCenterNames.get(0));
        } else {
            etCenter.setText("Multiple Centers (" + selectedCenterNames.size() + ")");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putParcelable("cameraImageUri", cameraImageUri);
        }
        if (selectedImageUri != null) {
            outState.putParcelable("selectedImageUri", selectedImageUri);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) return null;
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}
