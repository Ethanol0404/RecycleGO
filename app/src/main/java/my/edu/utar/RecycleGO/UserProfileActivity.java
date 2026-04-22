package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPhone;
    ImageButton btnClose;
    Button btnUpdate, btnLogout;
    FirestoreManager firestoreManager;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firestoreManager = new FirestoreManager();

        etUsername = findViewById(R.id.etProfileUsername);
        etEmail = findViewById(R.id.etProfileEmail);
        etPhone = findViewById(R.id.etProfilePhone);
        btnUpdate = findViewById(R.id.btnUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnClose = findViewById(R.id.btnClose2);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserProfileActivity.this, Main.class); // Or whatever the back activity is
                startActivity(intent);
                finish();
            }
        });


        // Make Email read-only as it's the unique identifier
        etEmail.setEnabled(false);

        // Retrieve logged in user's email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("loggedInEmail", "");

        if (!userEmail.isEmpty()) {
            loadUserData();
        }

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String phone = etPhone.getText().toString();

                if (username.isEmpty()) {
                    Toast.makeText(UserProfileActivity.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("username", username);
                updates.put("phone", phone);

                // Need a UID for update, or update by email? 
                // Currently FirestoreManager uses UID. Let's get UID from SharedPreferences
                String uid = sharedPreferences.getString("loggedInUid", "");
                if (uid.isEmpty()) {
                    Toast.makeText(UserProfileActivity.this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                firestoreManager.updateUser(uid, updates, new FirestoreManager.OnTaskCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(UserProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(UserProfileActivity.this, "Update Failed: " + error, Toast.LENGTH_SHORT).show();
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

                Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadUserData() {
        firestoreManager.getUserByEmail(userEmail, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null) {
                    etUsername.setText(user.getUsername());
                    etEmail.setText(user.getEmail());
                    etPhone.setText(user.getPhone());
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(UserProfileActivity.this, "Error loading data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}