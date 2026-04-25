package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    EditText etUsername, etEmail, etPassword, etRetypePassword, etRecycleCenter;
    Spinner spinnerSignUpRole;
    Button btnSignUp;
    TextView tvSignIn;
    FirestoreManager firestoreManager;
    FirebaseAuth mAuth;
    String selectedCenterId = "";

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
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRetypePassword = findViewById(R.id.etRetypePassword);
        etRecycleCenter = findViewById(R.id.etRecycleCenter);
        spinnerSignUpRole = findViewById(R.id.spinnerSignUpRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Setup Spinner for Role selection
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSignUpRole.setAdapter(adapter);

        // Show/Hide Recycle Center field based on role
        spinnerSignUpRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = parent.getItemAtPosition(position).toString();
                if (selectedRole.equals("Admin")) {
                    etRecycleCenter.setVisibility(View.VISIBLE);
                } else {
                    etRecycleCenter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etRecycleCenter.setFocusable(false);
        etRecycleCenter.setClickable(true);
        etRecycleCenter.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, FrameActivity.class);
            intent.putExtra("targetFragment", "MAP");
            intent.putExtra("flow", "SIGNUP_TO_MAP");
            startActivityForResult(intent, 100);
        });

        // Sign In text listener
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String retypePassword = etRetypePassword.getText().toString().trim();
            String role = spinnerSignUpRole.getSelectedItem().toString();
            String recycleCenter = etRecycleCenter.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role.equals("Admin") && recycleCenter.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please enter your Recycle Center Name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(retypePassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register with Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        UserRecord newUser = new UserRecord(uid, username, email, password, role);
                        newUser.setPoints(0); // Start with 0
                        if (role.equals("Admin")) {
                            if (!selectedCenterId.isEmpty()) {
                                newUser.getJoinedCenters().add(selectedCenterId);
                            }
                            newUser.setRecycleCenter(recycleCenter);
                        }

                        // Save additional data to Firestore
                        firestoreManager.saveUser(newUser, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(SignUpActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(SignUpActivity.this, "Firestore Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String errorMessage = "Auth Failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                            if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = "Weak password. Use at least 6 characters.";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email format.";
                            } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Email already in use.";
                            } else if (task.getException() instanceof FirebaseAuthException) {
                                String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                                if (errorCode.equals("ERROR_OPERATION_NOT_ALLOWED")) {
                                    errorMessage = "Email/Password sign-in is not enabled in Firebase Console.";
                                }
                            }
                            Log.e(TAG, "Auth Error: ", task.getException());
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String selectedCenter = data.getStringExtra("selectedCenter");
            selectedCenterId = data.getStringExtra("selectedCenterId");
            if (selectedCenter != null) {
                etRecycleCenter.setText(selectedCenter);
            }
        }
    }
}
