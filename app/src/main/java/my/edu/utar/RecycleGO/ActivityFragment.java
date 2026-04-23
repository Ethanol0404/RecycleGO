package my.edu.utar.RecycleGO;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.*;

public class ActivityFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_activity, container, false);
    }
}
