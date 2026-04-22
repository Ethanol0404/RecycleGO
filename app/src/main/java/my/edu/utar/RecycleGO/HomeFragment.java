package my.edu.utar.RecycleGO;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.*;
import android.widget.TextView;

public class HomeFragment extends Fragment {

    TextView txtPoints;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtPoints = view.findViewById(R.id.txt_points);

        // 假数据（demo用）
        int points = 120;
        txtPoints.setText(points + " pts");

        return view;
    }
}
