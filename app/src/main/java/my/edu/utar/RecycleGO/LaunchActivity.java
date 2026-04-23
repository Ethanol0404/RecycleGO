package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        ImageView planet = findViewById(R.id.planet);

        // Start the rotation animation
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_infinitely);
        planet.startAnimation(rotate);

        // Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.contains("loggedInEmail");

        // Move to appropriate activity after 4 seconds
        new Handler().postDelayed(() -> {
            if (isLoggedIn) {
                startActivity(new Intent(this, FrameActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 4000);
    }
}