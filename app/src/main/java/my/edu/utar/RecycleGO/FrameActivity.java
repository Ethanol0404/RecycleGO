package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.NotificationModel;
import my.edu.utar.RecycleGO.database.UserRecord;

public class FrameActivity extends AppCompatActivity {
    private RelativeLayout header;
    private LinearLayout bottomBar;
    private ImageButton homeButton, btnMenu, btnNotification;
    private View notificationDot;
    private TextView tvUsername, tvLevel;
    private ImageView profileImage;
    private DrawerLayout drawerLayout;
    private FirestoreManager firestoreManager;
    private ListenerRegistration userListener;
    private ListenerRegistration notificationListener;
    private View mainContent;
    private com.google.android.material.navigation.NavigationView navView;
    private LinearLayout layoutThemeOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_frame);

        firestoreManager = new FirestoreManager();
        header = findViewById(R.id.header);
        bottomBar = findViewById(R.id.bottomBar);
        homeButton = findViewById(R.id.home);
        tvUsername = findViewById(R.id.username);
        tvLevel = findViewById(R.id.role);
        profileImage = findViewById(R.id.profileImage);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.menu);
        btnNotification = findViewById(R.id.notification);
        notificationDot = findViewById(R.id.notification_dot);
        mainContent = findViewById(R.id.main_content);
        navView = findViewById(R.id.nav_view);
        layoutThemeOptions = findViewById(R.id.layout_theme_options);

        applySavedTheme();

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        
        View btnChangeTheme = findViewById(R.id.menu_change_theme);
        if (btnChangeTheme != null) {
            btnChangeTheme.setOnClickListener(v -> {
                if (layoutThemeOptions != null) {
                    int visibility = layoutThemeOptions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                    layoutThemeOptions.setVisibility(visibility);
                }
            });
        }

        setupThemeOptions();

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String uid = sharedPreferences.getString("loggedInUid", "");
                if (!uid.isEmpty()) {
                    firestoreManager.markNotificationsAsRead(uid);
                }
                replaceFragment(new NotificationsFragment());
            });
        }

        String flow = getIntent().getStringExtra("flow");
        if ("SIGNUP_TO_MAP".equals(flow)) {
            setHeaderVisible(false);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            if (homeButton != null) homeButton.setVisibility(View.GONE);
        }

        loadUserData();

        if (savedInstanceState == null) {
            String target = getIntent().getStringExtra("targetFragment");
            Fragment fragment;
            if ("MAP".equals(target)) {
                fragment = new Map();
                Bundle args = new Bundle();
                args.putString("flow", flow);
                fragment.setArguments(args);
            } else {
                fragment = new HomeFragment();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(v -> replaceFragment(new HomeFragment()));
        }

        View btnCalendar = findViewById(R.id.btn_calendar);
        if (btnCalendar != null) btnCalendar.setOnClickListener(v -> replaceFragment(new CampaignsActivity()));

        View btnRecycle = findViewById(R.id.btn_recycle);
        if (btnRecycle != null) btnRecycle.setOnClickListener(v -> replaceFragment(new RecycleStatus()));

        View btnLocation = findViewById(R.id.btn_location);
        if (btnLocation != null) btnLocation.setOnClickListener(v -> replaceFragment(new Map()));

        View btnGroup = findViewById(R.id.btn_group);
        if (btnGroup != null) btnGroup.setOnClickListener(v -> replaceFragment(new Community()));
        
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> replaceFragment(new UserProfileActivity()));
        }
    }

    private void setupThemeOptions() {
        String[] bgColors = {"#D1E29B", "#A2D2FF", "#FFCCAC", "#eeddff"};
        String[] bottomColors = {"#265200", "#0B0052", "#522700", "#3a0052"};
        String[] homeBtnColors = {"#8ac29d", "#1dddff", "#ff971d", "#d680ff"};
        String[] headerColors = {"#9cc173", "#7dc9e1", "#ffae73", "#d2acf9"};

        View green = findViewById(R.id.theme_green);
        View blue = findViewById(R.id.theme_blue);
        View orange = findViewById(R.id.theme_orange);
        View purple = findViewById(R.id.theme_purple);

        if (green != null) green.setOnClickListener(v -> onThemeSelected(0, bgColors, bottomColors, homeBtnColors, headerColors));
        if (blue != null) blue.setOnClickListener(v -> onThemeSelected(1, bgColors, bottomColors, homeBtnColors, headerColors));
        if (orange != null) orange.setOnClickListener(v -> onThemeSelected(2, bgColors, bottomColors, homeBtnColors, headerColors));
        if (purple != null) purple.setOnClickListener(v -> onThemeSelected(3, bgColors, bottomColors, homeBtnColors, headerColors));
    }

    private void onThemeSelected(int index, String[] bgs, String[] bottoms, String[] homeBtns, String[] headers) {
        saveTheme(bgs[index], bottoms[index], headers[index], homeBtns[index], headers[index]);
        applyTheme(bgs[index], bottoms[index], headers[index], homeBtns[index], headers[index]);
        drawerLayout.closeDrawer(GravityCompat.START);
        recreate();
    }

    private void saveTheme(String bgColor, String bottomColor, String accentColor, String homeBtnColor, String headerColor) {
        SharedPreferences.Editor editor = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit();
        editor.putString("theme_color", bgColor);
        editor.putString("bottom_color", bottomColor);
        editor.putString("accent_color", accentColor);
        editor.putString("home_btn_color", homeBtnColor);
        editor.putString("header_color", headerColor);
        editor.apply();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String bgColor = prefs.getString("theme_color", "#D1E29B");
        String bottomColor = prefs.getString("bottom_color", "#265200");
        String accentColor = prefs.getString("accent_color", "#def9ac");
        String homeBtnColor = prefs.getString("home_btn_color", "#8ac29d");
        String headerColor = prefs.getString("header_color", "#def9ac");
        applyTheme(bgColor, bottomColor, accentColor, homeBtnColor, headerColor);
    }

    private void applyTheme(String bgColorCode, String bottomColorCode, String accentColorCode, String homeBtnColorCode, String headerColorCode) {
        int bgColor = Color.parseColor(bgColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);
        int accentColor = Color.parseColor(accentColorCode);
        int homeBtnColor = Color.parseColor(homeBtnColorCode);
        int headerColor = Color.parseColor(headerColorCode);

        if (mainContent != null) mainContent.setBackgroundColor(bgColor);
        if (header != null) header.setBackgroundColor(headerColor);
        if (bottomBar != null) bottomBar.setBackgroundColor(bottomColor);
        if (navView != null) {
            navView.setBackgroundColor(bgColor);
            updateNavViewTheme(navView, bottomColor);
        }

        if (tvLevel != null) tvLevel.setTextColor(bottomColor);
        if (tvUsername != null) tvUsername.setTextColor(bottomColor);
        if (btnMenu != null) btnMenu.setColorFilter(bottomColor);
        if (btnNotification != null) btnNotification.setColorFilter(bottomColor);

        if (homeButton != null) {
            homeButton.setColorFilter(bottomColor);
            homeButton.getBackground().mutate().setColorFilter(homeBtnColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void updateNavViewTheme(ViewGroup parent, int textColor) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateNavViewTheme((ViewGroup) child, textColor);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(textColor);
                TextView tv = (TextView) child;
                android.graphics.drawable.Drawable[] drawables = tv.getCompoundDrawablesRelative();
                for (android.graphics.drawable.Drawable d : drawables) {
                    if (d != null) d.setColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
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
                    if (user != null && !isFinishing()) {
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        String level = "BRONZE";
                        if (user.getPoints() > 5000) level = "GOLD";
                        else if (user.getPoints() > 2000) level = "SILVER";
                        if (tvLevel != null) tvLevel.setText(level);
                        if (profileImage != null) {
                            String picUrl = user.getProfilePicUrl();
                            if (picUrl != null && !picUrl.isEmpty()) {
                                Glide.with(FrameActivity.this).load(picUrl).placeholder(R.drawable.useravatar).into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.useravatar);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(String error) { }
            });

            // Real-time notification badge listener
            if (notificationListener != null) notificationListener.remove();
            notificationListener = firestoreManager.listenToNotifications(uid, new FirestoreManager.OnListFetchListener<NotificationModel>() {
                @Override
                public void onListFetched(List<NotificationModel> list) {
                    boolean hasUnread = false;
                    for (NotificationModel notif : list) {
                        if (!notif.isRead()) {
                            hasUnread = true;
                            break;
                        }
                    }
                    if (notificationDot != null) {
                        notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onFailure(String error) {}
            });
        }
    }

    private void replaceFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHeaderVisible(boolean visible) {
        if (header != null) header.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (notificationListener != null) notificationListener.remove();
    }
}
