package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends Fragment {

    EditText etUsername, etEmail, etPhone, etRecycleCenter;
    ImageButton btnClose;
    Button btnUpdate, btnLogout;
    FirestoreManager firestoreManager;
    String userEmail;
    String userRole = "";

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
        btnUpdate = view.findViewById(R.id.btnUpdateProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnClose = view.findViewById(R.id.btnClose2);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        // Make Email read-only as it's the unique identifier
        etEmail.setEnabled(false);

        // Retrieve logged in user's info from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInEmail", "");

        if (!userEmail.isEmpty()) {
            loadUserData();
        }

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String recycleCenter = etRecycleCenter.getText().toString().trim();

                if (username.isEmpty()) {
                    Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("username", username);
                updates.put("phone", phone);
                
                // Only update recycleCenter if the user is an Admin
                if ("Admin".equalsIgnoreCase(userRole)) {
                    updates.put("recycleCenter", recycleCenter);
                }

                String uid = sharedPreferences.getString("loggedInUid", "");
                if (uid.isEmpty()) {
                    Toast.makeText(getContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                firestoreManager.updateUser(uid, updates, new FirestoreManager.OnTaskCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Update Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear SharedPreferences on logout
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });
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
                    
                    // Role-based UI visibility
                    if ("Admin".equalsIgnoreCase(userRole)) {
                        etRecycleCenter.setVisibility(View.VISIBLE);
                        etRecycleCenter.setText(user.getRecycleCenter());
                    } else {
                        etRecycleCenter.setVisibility(View.GONE);
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
