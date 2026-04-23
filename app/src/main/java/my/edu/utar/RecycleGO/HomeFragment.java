package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    TextView txtPoints;
    CardView cardNews, cardRecycleNow, cardFindEvent, cardQuizPlastic, cardQuizFood;
    FloatingActionButton fabAiAssistant;
    ImageView imgPointsDropdown;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtPoints = view.findViewById(R.id.txt_points);
        cardNews = view.findViewById(R.id.card_news);
        cardRecycleNow = view.findViewById(R.id.card_recycle_now);
        cardFindEvent = view.findViewById(R.id.card_find_event);
        cardQuizPlastic = view.findViewById(R.id.card_quiz_plastic);
        cardQuizFood = view.findViewById(R.id.card_quiz_food);
        fabAiAssistant = view.findViewById(R.id.fab_ai_assistant);
        imgPointsDropdown = view.findViewById(R.id.img_points_dropdown);

        // Load Points
        loadPoints();

        // Points Arrow click listener
        if (imgPointsDropdown != null) {
            imgPointsDropdown.setOnClickListener(v -> {
                Fragment nextFragment = new RewardsActivity();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, nextFragment)
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            });
        }

        // Click Listener for Global Warming News Link
        cardNews.setOnClickListener(v -> {
            String url = "https://blog.ucs.org/marc-alessi/terrible-team-super-el-nino-and-climate-change-could-lead-to-record-breaking-global-temperatures/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        // Recycle Now
        cardRecycleNow.setOnClickListener(v -> {
            Fragment nextFragment = new RecycleStatus();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

        // Find Event
        cardFindEvent.setOnClickListener(v -> {
            Fragment nextFragment = new CampaignsActivity();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

        // Plastic Quiz
        cardQuizPlastic.setOnClickListener(v -> {
            QuizFragment nextFragment = new QuizFragment();
            Bundle args = new Bundle();
            args.putString("category", "plastic");
            nextFragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

        // Food Quiz
        cardQuizFood.setOnClickListener(v -> {
            QuizFragment nextFragment = new QuizFragment();
            Bundle args = new Bundle();
            args.putString("category", "food");
            nextFragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

        // AI Assistant
        if (fabAiAssistant != null) {
            fabAiAssistant.setOnClickListener(v -> {
                AIAssistant assistant = new AIAssistant();
                assistant.show(getParentFragmentManager(), assistant.getTag());
            });
        }

        return view;
    }

    private void loadPoints() {
        if (getContext() != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            int points = prefs.getInt("totalPoints", 1000);
            txtPoints.setText(String.valueOf(points));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPoints();
    }
}
