package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.Toast;
import androidx.annotation.Nullable;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleRequest;

public class PickUpActivity extends Fragment {

    // 1. Move variables here (Class Level) so the whole class can "see" them
    private TextInputEditText etCategory, etDate, etContact, etRemarks;
    private com.google.android.material.card.MaterialCardView cardPhoto;
    private ImageButton btnPhoto;
    private Button btnSubmit;

    private String flow = "STATUS_TO_FORM";
    private String selectedCenterId = null;
    private String selectedCenterName = null;
    private FirestoreManager firestoreManager;

    // 2. Define launchers at the top
    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    btnPhoto.setImageURI(uri);
                    btnPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
    );

    private final ActivityResultLauncher<Void> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    btnPhoto.setImageBitmap(bitmap);
                    btnPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
        if (getArguments() != null) {
            flow = getArguments().getString("flow", "STATUS_TO_FORM");
            selectedCenterId = getArguments().getString("centerId");
            selectedCenterName = getArguments().getString("centerName");
        }
    }

    private void navigateToMap(RecycleRequest draft) {
        Fragment nextFragment = new Map();
        Bundle args = new Bundle();
        args.putString("flow", "STATUS_TO_FORM");
        args.putString("category", draft.getCategory());
        args.putString("date", draft.getDate());
        args.putString("contact", draft.getContact());
        args.putString("remarks", draft.getRemarks());
        nextFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit();
    }

    private void submitToFirestore(RecycleRequest request) {
        firestoreManager.submitRequest(request, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Request Submitted Successfully!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RecycleStatus())
                        .commit();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        // 3. Store the inflated view in a variable first!
        View view = inflater.inflate(R.layout.activity_pick_up, container, false);

        // 4. Initialize views using that 'view' variable
        // Make sure these IDs match your XML exactly (I used etCategory for the photo text based on your logic)
        cardPhoto = view.findViewById(R.id.cardPhoto);
        btnPhoto = view.findViewById(R.id.btnPhoto);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        etContact = view.findViewById(R.id.etContact);
        etRemarks = view.findViewById(R.id.etRemarks);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // --- Logic Sections ---

        btnPhoto.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Option")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) takePicture.launch(null);
                        else getContent.launch("image/*");
                    }).show();
        });

        String[] categories = {"Plastic", "Metal", "Paper", "Glass"};
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

        //submit
        btnSubmit.setOnClickListener(v -> {
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String remarks = etRemarks.getText().toString().trim();

            if (category.isEmpty() || date.isEmpty() || contact.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");

            RecycleRequest request = new RecycleRequest(
                    currentUid,
                    category, date, contact, remarks
            );

            if (flow.equals("MAP_TO_FORM")) {
                request.setCenterId(selectedCenterId);
                request.setCenterName(selectedCenterName);
                submitToFirestore(request);
            } else {
                navigateToMap(request);
            }
        });

        return view;

    }
}

