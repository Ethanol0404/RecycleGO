package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import java.util.UUID;

public class SignUpActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword, etRetypePassword;
    Spinner spinnerSignUpRole;
    Button btnSignUp;
    TextView tvSignIn;
    FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestoreManager = new FirestoreManager();

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRetypePassword = findViewById(R.id.etRetypePassword);
        spinnerSignUpRole = findViewById(R.id.spinnerSignUpRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Setup Spinner for Role selection
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSignUpRole.setAdapter(adapter);

        // Sign In text listener
        tvSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();
                String retypePassword = etRetypePassword.getText().toString();
                String role = spinnerSignUpRole.getSelectedItem().toString();

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(retypePassword)) {
                    Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if user already exists
                firestoreManager.getUserByEmail(email, new FirestoreManager.OnUserFetchListener() {
                    @Override
                    public void onUserFetched(UserRecord existingUser) {
                        if (existingUser != null) {
                            Toast.makeText(SignUpActivity.this, "User with this email already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            // Register user
                            String uid = UUID.randomUUID().toString();
                            UserRecord newUser = new UserRecord(uid, username, email, password, role);

                            firestoreManager.saveUser(newUser, new FirestoreManager.OnTaskCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(SignUpActivity.this, "Registration Successful as " + role, Toast.LENGTH_SHORT).show();
                                    finish(); // Go back to Login
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(SignUpActivity.this, "Registration Failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(SignUpActivity.this, "Error checking user: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}