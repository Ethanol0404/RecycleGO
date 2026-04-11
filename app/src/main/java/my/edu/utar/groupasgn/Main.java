package my.edu.utar.groupasgn;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Main extends Fragment {

    public Main() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_main, container, false);
    }
}