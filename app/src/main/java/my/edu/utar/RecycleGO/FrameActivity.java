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
        setContentView(R.layout.fragment_frame);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        firestoreManager = new FirestoreManager();
        header = findViewById(R.id.header);
        tvUsername = findViewById(R.id.username);
        tvRole = findViewById(R.id.role);

        // Fetch and display user data
        loadUserData();

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, new Main());
            fragmentTransaction.commit();
        }

        ImageView profileImage = findViewById(R.id.profileImage);
        ImageButton btnHome = findViewById(R.id.home);
        ImageButton btnCalendar = findViewById(R.id.btn_calendar);
        ImageButton btnRecycle = findViewById(R.id.btn_recycle);
        ImageButton btnLocation = findViewById(R.id.btn_location);
        ImageButton btnGroup = findViewById(R.id.btn_group);

        if (profileImage != null) {
            profileImage.setOnClickListener(v -> replaceFragment(new UserProfileActivity()));
        }

        if (btnHome != null) btnHome.setOnClickListener(v -> replaceFragment(new Main()));

        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> {
                replaceFragment(new CampaignsActivity());
            });
        }

        if (btnRecycle != null) {
            btnRecycle.setOnClickListener(v -> {
                replaceFragment(new RecycleStatus());
            });
        }

        if (btnLocation != null) {
            btnLocation.setOnClickListener(v -> {
                replaceFragment(new Map());
            });
        }

        if (btnGroup != null) {
            btnGroup.setOnClickListener(v -> {
                replaceFragment(new Community());
            });
        }
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("loggedInEmail", "");

        if (!email.isEmpty()) {
            firestoreManager.getUserByEmail(email, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        if (tvRole != null) tvRole.setText(user.getRole().toUpperCase());
                    }
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(FrameActivity.this, "Error loading header: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void replaceFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHeaderVisible(boolean visible) {
        if (header != null) {
            header.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
