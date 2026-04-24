package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");

        loadPointHistory();
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
