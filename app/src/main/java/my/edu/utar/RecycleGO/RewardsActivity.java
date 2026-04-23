package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RewardsActivity extends Fragment {

    private TextView txtTotalPoints;
    private ImageView btnBack;

    public RewardsActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTotalPoints = view.findViewById(R.id.reward_total_points);
        btnBack = view.findViewById(R.id.btn_back_rewards);

        // Load Points
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int points = prefs.getInt("totalPoints", 1000);
        txtTotalPoints.setText(String.valueOf(points));

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}
