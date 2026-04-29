package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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

import com.google.firebase.Timestamp;

import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;

public class ActivityFragment extends Fragment {

    private RecyclerView recyclerView;
    private FirestoreManager firestoreManager;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvPointHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firestoreManager = new FirestoreManager();
        SharedPreferences userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = userPrefs.getString("loggedInUid", "");

        applyCustomTheme(view);
        loadPointHistory();
    }

    private void applyCustomTheme(View view) {
        SharedPreferences appPrefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String headerColorCode = appPrefs.getString("header_color", "#D1E29B");
        String bottomColorCode = appPrefs.getString("bottom_color", "#265200");
        
        int headerColor = Color.parseColor(headerColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        // Apply header_color to the root background
        view.setBackgroundColor(headerColor);

        // Find the title TextView and apply bottom_color for consistency
        if (view instanceof ViewGroup) {
            ViewGroup root = (ViewGroup) view;
            for (int i = 0; i < root.getChildCount(); i++) {
                View child = root.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(bottomColor);
                }
            }
        }
    }

    private void loadPointHistory() {
        if (userId == null || userId.isEmpty()) return;

        firestoreManager.getPointHistory(userId, new FirestoreManager.OnPointsHistoryFetchListener() {
            @Override
            public void onHistoryFetched(List<String> activities, List<Long> points, List<Timestamp> timestamps) {
                if (isAdded()) {
                    if (activities.isEmpty()) {
                        Toast.makeText(getContext(), "No history found", Toast.LENGTH_SHORT).show();
                    } else {
                        AdapterPointHistory adapter = new AdapterPointHistory(activities, points, timestamps);
                        recyclerView.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load history: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
