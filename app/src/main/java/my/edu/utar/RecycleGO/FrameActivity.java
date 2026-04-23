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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FrameActivity extends AppCompatActivity {
    private RelativeLayout header;
    private TextView tvUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fix: Removed EdgeToEdge to prevent black screen issue on some emulators
        // If you want EdgeToEdge, it requires precise window insets handling
        // EdgeToEdge.enable(this);

        setContentView(R.layout.fragment_frame);

        // Fix: Added null check for the root view
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        header = findViewById(R.id.header);
        tvUsername = findViewById(R.id.username);

        // Load actual username
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("loggedInUsername", "User");
        if (tvUsername != null) {
            tvUsername.setText(username);
        }

        if (savedInstanceState == null) {
            replaceFragment(new Main());
        }

        ImageView profileImage = findViewById(R.id.profileImage);
        ImageButton btnHome = findViewById(R.id.home);
        ImageButton btnCalendar = findViewById(R.id.btn_calendar);
        ImageButton btnRecycle = findViewById(R.id.btn_recycle);
        ImageButton btnLocation = findViewById(R.id.btn_location);
        ImageButton btnGroup = findViewById(R.id.btn_group);

        if (profileImage != null) {
            profileImage.setOnClickListener(v -> {
                Intent intent = new Intent(FrameActivity.this, UserProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnHome != null) btnHome.setOnClickListener(v -> replaceFragment(new HomeFragment()));

        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> {
                replaceFragment(new ActivityFragment());
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
                replaceFragment(new AIAssistant());
            });
        }
    }

    private void replaceFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
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