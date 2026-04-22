package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import my.edu.utar.RecycleGO.database.DatabaseHelper;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    EditText etUsername, etEmail, etPassword, etRetypePassword;
    Spinner spinnerSignUpRole;
    Button btnSignUp;
    TextView tvSignIn;
    DatabaseHelper db;

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

        db = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRetypePassword = findViewById(R.id.etRetypePassword);
        spinnerSignUpRole = findViewById(R.id.spinnerSignUpRole);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSignUpRole.setAdapter(adapter);

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
                Log.d(TAG, "Sign Up Button Clicked");
                registerUser();
            }
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String retypePassword = etRetypePassword.getText().toString().trim();
        String role = spinnerSignUpRole.getSelectedItem().toString();

        Log.d(TAG, "Attempting to register: " + email + " as " + role);

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(retypePassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user already exists locally
        if (db.getUserByEmail(email).getCount() > 0) {
            Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
        } else {
            boolean success = db.addUser(username, email, password, role);
            if (success) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Registration Failed in Database", Toast.LENGTH_SHORT).show();
            }
        }
    }
}