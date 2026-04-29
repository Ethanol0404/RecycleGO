package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.PrivacyPolicyHelper;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    Button btnLogin, btnCreateAccount;
    EditText etLoginEmail, etLoginPassword;
    Spinner spinnerRole;
    CheckBox cbLoginAgree;
    FirestoreManager firestoreManager;
    FirebaseAuth mAuth;

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

        firestoreManager = new FirestoreManager();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        cbLoginAgree = findViewById(R.id.cbLoginAgree);

        // Setup Hyperlink for Privacy Policy
        String text = "I agree to the Privacy Policy";
        SpannableString ss = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                PrivacyPolicyHelper.showPrivacyPolicy(LoginActivity.this, cbLoginAgree);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#2D5D5B")); // Dark Green
                ds.setUnderlineText(true);
                ds.setFakeBoldText(true); // Bold
            }
        };
        ss.setSpan(clickableSpan, 15, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        cbLoginAgree.setText(ss);
        cbLoginAgree.setMovementMethod(LinkMovementMethod.getInstance());

        // Setup Role Spinner
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Login button listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etLoginEmail.getText().toString().trim();
                String password = etLoginPassword.getText().toString().trim();
                String role = spinnerRole.getSelectedItem().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!cbLoginAgree.isChecked()) {
                    Toast.makeText(LoginActivity.this, "Please read and agree to the Privacy Policy", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Authenticate with Firebase Auth first
                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Auth successful, now check role in Firestore
                            firestoreManager.getUser(mAuth.getCurrentUser().getUid(), new FirestoreManager.OnUserFetchListener() {
                                @Override
                                public void onUserFetched(UserRecord user) {
                                    if (user != null && user.getRole().equalsIgnoreCase(role)) {
                                        // Save user info to SharedPreferences
                                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString("loggedInEmail", email);
                                        editor.putString("loggedInUid", user.getUid());
                                        editor.putString("loggedInUsername", user.getUsername());
                                        editor.putString("loggedInRole", user.getRole());
                                        editor.putBoolean("privacyAgreed", true); // Save agreement state
                                        editor.apply();

                                        Toast.makeText(LoginActivity.this, "Login Successful as " + user.getRole(), Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(LoginActivity.this, FrameActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        mAuth.signOut();
                                        Toast.makeText(LoginActivity.this, "Role mismatch or user not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "Firestore Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Log.e(TAG, "Auth Failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Role mismatch or user not found", Toast.LENGTH_LONG).show();
                        }
                    });
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
}
