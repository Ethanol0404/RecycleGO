package my.edu.utar.groupasgn;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FrameActivity extends AppCompatActivity {
    private RelativeLayout header;
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

        //header view
        header = findViewById(R.id.header);

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(new Main());
        }

        // Initialize buttons
        ImageButton btnHome = findViewById(R.id.home);
        ImageButton btnCalendar = findViewById(R.id.btn_calendar);
        ImageButton btnRecycle = findViewById(R.id.btn_recycle);
        ImageButton btnLocation = findViewById(R.id.btn_location);
        ImageButton btnGroup = findViewById(R.id.btn_group);

        // Click listeners
        btnHome.setOnClickListener(v -> replaceFragment(new Main()));
        
        btnCalendar.setOnClickListener(v -> {
            // replaceFragment(new CalendarFragment());
        });

        btnRecycle.setOnClickListener(v -> {
            // replaceFragment(new RecycleFragment());
        });

        btnLocation.setOnClickListener(v -> {
              replaceFragment(new Map());
        });

        btnGroup.setOnClickListener(v -> {
            // replaceFragment(new GroupFragment());
        });
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