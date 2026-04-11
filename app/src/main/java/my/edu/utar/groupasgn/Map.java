package my.edu.utar.groupasgn;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class Map extends Fragment {

    public Map() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(false);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_map, container, false);
    }

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        // Show the header again when leaving this fragment
//        if (getActivity() instanceof FrameActivity) {
//            ((FrameActivity) getActivity()).setHeaderVisible(true);
//        }
//    }
}