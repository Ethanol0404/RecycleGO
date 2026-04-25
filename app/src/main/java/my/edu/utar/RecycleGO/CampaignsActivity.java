package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.CampaignRecord;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class CampaignsActivity extends Fragment {

    private RecyclerView rvCampaigns;
    private CampaignAdapter adapter;
    private List<CampaignRecord> campaignList;
    private FirestoreManager firestoreManager;
    private FloatingActionButton btnAdd;

    public CampaignsActivity() {
        // Required empty public constructor
    }

    public static CampaignsActivity newInstance() {
        return new CampaignsActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_campaigns, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreManager = new FirestoreManager();

        // 1. Initialize RecyclerView
        rvCampaigns = view.findViewById(R.id.rv_feed);
        rvCampaigns.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCampaigns.setNestedScrollingEnabled(false);

        // 2. Setup Adapter
        campaignList = new ArrayList<>();
        adapter = new CampaignAdapter(campaignList);
        rvCampaigns.setAdapter(adapter);

        // 3. Initialize FAB and set click listener based on role
        btnAdd = view.findViewById(R.id.community_btnAdd);
        
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userRole = prefs.getString("loggedInRole", "User");
        
        if (btnAdd != null) {
            if ("Admin".equalsIgnoreCase(userRole)) {
                btnAdd.setVisibility(View.VISIBLE);
                btnAdd.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new CreateCampaignActivity())
                            .addToBackStack(null)
                            .commit();
                    }
                });
            } else {
                btnAdd.setVisibility(View.GONE);
            }
        }

        // 4. Load Data from Firestore
        loadCampaigns();
    }

    private void loadCampaigns() {
        firestoreManager.getUpcomingCampaigns(new FirestoreManager.OnListFetchListener<CampaignRecord>() {
            @Override
            public void onListFetched(List<CampaignRecord> list) {
                if (!isAdded()) return;
                
                campaignList.clear();
                if (list != null && !list.isEmpty()) {
                    campaignList.addAll(list);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load events: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
