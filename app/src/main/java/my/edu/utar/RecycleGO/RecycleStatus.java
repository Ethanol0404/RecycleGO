package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleRequest;
import my.edu.utar.RecycleGO.database.UserRecord;

public class RecycleStatus extends Fragment {

    private RecyclerView recyclerView;
    private AdapterRecycleStatus adapter;
    private List<RecycleRequest> allItems = new ArrayList<>();
    private String currentFilter = "Selected: All";
    private FirestoreManager firestoreManager;
    private String userId;
    private String userRole;
    private View layoutStatus, layoutMessage; 
    private com.google.android.material.button.MaterialButtonToggleGroup toggleGroup;
    private ListenerRegistration snapshotListener;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }
        return inflater.inflate(R.layout.activity_recycle_status, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
        
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");
        userRole = prefs.getString("loggedInRole", "User");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.rvRecycleStatus);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterRecycleStatus(new ArrayList<>(), new AdapterRecycleStatus.OnStatusActionListener() {
            @Override
            public void onAccept(RecycleRequest request) { updateStatus(request.getId(), "Accepted"); }
            @Override
            public void onComplete(RecycleRequest request) { updateStatus(request.getId(), "Completed"); }
            @Override
            public void onVerify(RecycleRequest request) { confirmComplete(request); }
            @Override
            public void onItemClick(RecycleRequest request) { showDetailPopup(request); }
        });
        recyclerView.setAdapter(adapter);

        // Setup Spinner
        Spinner spinner = view.findViewById(R.id.statusSpinner);
        String[] options = {"Selected: All", "Requesting", "Accepted", "Completed"};
        
        if (spinner != null) {
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(spinnerAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentFilter = options[position];
                    applyFilter();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        View btnCreate = view.findViewById(R.id.status_btnCreate);
        // Requirement 2: Admin can no longer pick up/create, so hide button.
        if ("Admin".equalsIgnoreCase(userRole)) {
            if (btnCreate != null) btnCreate.setVisibility(View.GONE);
        }
        
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

        layoutStatus = view.findViewById(R.id.layout_status_list);
        layoutMessage = view.findViewById(R.id.layout_message_list);
        toggleGroup = view.findViewById(R.id.toggleGroup);

        if (toggleGroup != null) {
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btn_status_list) {
                        if (layoutStatus != null) layoutStatus.setVisibility(View.VISIBLE);
                        if (layoutMessage != null) layoutMessage.setVisibility(View.GONE);
                    } else {
                        if (layoutStatus != null) layoutStatus.setVisibility(View.GONE);
                        if (layoutMessage != null) layoutMessage.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::startRealtimeListener);
        }

        startRealtimeListener();
    }

    private void startRealtimeListener() {
        if (userId == null || userId.isEmpty()) return;

        firestoreManager.getUser(userId, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user == null || !isAdded()) return;
                
                Query query;
                if ("Admin".equalsIgnoreCase(user.getRole())) {
                    List<String> centerIds = user.getJoinedCenters();
                    Log.d("RecycleStatus", "Admin query centers: " + centerIds);
                    query = firestoreManager.getRequestsByCenters(centerIds);
                } else {
                    query = firestoreManager.getRequestsByUser(userId);
                }

                if (snapshotListener != null) snapshotListener.remove();

                snapshotListener = query.addSnapshotListener((value, error) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    
                    if (error != null) {
                        Log.e("RecycleStatus", "Listen failed.", error);
                        if (error.getMessage() != null && error.getMessage().contains("FAILED_PRECONDITION")) {
                            if (isAdded()) Toast.makeText(getContext(), "Database index required.", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    if (value != null) {
                        allItems.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            RecycleRequest req = doc.toObject(RecycleRequest.class);
                            if (req.getId() == null) req.setId(doc.getId());
                            allItems.add(req);
                        }
                        applyFilter();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("RecycleStatus", "User fetch error: " + error);
            }
        });
    }

    private void applyFilter() {
        if (adapter == null) return;
        
        List<RecycleRequest> filtered = new ArrayList<>();
        if (currentFilter.equals("Selected: All")) {
            filtered.addAll(allItems);
        } else {
            for (RecycleRequest item : allItems) {
                if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(currentFilter)) {
                    filtered.add(item);
                }
            }
        }
        adapter.updateList(filtered);
        
        View noDataView = getView() != null ? getView().findViewById(R.id.tv_no_data) : null;
        if (noDataView != null) {
            noDataView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStatus(String requestId, String newStatus) {
        firestoreManager.updateRequestStatus(requestId, newStatus, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String error) {
                if (isAdded()) Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDetailPopup(RecycleRequest request) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_detail, null);
        
        ((TextView)view.findViewById(R.id.detail_category)).setText(request.getCategory());
        ((TextView)view.findViewById(R.id.detail_date)).setText("Date: " + request.getDate());
        ((TextView)view.findViewById(R.id.detail_center)).setText("Center: " + request.getCenterName());
        ((TextView)view.findViewById(R.id.detail_remarks)).setText("Remarks: " + request.getRemarks());
        ((TextView)view.findViewById(R.id.detail_status)).setText(request.getStatus());

        Button btnEdit = view.findViewById(R.id.btn_action_edit);
        Button btnDelete = view.findViewById(R.id.btn_action_delete);
        Button btnAccept = view.findViewById(R.id.btn_action_accept);
        Button btnConfirmPickup = view.findViewById(R.id.btn_action_confirm_pickup);
        Button btnConfirmComplete = view.findViewById(R.id.btn_action_confirm_complete);
        Button btnReport = view.findViewById(R.id.btn_action_report);
        Button btnMsg = view.findViewById(R.id.btn_action_message);

        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        String s = request.getStatus();

        // Reset visibility
        btnEdit.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
        btnAccept.setVisibility(View.GONE);
        btnConfirmPickup.setVisibility(View.GONE);
        btnConfirmComplete.setVisibility(View.GONE);
        btnReport.setVisibility(View.GONE);
        btnMsg.setVisibility(View.GONE);

        if ("Requesting".equalsIgnoreCase(s)) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            if (isAdmin) btnAccept.setVisibility(View.VISIBLE);
        } else if ("Accepted".equalsIgnoreCase(s)) {
            btnMsg.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            if (isAdmin) btnConfirmPickup.setVisibility(View.VISIBLE);
        } else if ("Completed".equalsIgnoreCase(s)) {
            btnMsg.setVisibility(View.VISIBLE);
            btnReport.setVisibility(View.VISIBLE);
        }

        btnEdit.setOnClickListener(v -> {
            Fragment nextFragment = new PickUpActivity();
            Bundle args = new Bundle();
            args.putString("flow", "EDIT_MODE");
            args.putSerializable("edit_request", request);
            nextFragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack("RECYCLE_STATUS")
                    .commit();
            dialog.dismiss();
        });

        btnAccept.setOnClickListener(v -> { updateStatus(request.getId(), "Accepted"); dialog.dismiss(); });
        btnConfirmPickup.setOnClickListener(v -> { updateStatus(request.getId(), "Completed"); dialog.dismiss(); });
        btnDelete.setOnClickListener(v -> { deleteRequest(request.getId()); dialog.dismiss(); });
        
        btnMsg.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening Messages...", Toast.LENGTH_SHORT).show();
            // Optional: Toggle to message tab or open AI Assistant
            if (toggleGroup != null) toggleGroup.check(R.id.btn_message_list);
            dialog.dismiss();
        });

        btnReport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Report submitted for request " + request.getId().substring(0, 5), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void confirmComplete(RecycleRequest request) {
        updateStatus(request.getId(), "Verified");
        firestoreManager.addPoints(userId, 50, "Verified Recycle", new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Points earned! +50", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFailure(String error) {
                Log.e("RecycleStatus", "Points error: " + error);
            }
        });
    }

    private void deleteRequest(String requestId) {
        firestoreManager.deleteRecycleRequest(requestId, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String error) {
                if (isAdded()) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (snapshotListener != null) {
            snapshotListener.remove();
        }
    }
}
