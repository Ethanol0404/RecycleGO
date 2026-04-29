package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleCenter;
import my.edu.utar.RecycleGO.database.RecycleRequest;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class PickUpActivity extends Fragment {

    private TextInputEditText etCategory, etDate, etContact, etAddress, etRemarks, etCenter;
    private ImageButton btnPhoto;
    private TextView tvPhotoStatus;
    private Button btnSubmit;

    private String flow = "STATUS_TO_FORM";
    private ArrayList<String> selectedCenterIds = new ArrayList<>();
    private ArrayList<String> selectedCenterNames = new ArrayList<>();
    
    private FirestoreManager firestoreManager;
    private RecycleRequest editRequest = null;
    private boolean isEditMode = false;
    
    private Uri cameraImageUri;
    private Uri selectedImageUri;
    private String manualImageUrl = null;
    private String savedImageFileName = null;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    manualImageUrl = null;
                    updatePhotoPreview(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    manualImageUrl = null;
                    updatePhotoPreview(cameraImageUri);
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

    private void updatePhotoPreview(Object source) {
        if (isAdded() && btnPhoto != null) {
            Glide.with(this)
                    .load(source)
                    .centerCrop()
                    .into(btnPhoto);
            if (tvPhotoStatus != null) {
                tvPhotoStatus.setVisibility(View.GONE);
            }
        }
    }

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
                
                if (isEditMode && selectedCenterIds.isEmpty()) {
                    if (editRequest.getTargetCenterIds() != null && !editRequest.getTargetCenterIds().isEmpty()) {
                        selectedCenterIds = new ArrayList<>(editRequest.getTargetCenterIds());
                        selectedCenterNames = new ArrayList<>();
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
        
        args.putStringArrayList("selectedCenterIds", selectedCenterIds);
        
        nextFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit();
    }

    private void submitToFirestore(RecycleRequest request) {
        request.setStatus("Requesting");

        if (selectedImageUri != null) {
            savedImageFileName = ImageManager.saveImageToInternalStorage(requireContext(), selectedImageUri);
            request.setPhotoUrl(savedImageFileName);
        } else if (manualImageUrl != null) {
            request.setPhotoUrl(manualImageUrl);
        }

        firestoreManager.submitRequest(request, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Request Submitted!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new RecycleStatus())
                            .commit();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
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
        tvPhotoStatus = view.findViewById(R.id.tvPhotoStatus);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        etContact = view.findViewById(R.id.etContact);
        etAddress = view.findViewById(R.id.etAddress);
        etRemarks = view.findViewById(R.id.etRemarks);
        etCenter = view.findViewById(R.id.etCenter);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        if (selectedImageUri != null) {
            updatePhotoPreview(selectedImageUri);
        } else if (isEditMode && editRequest.getPhotoUrl() != null) {
            String url = editRequest.getPhotoUrl();
            if (url.startsWith("http")) {
                updatePhotoPreview(url);
            } else {
                File file = new File(requireContext().getFilesDir(), url);
                if (file.exists()) {
                    updatePhotoPreview(file);
                }
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
            String[] options = {"Take Photo", "Choose from Gallery", "Insert Image URL"};
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
                        } else if (which == 1) {
                            getContent.launch("image/*");
                        } else {
                            showUrlInputDialog();
                        }
                    }).show();
        });

        etCategory.setOnClickListener(v -> {
            if (selectedCenterIds.isEmpty()) {
                Toast.makeText(getContext(), "Please select a target center first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fetch all selected centers to find common services
            firestoreManager.getCentersByIds(selectedCenterIds, new FirestoreManager.OnListFetchListener<RecycleCenter>() {
                @Override
                public void onListFetched(List<RecycleCenter> centers) {
                    if (centers == null || centers.isEmpty()) {
                        Toast.makeText(getContext(), "Could not load center information", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Set<String> commonServices = null;

                    for (RecycleCenter center : centers) {
                        if (center.supportedServices == null || center.supportedServices.isEmpty()) {
                            commonServices = new HashSet<>();
                            break;
                        }

                        String[] currentServices = center.supportedServices.split(",\\s*");
                        Set<String> centerServices = new HashSet<>(Arrays.asList(currentServices));

                        if (commonServices == null) {
                            commonServices = centerServices;
                        } else {
                            commonServices.retainAll(centerServices);
                        }
                    }

                    if (commonServices == null || commonServices.isEmpty()) {
                        Toast.makeText(getContext(), "No common services supported by all selected centers", Toast.LENGTH_LONG).show();
                        return;
                    }

                    final String[] servicesArray = commonServices.toArray(new String[0]);
                    Arrays.sort(servicesArray);
                    boolean[] checkedItems = new boolean[servicesArray.length];
                    
                    String currentCategory = etCategory.getText().toString();
                    for(int i=0; i<servicesArray.length; i++) {
                        if(currentCategory.contains(servicesArray[i])) checkedItems[i] = true;
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Select Service")
                            .setMultiChoiceItems(servicesArray, checkedItems, (dialog, which, isChecked) -> {
                                checkedItems[which] = isChecked;
                            })
                            .setPositiveButton("OK", (dialog, which) -> {
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < servicesArray.length; i++) {
                                    if (checkedItems[i]) {
                                        if (builder.length() > 0) builder.append(", ");
                                        builder.append(servicesArray[i]);
                                    }
                                }
                                etCategory.setText(builder.toString());
                            })
                            .show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
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
                } else if (manualImageUrl != null) {
                    editRequest.setPhotoUrl(manualImageUrl);
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

        applyCustomTheme(view);

        return view;
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String headerColorCode = prefs.getString("header_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        
        int headerColor = Color.parseColor(headerColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        // Apply header_color to background
        view.setBackgroundColor(headerColor);

        if (btnSubmit != null) {
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
        }
        
        // Update title color
        TextView title = view.findViewById(R.id.tv_request_title);
        if (title != null) title.setTextColor(bottomColor);

        // Also update all labels to bottom color as requested previously for other screens
        updateTextColorsRecursively((ViewGroup) view, bottomColor);
    }

    private void updateTextColorsRecursively(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateTextColorsRecursively((ViewGroup) child, color);
            } else if (child instanceof TextView && !(child instanceof EditText)) {
                ((TextView) child).setTextColor(color);
            }
        }
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
        if (btnOk != null) btnOk.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
        if (btnCancel != null) btnCancel.setTextColor(bottomColor);

        btnOk.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                manualImageUrl = url;
                selectedImageUri = null;
                updatePhotoPreview(url);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
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
