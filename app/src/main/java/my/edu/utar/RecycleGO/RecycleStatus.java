package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleRequest;

public class RecycleStatus extends Fragment {

    private RecyclerView recyclerView;
    private AdapterRecycleStatus adapter;
    private List<RecycleRequest> allItems = new ArrayList<>();
    private Button btnCreate;
    private FirestoreManager firestoreManager;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_recycle_status, container, false);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
        
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Setup Spinner
        Spinner spinner = view.findViewById(R.id.statusSpinner);
        String[] options = {"Selected: All", "Requesting", "Waiting pick up", "Completed"};
        
        if (spinner != null) {
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(spinnerAdapter);
            
            // 3. Spinner Filter Logic
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    filterList(options[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        recyclerView = view.findViewById(R.id.rvRecycleStatus);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            setupAdapter(allItems);
        }

        btnCreate = view.findViewById(R.id.status_btnCreate);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                Fragment nextFragment = new PickUpActivity();
                Bundle args = new Bundle();
                args.putString("flow", "STATUS_TO_FORM");
                nextFragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, nextFragment)
                        .addToBackStack("RECYCLE_STATUS")
                        .commit();
            });
        }
        
        loadData();
    }

    private void setupAdapter(List<RecycleRequest> list) {
        adapter = new AdapterRecycleStatus(list, new AdapterRecycleStatus.OnStatusActionListener() {
            @Override
            public void onAccept(RecycleRequest request) {
                updateStatus(request.getId(), "Accepted");
            }

            @Override
            public void onComplete(RecycleRequest request) {
                updateStatus(request.getId(), "Completed");
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        firestoreManager.getRequestsByUser(userId).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                allItems.clear();
                for (QueryDocumentSnapshot doc : value) {
                    RecycleRequest req = doc.toObject(RecycleRequest.class);
                    allItems.add(req);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void updateStatus(String requestId, String newStatus) {
        firestoreManager.updateRequestStatus(requestId, newStatus, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Fail: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterList(String status) {
        if (adapter == null) return;
        
        List<RecycleRequest> filtered = new ArrayList<>();
        if (status.equals("Selected: All")) {
            filtered.addAll(allItems);
        } else {
            for (RecycleRequest item : allItems) {
                if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(status)) {
                    filtered.add(item);
                }
            }
        }
        setupAdapter(filtered);
    }
}
