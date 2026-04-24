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
import com.google.firebase.firestore.ListenerRegistration;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class HomeFragment extends Fragment {

    TextView txtPoints;
    CardView cardNews, cardRecycleNow, cardFindEvent, cardQuizPlastic, cardQuizFood;
    FloatingActionButton fabAiAssistant;
    ImageView imgPointsDropdown;
    FirestoreManager firestoreManager;
    String userId;
    ListenerRegistration userListener;

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

        firestoreManager = new FirestoreManager();
        
        // Use userId from SharedPreferences for session management
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");

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
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, assistant)
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            });
        }

        return view;
    }

    private void startListeningToPoints() {
        if (userId == null || userId.isEmpty()) {
            txtPoints.setText("0");
            return;
        }

        // Remove previous listener if exists
        if (userListener != null) {
            userListener.remove();
        }

        // Real-time listener to Firebase Firestore
        userListener = firestoreManager.listenToUser(userId, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord userRecord) {
                if (userRecord != null && isAdded()) {
                    txtPoints.setText(String.valueOf(userRecord.getPoints()));
                }
            }

            @Override
            public void onFailure(String error) {
                // If Firebase fails, we don't fall back to 1000
                txtPoints.setText("0");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningToPoints();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
