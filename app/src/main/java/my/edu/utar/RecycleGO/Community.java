package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class Community extends Fragment {

    private RecyclerView rvCommunities, rvFeed;
    private AdapterCommunityProfile communityAdapter;
    private AdapterCommunityPost feedAdapter;
    private FirestoreManager firestoreManager;
    private String userUid;
    private String selectedCommunityId = ""; // Track current filter
    private List<String> subscribedIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        View view = inflater.inflate(R.layout.activity_community, container, false);

        firestoreManager = new FirestoreManager();
        
        // Use UID from SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        userUid = prefs.getString("loggedInUid", "");

        rvCommunities = view.findViewById(R.id.community_profileList);
        rvFeed = view.findViewById(R.id.rv_feed);
        FloatingActionButton btnAdd = view.findViewById(R.id.community_btnAdd);
        
        EditText etSearchTrigger = view.findViewById(R.id.et_search_trigger);
        if (etSearchTrigger != null) {
            etSearchTrigger.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), CommunitySearchActivity.class);
                startActivity(intent);
            });
        }

        rvCommunities.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        communityAdapter = new AdapterCommunityProfile(new ArrayList<>(), community -> {
            selectedCommunityId = community.getCommunityID();
            loadFeed(java.util.Collections.singletonList(selectedCommunityId));
        });
        rvCommunities.setAdapter(communityAdapter);

        rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new AdapterCommunityPost(new ArrayList<>());
        rvFeed.setAdapter(feedAdapter);

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), CreatePostActivity.class);
                // Pass current selection or fallback
                String targetId = selectedCommunityId;
                if (targetId.isEmpty() && !subscribedIds.isEmpty()) {
                    targetId = subscribedIds.get(0);
                }
                intent.putExtra("communityID", targetId);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userUid != null && !userUid.isEmpty()) {
            loadSubscribedCommunities();
        } else {
            Toast.makeText(getContext(), "Please login to view community", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSubscribedCommunities() {
        firestoreManager.getUser(userUid, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null && user.getSubscribedCommunities() != null && !user.getSubscribedCommunities().isEmpty()) {
                    subscribedIds = user.getSubscribedCommunities();

                    if (selectedCommunityId.isEmpty()) {
                        selectedCommunityId = subscribedIds.get(0);
                    }

                    fetchCommunityModels(subscribedIds);
                    loadFeed(java.util.Collections.singletonList(selectedCommunityId));
                } else {
                    // Handle case where user has no subscriptions
                    communityAdapter.updateList(new ArrayList<>());
                    feedAdapter.updateList(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCommunityModels(List<String> ids) {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                List<CommunityModel> filtered = list.stream()
                        .filter(c -> ids.contains(c.getCommunityID()))
                        .collect(java.util.stream.Collectors.toList());
                communityAdapter.updateList(filtered);
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    private void loadFeed(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            feedAdapter.updateList(new ArrayList<>());
            return;
        }

        firestoreManager.getFeedPosts(ids, new FirestoreManager.OnListFetchListener<CommunityPost>() {
            @Override
            public void onListFetched(List<CommunityPost> list) {
                feedAdapter.updateList(list);
            }

            @Override
            public void onFailure(String error) {
                // If it's a new community or no index, don't show toast if it's just empty
                if (getContext() != null) {
                    // Check if error is index related
                    if (error.contains("FAILED_PRECONDITION")) {
                        Toast.makeText(getContext(), "Loading feed... (Setting up database)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Feed Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
