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
import android.widget.TextView;
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
    TextView txtPoints, txtTotalRecycled;  // 新增：显示积分和回收次数
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
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // 新增：初始化积分显示
        txtPoints = view.findViewById(R.id.txt_profile_points);
        txtTotalRecycled = view.findViewById(R.id.txt_profile_total_recycled);

        btnClose.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        etEmail.setEnabled(false);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInEmail", "");

        if (!userEmail.isEmpty()) {
            loadUserData();
        }

        btnUpdate.setOnClickListener(v -> {
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
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
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

                    // 新增：显示积分和回收次数
                    txtPoints.setText(user.getPoints() + " pts");
                    txtTotalRecycled.setText(user.getTotalRecycled() + " times");

                    userRole = user.getRole();

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
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
    }
}