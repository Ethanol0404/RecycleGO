package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.NewsRecord;
import my.edu.utar.RecycleGO.database.UserRecord;
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
    ViewPager2 viewPagerNews;
    LinearLayout layoutDots;

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
        txtTipTitle = view.findViewById(R.id.txt_tip_title);
        txtTipBody = view.findViewById(R.id.txt_tip_body);
        viewPagerNews = view.findViewById(R.id.view_pager_news);
        layoutDots = view.findViewById(R.id.layout_dots);

        firestoreManager = new FirestoreManager();
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");

        // Apply theme to AI Assistant FAB
        applyTheme(view);

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

        // Fetch and display News from Firestore
        loadNews();

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

        // AI Assistant - Fixed to show as full fragment within Frame
        if (fabAiAssistant != null) {
            fabAiAssistant.setOnClickListener(v -> {
                Fragment assistantFragment = new AIAssistant();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, assistantFragment)
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            });
        }

        return view;
    }

    private void applyTheme(View view) {
        SharedPreferences themePrefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bottomColorCode = themePrefs.getString("bottom_color", "#265200");
        int themeColor = Color.parseColor(bottomColorCode);

        if (fabAiAssistant != null) {
            fabAiAssistant.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        }
    }

    private void loadNews() {
        if (viewPagerNews == null) return;

        firestoreManager.getNews(new FirestoreManager.OnListFetchListener<NewsRecord>() {
            @Override
            public void onListFetched(List<NewsRecord> newsList) {
                if (newsList != null && !newsList.isEmpty()) {
                    List<NewsRecord> expandedList = new ArrayList<>();
                    
                    for (NewsRecord doc : newsList) {
                        // Check if title is a List and expand it
                        if (doc.getTitle() instanceof List) {
                            List<?> titles = (List<?>) doc.getTitle();
                            for (int i = 0; i < titles.size(); i++) {
                                NewsRecord item = new NewsRecord();
                                item.setTitle(titles.get(i));
                                // Safely extract corresponding index from other potential arrays
                                item.setPicurl(getItemAt(doc.getPicurl(), i));
                                item.setNewsurl(getItemAt(doc.getNewsurl(), i));
                                item.setColor(getItemAt(doc.getColor(), i));
                                expandedList.add(item);
                            }
                        } else {
                            // If it's not a list, it's a single news document
                            expandedList.add(doc);
                        }
                    }

                    if (isAdded()) {
                        NewsAdapter adapter = new NewsAdapter(expandedList);
                        viewPagerNews.setAdapter(adapter);
                        setupDotsIndicator(expandedList.size());
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading news: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Helper to safely get index from a List or return the object itself if not a list
    private Object getItemAt(Object obj, int index) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (index < list.size()) {
                return list.get(index);
            } else if (!list.isEmpty()) {
                return list.get(0); // Fallback to first if index out of bounds
            }
        }
        return obj;
    }

    private void setupDotsIndicator(int count) {
        if (layoutDots == null || !isAdded()) return;
        layoutDots.removeAllViews();
        if (count <= 1) return; // Don't show dots for a single item

        ImageView[] dots = new ImageView[count];
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(getContext());
            dots[i].setImageResource(R.drawable.circle_dot); // Using the new circle drawable
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    16, // Width in pixels
                    16  // Height in pixels
            );
            params.setMargins(12, 0, 12, 0);
            layoutDots.addView(dots[i], params);
        }
        
        viewPagerNews.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < count; i++) {
                    dots[i].setAlpha(i == position ? 1.0f : 0.4f);
                }
            }
        });
    }

    private void startListeningToPoints() {
        if (userId == null || userId.isEmpty()) {
            txtPoints.setText("0");
            return;
        }

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
                txtPoints.setText("0");
            }
        });
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
        fetchRecyclingTip();
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
