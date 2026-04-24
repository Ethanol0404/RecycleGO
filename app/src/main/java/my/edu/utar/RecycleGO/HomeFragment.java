package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Random;

import my.edu.utar.RecycleGO.utils.RecycleApiService;
import my.edu.utar.RecycleGO.utils.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    TextView txtPoints, txtTipTitle, txtTipBody;
    CardView cardNews, cardRecycleNow, cardFindEvent, cardQuizPlastic, cardQuizFood;
    FloatingActionButton fabAiAssistant;
    ImageView imgPointsDropdown;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_main, container, false);

        txtPoints = view.findViewById(R.id.txt_points);
        cardNews = view.findViewById(R.id.card_news);
        cardRecycleNow = view.findViewById(R.id.card_recycle_now);
        cardFindEvent = view.findViewById(R.id.card_find_event);
        cardQuizPlastic = view.findViewById(R.id.card_quiz_plastic);
        cardQuizFood = view.findViewById(R.id.card_quiz_food);
        fabAiAssistant = view.findViewById(R.id.fab_ai_assistant);
        imgPointsDropdown = view.findViewById(R.id.img_points_dropdown);
        txtTipTitle = view.findViewById(R.id.txt_tip_title);
        txtTipBody = view.findViewById(R.id.txt_tip_body);

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
                // Change AIAssistant to extend Fragment (not DialogFragment)
                AIAssistant assistant = new AIAssistant();

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, assistant)
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
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

    private void fetchRecyclingTip() {
        RecycleApiService service = RetrofitClient.getClient().create(RecycleApiService.class);
        service.getRecyclingTips().enqueue(new Callback<List<RecycleApiService.RecycleTip>>() {
            @Override
            public void onResponse(Call<List<RecycleApiService.RecycleTip>> call, Response<List<RecycleApiService.RecycleTip>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<RecycleApiService.RecycleTip> tips = response.body();

                    Random random = new Random();
                    RecycleApiService.RecycleTip tip = tips.get(random.nextInt(tips.size()));

                    if (txtTipTitle != null) txtTipTitle.setText("Tip: " + tip.getTitle());
                    if (txtTipBody != null) txtTipBody.setText(tip.getBody());
                }
            }

            @Override
            public void onFailure(Call<List<RecycleApiService.RecycleTip>> call, Throwable t) {
                if (txtTipTitle != null) txtTipTitle.setText("Unable to load tips");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPoints();
        fetchRecyclingTip();
    }
}
