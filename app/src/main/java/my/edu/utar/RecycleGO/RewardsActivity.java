package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class RewardsActivity extends Fragment {

    private static final String TAG = "RewardsActivity";

    private TextView txtPoints, txtTreesSaved;
    private PieChart pieChart;
    private Button btnHistory, btnRedeem;
    
    // Ranking views
    private TextView txtRank1Name, txtRank1Points;
    private TextView txtRank2Name, txtRank2Points;
    private TextView txtRank3Name, txtRank3Points;
    private TextView txtCurrentUserPoints, txtCurrentUserNameRank, txtCurrentRank;
    private CircleImageView imgRank1, imgRank2, imgRank3, imgCurrentUserRank;

    private FirestoreManager firestoreManager;
    private String currentUserId;
    private ListenerRegistration userListener;
    private int currentBottomColor = Color.BLACK;

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
        
        imgRank1 = view.findViewById(R.id.img_rank1);
        imgRank2 = view.findViewById(R.id.img_rank2);
        imgRank3 = view.findViewById(R.id.img_rank3);

        txtCurrentUserPoints = view.findViewById(R.id.txt_current_user_points);
        txtCurrentUserNameRank = view.findViewById(R.id.txt_current_user_name_rank);
        txtCurrentRank = view.findViewById(R.id.txt_current_rank);
        imgCurrentUserRank = view.findViewById(R.id.img_current_user_rank);

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

        setupPieChart();
        applyCustomTheme(view);

        return view;
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String accentColorCode = prefs.getString("accent_color", "#1A2A4E");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int accentColor = Color.parseColor(accentColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);
        currentBottomColor = bottomColor;

        ViewGroup root = (ViewGroup) view;
        updateColorsRecursively(root, accentColor, bottomColor);

        if (btnRedeem != null) btnRedeem.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
        if (btnHistory != null) btnHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        
        if (pieChart != null) {
            pieChart.setCenterTextColor(bottomColor);
        }
    }

    private void updateColorsRecursively(ViewGroup parent, int accentColor, int bottomColor) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (child instanceof androidx.cardview.widget.CardView) {
                    androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) child;
                    if (card.getCardBackgroundColor().getDefaultColor() != Color.parseColor("#ADE3F2")) {
                        card.setCardBackgroundColor(bottomColor);
                    }
                }
                updateColorsRecursively((ViewGroup) child, accentColor, bottomColor);
            } else if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                if (iv.getId() != R.id.img_rank1 && iv.getId() != R.id.img_rank2 && 
                    iv.getId() != R.id.img_rank3 && iv.getId() != R.id.img_current_user_rank &&
                    iv.getId() != R.id.img_earth && iv.getId() != R.id.iv_rank_icon) {
                    iv.setColorFilter(accentColor);
                } else if (iv.getId() == R.id.iv_rank_icon) {
                    iv.clearColorFilter();
                }
            }
        }
    }

    private void setupPieChart() {
        if (pieChart == null) return;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        
        pieChart.setDrawEntryLabels(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);

        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setWordWrapEnabled(true);
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
                    
                    if (imgCurrentUserRank != null && userRecord.getProfilePicUrl() != null && !userRecord.getProfilePicUrl().isEmpty()) {
                        Glide.with(RewardsActivity.this)
                                .load(userRecord.getProfilePicUrl())
                                .placeholder(R.drawable.useravatar)
                                .into(imgCurrentUserRank);
                    }
                }
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error listening to user: " + error);
            }
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
            public void onFailure(String error) {
                Log.e(TAG, "Error fetching rankings: " + error);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load rankings. Check Firestore indexes.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updatePieChartAndTrees(List<String> materials, List<Long> counts) {
        long totalTrees = 0;
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (materials != null && !materials.isEmpty()) {
            for (int i = 0; i < materials.size(); i++) {
                String material = materials.get(i);
                long count = counts.get(i);
                entries.add(new PieEntry(count, material));

                if (material.equalsIgnoreCase("Plastic")) {
                    totalTrees += count * 10;
                    colors.add(Color.parseColor("#2196F3"));
                } else if (material.equalsIgnoreCase("Paper")) {
                    totalTrees += count * 5;
                    colors.add(Color.parseColor("#FFEB3B"));
                } else if (material.equalsIgnoreCase("Metal")) {
                    totalTrees += count * 8;
                    colors.add(Color.parseColor("#9E9E9E"));
                } else if (material.equalsIgnoreCase("E-Waste")) {
                    totalTrees += count * 15;
                    colors.add(Color.parseColor("#9C27B0"));
                } else if (material.equalsIgnoreCase("Clothing") || material.equalsIgnoreCase("Textile")) {
                    totalTrees += count * 4;
                    colors.add(Color.parseColor("#795548"));
                } else if (material.equalsIgnoreCase("Household Waste")) {
                    totalTrees += count * 2;
                    colors.add(Color.parseColor("#4CAF50"));
                } else {
                    totalTrees += count * 2;
                    colors.add(ColorTemplate.JOYFUL_COLORS[i % ColorTemplate.JOYFUL_COLORS.length]);
                }
            }
        } else {
            entries.add(new PieEntry(1f, "No Data"));
            colors.add(Color.LTGRAY);
        }

        if (txtTreesSaved != null) {
            txtTreesSaved.setText("You save " + totalTrees + " Trees!");
        }

        if (pieChart == null) return;

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);
        pieChart.setCenterTextColor(currentBottomColor);

        // Add Listener to show details when tapped
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    if (pe.getLabel().equals("No Data")) {
                        pieChart.setCenterText("No recycling data");
                        return;
                    }
                    float total = pieChart.getData().getYValueSum();
                    float percentage = (pe.getValue() / total) * 100f;
                    
                    String centerText = pe.getLabel() + "\n" + 
                                     String.format(Locale.getDefault(), "%.1f%%", percentage) + "\n" +
                                     (int)pe.getValue() + " items";
                    
                    pieChart.setCenterText(centerText);
                    pieChart.setCenterTextSize(14f);
                }
            }

            @Override
            public void onNothingSelected() {
                pieChart.setCenterText("");
            }
        });

        pieChart.invalidate();
    }
    
    private float spToPx(int sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void updateRanking(List<UserRecord> list) {
        if (list == null) return;

        // Top 3 ranking
        if (list.size() >= 1 && txtRank1Name != null) {
            txtRank1Name.setText(list.get(0).getUsername());
            txtRank1Points.setText(String.valueOf(list.get(0).getPoints()));
            loadAvatarToRanking(list.get(0).getProfilePicUrl(), imgRank1);
        }
        if (list.size() >= 2 && txtRank2Name != null) {
            txtRank2Name.setText(list.get(1).getUsername());
            txtRank2Points.setText(String.valueOf(list.get(1).getPoints()));
            loadAvatarToRanking(list.get(1).getProfilePicUrl(), imgRank2);
        }
        if (list.size() >= 3 && txtRank3Name != null) {
            txtRank3Name.setText(list.get(2).getUsername());
            txtRank3Points.setText(String.valueOf(list.get(2).getPoints()));
            loadAvatarToRanking(list.get(2).getProfilePicUrl(), imgRank3);
        }

        // Current user rank
        if (txtCurrentRank != null && currentUserId != null) {
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                UserRecord user = list.get(i);
                if (user.getUid() != null && user.getUid().equals(currentUserId)) {
                    int rank = i + 1;
                    txtCurrentRank.setText(rank + getRankSuffix(rank));
                    found = true;
                    break;
                }
            }
            if (!found) txtCurrentRank.setText("-");
        }
    }

    private String getRankSuffix(int rank) {
        if (rank >= 11 && rank <= 13) return "th";
        switch (rank % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private void loadAvatarToRanking(String url, CircleImageView iv) {
        if (url != null && !url.isEmpty() && isAdded()) {
            Glide.with(this).load(url).placeholder(R.drawable.useravatar).into(iv);
        } else if (iv != null) {
            iv.setImageResource(R.drawable.useravatar);
        }
    }
}
