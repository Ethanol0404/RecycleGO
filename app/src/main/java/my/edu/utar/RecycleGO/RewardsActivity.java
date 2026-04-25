package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class RewardsActivity extends Fragment {

    private TextView txtPoints, txtTreesSaved;
    private PieChart pieChart;
    private Button btnHistory, btnRedeem;
    
    // Ranking views
    private TextView txtRank1Name, txtRank1Points;
    private TextView txtRank2Name, txtRank2Points;
    private TextView txtRank3Name, txtRank3Points;
    private TextView txtCurrentUserPoints, txtCurrentUserNameRank, txtCurrentRank;

    private FirestoreManager firestoreManager;
    private String currentUserId;
    private ListenerRegistration userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        View view = inflater.inflate(R.layout.activity_reward, container, false);

        // Initialize views
        txtPoints = view.findViewById(R.id.txt_points);
        txtTreesSaved = view.findViewById(R.id.txt_trees_saved);
        pieChart = view.findViewById(R.id.pieChart);
        btnHistory = view.findViewById(R.id.btn_history);
        btnRedeem = view.findViewById(R.id.btn_redeem);

        txtRank1Name = view.findViewById(R.id.txt_rank1_name);
        txtRank1Points = view.findViewById(R.id.txt_rank1_points);
        txtRank2Name = view.findViewById(R.id.txt_rank2_name);
        txtRank2Points = view.findViewById(R.id.txt_rank2_points);
        txtRank3Name = view.findViewById(R.id.txt_rank3_name);
        txtRank3Points = view.findViewById(R.id.txt_rank3_points);

        txtCurrentUserPoints = view.findViewById(R.id.txt_current_user_points);
        txtCurrentUserNameRank = view.findViewById(R.id.txt_current_user_name_rank);
        txtCurrentRank = view.findViewById(R.id.txt_current_rank);

        firestoreManager = new FirestoreManager();
        
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("loggedInUid", "");

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ActivityFragment())
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            });
        }

        if (btnRedeem != null) {
            btnRedeem.setOnClickListener(v -> {
                // Link to Redemption activity or fragment if it exists
                // Fragment nextFragment = new RedemptionFragment();
                // getParentFragmentManager().beginTransaction()
                //         .replace(R.id.fragment_container, nextFragment)
                //         .addToBackStack(null)
                //         .commit();
            });
        }

        setupPieChart();

        return view;
    }

    private void setupPieChart() {
        if (pieChart == null) return;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadData();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void loadData() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        if (userListener != null) userListener.remove();
        userListener = firestoreManager.listenToUser(currentUserId, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord userRecord) {
                if (userRecord != null && isAdded()) {
                    if (txtPoints != null) txtPoints.setText(String.valueOf(userRecord.getPoints()));
                    if (txtCurrentUserNameRank != null) txtCurrentUserNameRank.setText(userRecord.getUsername());
                    if (txtCurrentUserPoints != null) txtCurrentUserPoints.setText(String.valueOf(userRecord.getPoints()));
                }
            }
            @Override
            public void onFailure(String error) {}
        });

        firestoreManager.getMaterialStats(currentUserId, new FirestoreManager.OnMaterialStatsFetchListener() {
            @Override
            public void onStatsFetched(List<String> materials, List<Long> counts) {
                if (isAdded()) updatePieChartAndTrees(materials, counts);
            }
            @Override
            public void onFailure(String error) {}
        });

        firestoreManager.getAllUsers(new FirestoreManager.OnListFetchListener<UserRecord>() {
            @Override
            public void onListFetched(List<UserRecord> list) {
                if (isAdded()) updateRanking(list);
            }
            @Override
            public void onFailure(String error) {}
        });
    }

    private void updatePieChartAndTrees(List<String> materials, List<Long> counts) {
        long totalTrees = 0;
        List<PieEntry> entries = new ArrayList<>();

        for (int i = 0; i < materials.size(); i++) {
            String material = materials.get(i);
            long count = counts.get(i);

            entries.add(new PieEntry(count, material));

            if (material.equalsIgnoreCase("Plastic")) {
                totalTrees += count * 10;
            } else if (material.equalsIgnoreCase("Paper")) {
                totalTrees += count * 5;
            } else if (material.equalsIgnoreCase("Metal")) {
                totalTrees += count * 5;
            } else if (material.equalsIgnoreCase("Paper")) {
                totalTrees += count * 5;
            } else {
                totalTrees += count * 2;
            }
        }

        if (txtTreesSaved != null) {
            txtTreesSaved.setText("You save " + totalTrees + " Trees!");
        }

        if (pieChart == null || entries.isEmpty()) return;

        PieDataSet dataSet = new PieDataSet(entries, "Recycling Distribution");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateRanking(List<UserRecord> list) {
        if (list.size() >= 1 && txtRank1Name != null && txtRank1Points != null) {
            txtRank1Name.setText(list.get(0).getUsername());
            txtRank1Points.setText(String.valueOf(list.get(0).getPoints()));
        }
        if (list.size() >= 2 && txtRank2Name != null && txtRank2Points != null) {
            txtRank2Name.setText(list.get(1).getUsername());
            txtRank2Points.setText(String.valueOf(list.get(1).getPoints()));
        }
        if (list.size() >= 3 && txtRank3Name != null && txtRank3Points != null) {
            txtRank3Name.setText(list.get(2).getUsername());
            txtRank3Points.setText(String.valueOf(list.get(2).getPoints()));
        }

        if (txtCurrentRank != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUid().equals(currentUserId)) {
                    int rank = i + 1;
                    String suffix = "th";
                    if (rank == 1) suffix = "st";
                    else if (rank == 2) suffix = "nd";
                    else if (rank == 3) suffix = "rd";
                    txtCurrentRank.setText(rank + suffix);
                    break;
                }
            }
        }
    }
}
