package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class FrameActivity extends AppCompatActivity {
    private RelativeLayout header;
    private TextView tvUsername, tvRole;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_frame);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestoreManager = new FirestoreManager();

        // header view
        header = findViewById(R.id.header);
        tvUsername = findViewById(R.id.username);
        tvRole = findViewById(R.id.role);

        // Fetch user data
        loadUserData();

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(new Main());
        }

        ImageView profileImage = findViewById(R.id.profileImage);

        // Initialize buttons
        ImageButton btnHome = findViewById(R.id.home);
        ImageButton btnCalendar = findViewById(R.id.btn_calendar);
        ImageButton btnRecycle = findViewById(R.id.btn_recycle);
        ImageButton btnLocation = findViewById(R.id.btn_location);
        ImageButton btnGroup = findViewById(R.id.btn_group);

        profileImage.setOnClickListener(v -> {
            replaceFragment(new UserProfileActivity());
        });

        // Click listeners
        btnHome.setOnClickListener(v -> replaceFragment(new Main()));

        btnCalendar.setOnClickListener(v -> {
            replaceFragment(new CampaignsActivity());
        });

        btnRecycle.setOnClickListener(v -> {
            replaceFragment(new RecycleStatus());
        });

        btnLocation.setOnClickListener(v -> {
              replaceFragment(new Map());
        });

        btnGroup.setOnClickListener(v -> {
            replaceFragment(new Community());
        });
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("loggedInEmail", "");

        if (!email.isEmpty()) {
            firestoreManager.getUserByEmail(email, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        tvUsername.setText(user.getUsername());
                        tvRole.setText(user.getRole().toUpperCase());
                    }
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(FrameActivity.this, "Error loading header data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // header visibility
    public void setHeaderVisible(boolean visible) {
        if (header != null) {
            header.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
