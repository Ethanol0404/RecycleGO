package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
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
    private String userRole = "User";
    private String selectedCommunityId = "";
    private List<String> subscribedIds = new ArrayList<>();
    private String targetPostID = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        View view = inflater.inflate(R.layout.activity_community, container, false);

        if (getArguments() != null) {
            targetPostID = getArguments().getString("targetPostID");
        }

        firestoreManager = new FirestoreManager();
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        userUid = prefs.getString("loggedInUid", "");
        userRole = prefs.getString("loggedInRole", "User");

        rvCommunities = view.findViewById(R.id.community_profileList);
        rvFeed = view.findViewById(R.id.rv_feed);
        FloatingActionButton btnAdd = view.findViewById(R.id.community_btnAdd);
        FloatingActionButton btnAddGroup = view.findViewById(R.id.community_btnAddGroup);

        if ("Admin".equalsIgnoreCase(userRole)) {
            btnAddGroup.setVisibility(View.VISIBLE);
            btnAddGroup.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CommunityAdminFragment())
                        .addToBackStack("ADD_COMMUNITY")
                        .commit();
            });
        }
        
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
                String targetId = selectedCommunityId;
                if (targetId.isEmpty() && !subscribedIds.isEmpty()) {
                    targetId = subscribedIds.get(0);
                }
                intent.putExtra("communityID", targetId);
                startActivity(intent);
            });
        }

        applyCustomTheme(view);
        return view;
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        String accentColorCode = prefs.getString("accent_color", "#1A2A4E");
        int bottomColor = Color.parseColor(bottomColorCode);
        int accentColor = Color.parseColor(accentColorCode);

        TextView tvTitle = view.findViewById(R.id.tv_community_title);
        FloatingActionButton btnAdd = view.findViewById(R.id.community_btnAdd);
        FloatingActionButton btnAddGroup = view.findViewById(R.id.community_btnAddGroup);

        if (tvTitle != null) tvTitle.setTextColor(bottomColor);
        if (btnAdd != null) btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        if (btnAddGroup != null) btnAddGroup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userUid != null && !userUid.isEmpty()) {
            if (targetPostID != null) {
                findPostAndLoad(targetPostID);
            } else {
                loadSubscribedCommunities();
            }
        }
    }

    private void findPostAndLoad(String postID) {
        firestoreManager.getPost(postID, new FirestoreManager.OnPostFetchListener() {
            @Override
            public void onPostFetched(CommunityPost post) {
                if (post != null) {
                    selectedCommunityId = post.getCommunityID();
                    loadSubscribedCommunities();
                } else {
                    loadSubscribedCommunities();
                }
            }

            @Override
            public void onFailure(String error) {
                loadSubscribedCommunities();
            }
        });
    }

    private void loadSubscribedCommunities() {
        firestoreManager.getUser(userUid, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null) {
                    subscribedIds = user.getSubscribedCommunities() != null ? user.getSubscribedCommunities() : new ArrayList<>();
                    
                    if (selectedCommunityId.isEmpty() && !subscribedIds.isEmpty()) {
                        selectedCommunityId = subscribedIds.get(0);
                    }

                    fetchCommunityModels(subscribedIds);
                    
                    if (!selectedCommunityId.isEmpty()) {
                        loadFeed(java.util.Collections.singletonList(selectedCommunityId));
                    } else {
                        fetchAllCommunitiesForDiscovery();
                    }
                } else {
                    fetchAllCommunitiesForDiscovery();
                }
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAllCommunitiesForDiscovery() {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                communityAdapter.updateList(list);
                if (!list.isEmpty()) {
                    if (selectedCommunityId.isEmpty()) selectedCommunityId = list.get(0).getCommunityID();
                    loadFeed(java.util.Collections.singletonList(selectedCommunityId));
                }
            }
            @Override
            public void onFailure(String error) {}
        });
    }

    private void fetchCommunityModels(List<String> ids) {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                List<CommunityModel> filtered = new ArrayList<>();
                for (CommunityModel c : list) { if (ids.contains(c.getCommunityID())) filtered.add(c); }
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
                if (targetPostID != null) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getPostID().equals(targetPostID)) {
                            rvFeed.scrollToPosition(i);
                            // Do not clear targetPostID here yet, in case onListFetched is called during async transitions
                            // We can use a temporary flag or just scroll.
                            final int pos = i;
                            rvFeed.post(() -> rvFeed.scrollToPosition(pos));
                            break;
                        }
                    }
                    targetPostID = null; // Clear it after processing
                }
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null && !error.contains("FAILED_PRECONDITION")) {
                    Toast.makeText(getContext(), "Feed Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
