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

import com.google.firebase.firestore.ListenerRegistration;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class FrameActivity extends AppCompatActivity {
    private RelativeLayout header;
    private TextView tvUsername, tvLevel;
    private FirestoreManager firestoreManager;
    private ListenerRegistration userListener;

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
        tvLevel = findViewById(R.id.role); // Keeping ID as 'role' but using for Level or changing in XML

        // Fetch and display user data
        loadUserData();

        if (savedInstanceState == null) {
            String target = getIntent().getStringExtra("targetFragment");
            Fragment fragment;
            if ("MAP".equals(target)) {
                fragment = new Map();
                Bundle args = new Bundle();
                args.putString("flow", getIntent().getStringExtra("flow"));
                fragment.setArguments(args);
            } else {
                fragment = new HomeFragment();
            }
            
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
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

        if (btnHome != null) btnHome.setOnClickListener(v -> replaceFragment(new HomeFragment()));

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
        String uid = sharedPreferences.getString("loggedInUid", "");

        if (!uid.isEmpty()) {
            if (userListener != null) userListener.remove();
            userListener = firestoreManager.listenToUser(uid, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        
                        // Calculate level based on points
                        String level = "BRONZE";
                        if (user.getPoints() > 5000) level = "GOLD";
                        else if (user.getPoints() > 2000) level = "SILVER";
                        
                        if (tvLevel != null) tvLevel.setText(level);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
    }
}
