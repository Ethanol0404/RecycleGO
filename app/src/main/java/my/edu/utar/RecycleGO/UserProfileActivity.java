package my.edu.utar.RecycleGO;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleCenter;
import my.edu.utar.RecycleGO.database.UserRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends Fragment {

    EditText etUsername, etEmail, etPhone, etRecycleCenter;
    ImageButton btnClose;
    ImageView ivProfilePicLarge;
    Button btnUpdate, btnLogout;
    FirestoreManager firestoreManager;
    String userEmail;
    String userRole = "";
    String currentUid;
    String profilePicUrl = "";
    Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfilePicLarge.setImageURI(uri);
                    profilePicUrl = uri.toString();
                }
            }
    );

    public UserProfileActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide header when in profile fragment
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(false);
        }

        firestoreManager = new FirestoreManager();

        etUsername = view.findViewById(R.id.etProfileUsername);
        etEmail = view.findViewById(R.id.etProfileEmail);
        etPhone = view.findViewById(R.id.etProfilePhone);
        etRecycleCenter = view.findViewById(R.id.etProfileRecycleCenter);
        ivProfilePicLarge = view.findViewById(R.id.ivProfilePicLarge);
        btnUpdate = view.findViewById(R.id.btnUpdateProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnClose = view.findViewById(R.id.btnClose2);

        applyCustomTheme(view);

        btnClose.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        ivProfilePicLarge.setOnClickListener(v -> showProfilePicOptions());

        // Make Email and Recycle Center read-only
        etEmail.setEnabled(false);
        etRecycleCenter.setEnabled(false);
        etRecycleCenter.setFocusable(false);
        etRecycleCenter.setVisibility(View.GONE); // Hidden by default until role is confirmed

        // Retrieve logged in user's info from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInEmail", "");
        currentUid = sharedPreferences.getString("loggedInUid", "");

        if (!userEmail.isEmpty()) {
            loadUserData();
        }

        btnUpdate.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("username", username);
            updates.put("phone", phone);
            updates.put("profilePicUrl", profilePicUrl);
            
            if (currentUid.isEmpty()) {
                Toast.makeText(getContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            firestoreManager.updateUser(currentUid, updates, new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Update Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnLogout.setOnClickListener(v -> {
            // Clear SharedPreferences on logout
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String themeColorCode = prefs.getString("theme_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int themeColor = Color.parseColor(themeColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        // Root Background
        view.setBackgroundColor(themeColor);

        // Header Background
        View headerBg = view.findViewById(R.id.profileHeaderBg);
        if (headerBg != null) {
            headerBg.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
        }

        // Title and Close button
        TextView title = view.findViewById(R.id.tvProfileTitle);
        if (title != null) title.setTextColor(Color.WHITE);
        if (btnClose != null) btnClose.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        // Update Info Button
        if (btnUpdate != null) {
            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            btnUpdate.setTextColor(Color.WHITE);
        }

        // EditText Text, Hint and Frame (Stroke) Colors
        updateEditTextTheme(etUsername, bottomColor);
        updateEditTextTheme(etEmail, bottomColor);
        updateEditTextTheme(etPhone, bottomColor);
        updateEditTextTheme(etRecycleCenter, bottomColor);
    }

    private void updateEditTextTheme(EditText et, int color) {
        if (et != null) {
            et.setTextColor(color);
            et.setHintTextColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)));
            
            // Tint Drawables (User icon, Email icon, etc.)
            if (et.getCompoundDrawablesRelative()[0] != null) {
                et.getCompoundDrawablesRelative()[0].setTint(color);
            }

            // Dynamically change the stroke color of the background drawable
            if (et.getBackground() instanceof GradientDrawable) {
                GradientDrawable shape = (GradientDrawable) et.getBackground().mutate();
                shape.setStroke(4, color); // Increased width for better visibility
            }
        }
    }

    private void showProfilePicOptions() {
        String[] options = {"Choose from Gallery", "Insert Image URL"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        showUrlInputDialog();
                    }
                })
                .show();
    }

    private void showUrlInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Image URL");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://example.com/photo.jpg");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                profilePicUrl = url;
                selectedImageUri = null;
                Glide.with(this).load(url).into(ivProfilePicLarge);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void loadUserData() {
        firestoreManager.getUserByEmail(userEmail, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null && isAdded()) {
                    etUsername.setText(user.getUsername());
                    etEmail.setText(user.getEmail());
                    etPhone.setText(user.getPhone());
                    
                    userRole = user.getRole();

                    // Show Recycle Center only for Admin
                    if ("Admin".equalsIgnoreCase(userRole)) {
                        etRecycleCenter.setVisibility(View.VISIBLE);
                        
                        // Display joined centers by name, each on a new line
                        List<String> joinedCenterIds = user.getJoinedCenters();
                        if (joinedCenterIds != null && !joinedCenterIds.isEmpty()) {
                            firestoreManager.getCentersByIds(joinedCenterIds, new FirestoreManager.OnListFetchListener<RecycleCenter>() {
                                @Override
                                public void onListFetched(List<RecycleCenter> list) {
                                    if (isAdded()) {
                                        List<String> names = new ArrayList<>();
                                        for (RecycleCenter center : list) {
                                            names.add(center.name);
                                        }
                                        if (names.isEmpty()) {
                                            etRecycleCenter.setText("No Centers Found");
                                        } else {
                                            etRecycleCenter.setText(TextUtils.join("\n", names));
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    if (isAdded()) {
                                        etRecycleCenter.setText("Error loading centers");
                                    }
                                }
                            });
                        } else {
                            etRecycleCenter.setText("No Centers Joined");
                        }
                    } else {
                        etRecycleCenter.setVisibility(View.GONE);
                    }

                    profilePicUrl = user.getProfilePicUrl();
                    
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(UserProfileActivity.this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.useravatar)
                                .into(ivProfilePicLarge);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading data: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure header is restored when leaving this fragment
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
    }
}
