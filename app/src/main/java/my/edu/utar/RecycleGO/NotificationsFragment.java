package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.NotificationModel;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TextView tvNoNotifications;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private ListenerRegistration notificationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreManager = new FirestoreManager();
        applyCustomTheme(view);

        recyclerView = view.findViewById(R.id.rv_notifications);
        tvNoNotifications = view.findViewById(R.id.tv_no_notifications);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList, this);
        recyclerView.setAdapter(adapter);

        startListeningToNotifications();
        markAllAsRead();
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String themeColorCode = prefs.getString("theme_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int themeColor = Color.parseColor(themeColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        view.setBackgroundColor(themeColor);
        
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar_notifications);
        if (toolbar != null) {
            toolbar.setTitleTextColor(bottomColor);
        }
    }

    private void startListeningToNotifications() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        notificationListener = firestoreManager.listenToNotifications(userId, new FirestoreManager.OnListFetchListener<NotificationModel>() {
            @Override
            public void onListFetched(List<NotificationModel> list) {
                notificationList.clear();
                notificationList.addAll(list);
                adapter.notifyDataSetChanged();
                
                if (notificationList.isEmpty()) {
                    tvNoNotifications.setVisibility(View.VISIBLE);
                } else {
                    tvNoNotifications.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load notifications: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void markAllAsRead() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            firestoreManager.markNotificationsAsRead(userId);
        }
    }

    @Override
    public void onNotificationClick(NotificationModel notification) {
        String type = notification.getType();
        String targetID = notification.getTargetID();

        Log.d("NotificationsFragment", "Notification clicked: Type=" + type + ", TargetID=" + targetID);

        Fragment fragment = null;
        Bundle args = new Bundle();

        if ("Campaign".equalsIgnoreCase(type)) {
            fragment = new CampaignsActivity();
            if (targetID != null) args.putString("targetCampaignID", targetID);
        } else if ("Like".equalsIgnoreCase(type) || "Comment".equalsIgnoreCase(type)) {
            fragment = new Community();
            if (targetID != null) args.putString("targetPostID", targetID);
        } else if ("Request".equalsIgnoreCase(type)) {
            fragment = new RecycleStatus();
            if (targetID != null) args.putString("targetRequestID", targetID);
        } else {
            // Fallback for system notifications or unknown types
            Toast.makeText(getContext(), notification.getTitle(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (fragment != null) {
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Log.e("NotificationsFragment", "Failed to create fragment for type: " + type);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
