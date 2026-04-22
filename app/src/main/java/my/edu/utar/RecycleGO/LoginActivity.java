package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin, btnCreateAccount;
    EditText etLoginEmail;
    EditText etLoginPassword;
    Spinner spinnerRole;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        spinnerRole = findViewById(R.id.spinnerRole);

        // Setup Spinner
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Login button listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Create account button listener
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();
        String selectedRole = spinnerRole.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Authenticate with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 2. Auth successful, now check role in Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        checkUserRole(userId, selectedRole);
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(String userId, String selectedRole) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userRole = documentSnapshot.getString("role");
                        if (selectedRole.equals(userRole)) {
                            // Role matches
                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("loggedInEmail", mAuth.getCurrentUser().getEmail());
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Login Successful as " + userRole, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, FrameActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Role mismatch
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "Access Denied: You are not registered as " + selectedRole, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    Toast.makeText(LoginActivity.this, "Error checking role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}