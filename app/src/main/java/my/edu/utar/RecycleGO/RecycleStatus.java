package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

    private RecyclerView recyclerView, rvMessages;
    private AdapterRecycleStatus adapter;
    private AdapterChatList chatAdapter;
    private List<RecycleRequest> allItems = new ArrayList<>();
    private List<String> adminCenterIds = new ArrayList<>();
    private String currentFilter = "Selected: All";
    private FirestoreManager firestoreManager;
    private String userId;
    private String userRole;
    private View layoutStatus, layoutMessages;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleGroup;
    private ListenerRegistration snapshotListener;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View dotUnreadTab;

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

        // Setup Layouts
        layoutStatus = view.findViewById(R.id.layout_status_list);
        layoutMessages = view.findViewById(R.id.layout_message_list);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        dotUnreadTab = view.findViewById(R.id.dot_unread_tab);

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.rvRecycleStatus);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        adapter = new AdapterRecycleStatus(new ArrayList<>(), isAdmin, new AdapterRecycleStatus.OnStatusActionListener() {
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

        rvMessages = view.findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        chatAdapter = new AdapterChatList(new ArrayList<>(), userRole, new AdapterChatList.OnChatActionListener() {
            @Override
            public void onChatClick(RecycleRequest request) {
                openChat(request.getId());
            }

            @Override
            public void onDeleteChat(RecycleRequest request) {
                showConfirmDeleteDialog(request.getId(), true);
            }
        });
        rvMessages.setAdapter(chatAdapter);

        if (toggleGroup != null) {
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btn_status_list) {
                        if (layoutStatus != null) layoutStatus.setVisibility(View.VISIBLE);
                        if (layoutMessages != null) layoutMessages.setVisibility(View.GONE);
                    } else {
                        if (layoutStatus != null) layoutStatus.setVisibility(View.GONE);
                        if (layoutMessages != null) layoutMessages.setVisibility(View.VISIBLE);
                        applyFilter();
                    }
                }
            });
        }

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

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::startRealtimeListener);
        }

        startRealtimeListener();
    }

    private void openChat(String requestId) {
        Intent intent = new Intent(requireContext(), ChatDirectActivity.class);
        intent.putExtra("requestId", requestId);
        startActivity(intent);
    }

    private void startRealtimeListener() {
        if (userId == null || userId.isEmpty()) return;

        firestoreManager.getUser(userId, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user == null || !isAdded()) return;
                
                Query query;
                if ("Admin".equalsIgnoreCase(user.getRole())) {
                    adminCenterIds = user.getJoinedCenters();
                    query = firestoreManager.getRequestsByCenters(adminCenterIds);
                } else {
                    query = firestoreManager.getRequestsByUser(userId);
                }

                if (snapshotListener != null) snapshotListener.remove();

                snapshotListener = query.addSnapshotListener((value, error) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    
                    if (error != null) {
                        Log.e("RecycleStatus", "Listen failed.", error);
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
            public void onFailure(String error) {}
        });
    }

    private void applyFilter() {
        if (adapter == null || chatAdapter == null) return;
        
        List<RecycleRequest> visibleList = new ArrayList<>();
        if ("Admin".equalsIgnoreCase(userRole)) {
            for (RecycleRequest item : allItems) {
                if ("Requesting".equalsIgnoreCase(item.getStatus())) {
                    // Everyone in targetCenterIds can see Requesting tasks
                    visibleList.add(item);
                } else {
                    if (adminCenterIds != null && adminCenterIds.contains(item.getCenterId())) {
                        visibleList.add(item);
                    }
                }
            }
        } else {
            visibleList.addAll(allItems);
        }

        List<RecycleRequest> filtered = new ArrayList<>();
        List<RecycleRequest> chatFiltered = new ArrayList<>();
        boolean hasUnread = false;

        if (currentFilter.equals("Selected: All")) {
            filtered.addAll(visibleList);
        } else {
            for (RecycleRequest item : visibleList) {
                if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(currentFilter)) {
                    filtered.add(item);
                }
            }
        }
        
        for (RecycleRequest item : visibleList) {
            if ("Accepted".equalsIgnoreCase(item.getStatus()) || "Completed".equalsIgnoreCase(item.getStatus())) {
                chatFiltered.add(item);
                
                // Red dot logic for tab
                com.google.firebase.Timestamp lastMsg = item.getLastMessageTime();
                com.google.firebase.Timestamp lastRead = "Admin".equalsIgnoreCase(userRole) ? item.getLastReadAdmin() : item.getLastReadUser();
                if (lastMsg != null && (lastRead == null || lastMsg.compareTo(lastRead) > 0)) {
                    hasUnread = true;
                }
            }
        }

        if (dotUnreadTab != null) {
            dotUnreadTab.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }

        adapter.updateList(filtered);
        chatAdapter.updateList(chatFiltered);
        
        View noDataView = getView() != null ? getView().findViewById(R.id.tv_no_data) : null;
        if (noDataView != null) {
            noDataView.setVisibility(filtered.isEmpty() && chatFiltered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStatus(String requestId, String newStatus) {
        firestoreManager.updateRequestStatus(requestId, newStatus, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Status updated", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String error) {}
        });
    }

    private void showDetailPopup(RecycleRequest request) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_detail, null);
        
        ((TextView)view.findViewById(R.id.detail_category)).setText(request.getCategory());
        ((TextView)view.findViewById(R.id.detail_date)).setText("Date: " + request.getDate());
        
        String centerName = request.getCenterName();
        String displayCenter = (centerName == null || centerName.isEmpty() || ("MULTIPLE".equals(request.getCenterId()) && "Requesting".equalsIgnoreCase(request.getStatus()))) 
                ? "Multiple Centers" : centerName;
        ((TextView)view.findViewById(R.id.detail_center)).setText("Center: " + displayCenter);

        ((TextView)view.findViewById(R.id.detail_remarks)).setText("Remarks: " + request.getRemarks());
        ((TextView)view.findViewById(R.id.detail_address)).setText("Address: " + request.getAddress());
        ((TextView)view.findViewById(R.id.detail_status)).setText(request.getStatus());

        android.widget.ImageView ivDetail = view.findViewById(R.id.detail_image);
        if (ivDetail != null && request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            ivDetail.setVisibility(View.VISIBLE);
            my.edu.utar.RecycleGO.utils.ImageManager.loadImage(requireContext(), request.getPhotoUrl(), ivDetail);
        }

        Button btnEdit = view.findViewById(R.id.btn_action_edit);
        Button btnDelete = view.findViewById(R.id.btn_action_delete);
        Button btnAccept = view.findViewById(R.id.btn_action_accept);
        Button btnConfirmPickup = view.findViewById(R.id.btn_action_confirm_pickup);
        Button btnConfirmComplete = view.findViewById(R.id.btn_action_confirm_complete);
        Button btnReport = view.findViewById(R.id.btn_action_report);
        Button btnMsg = view.findViewById(R.id.btn_action_message);

        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        String s = request.getStatus();

        // Initial Reset
        btnEdit.setVisibility(View.GONE);
        btnDelete.setVisibility(View.VISIBLE); // Requirement: visible in all states
        btnAccept.setVisibility(View.GONE);
        btnConfirmPickup.setVisibility(View.GONE);
        btnConfirmComplete.setVisibility(View.GONE);
        btnReport.setVisibility(View.GONE);
        btnMsg.setVisibility(View.GONE);

        if ("Requesting".equalsIgnoreCase(s)) {
            btnEdit.setVisibility(View.VISIBLE);
            if (isAdmin) {
                btnAccept.setVisibility(View.VISIBLE);
                btnMsg.setVisibility(View.VISIBLE);
            }
        } else if ("Accepted".equalsIgnoreCase(s)) {
            btnMsg.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            if (isAdmin) btnConfirmPickup.setVisibility(View.VISIBLE);
        } else if ("Completed".equalsIgnoreCase(s)) {
            btnMsg.setVisibility(View.VISIBLE);
            btnReport.setVisibility(View.VISIBLE);
            if (!isAdmin) {
                btnConfirmComplete.setVisibility(View.VISIBLE);
            }
        } else if ("Verified".equalsIgnoreCase(s)) {
            btnMsg.setVisibility(View.VISIBLE);
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

        btnAccept.setOnClickListener(v -> {
            firestoreManager.getUser(userId, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null && !user.getJoinedCenters().isEmpty()) {
                        String centerId = user.getJoinedCenters().get(0);
                        String centerName = user.getRecycleCenter();
                        firestoreManager.acceptRequest(request.getId(), centerId, centerName, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                if (isAdded()) Toast.makeText(getContext(), "Request Accepted!", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFailure(String error) {}
                        });
                    }
                }
                @Override
                public void onFailure(String error) {}
            });
            dialog.dismiss();
        });
        btnConfirmPickup.setOnClickListener(v -> { updateStatus(request.getId(), "Completed"); dialog.dismiss(); });
        btnDelete.setOnClickListener(v -> {
            showConfirmDeleteDialog(request.getId(), false);
            dialog.dismiss();
        });
        
        btnMsg.setOnClickListener(v -> {
            openChat(request.getId());
            dialog.dismiss();
        });

        btnConfirmComplete.setOnClickListener(v -> {
            confirmComplete(request);
            dialog.dismiss();
        });

        btnReport.setOnClickListener(v -> {
            EditText input = new EditText(getContext());
            input.setHint("Enter report reason");
            input.setMaxLines(1);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Report Issue")
                    .setMessage("Please provide a brief reason for reporting this request:")
                    .setView(input)
                    .setPositiveButton("Submit", (dialogInterface, which) -> {
                        String reason = input.getText().toString().trim();
                        if (!reason.isEmpty()) {
                            firestoreManager.submitReport(request.getId(), userId, reason, new FirestoreManager.OnTaskCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    if (isAdded()) Toast.makeText(getContext(), "Report submitted successfully", Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void onFailure(String error) {
                                    if (isAdded()) Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void showConfirmDeleteDialog(String requestId, boolean isChat) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage(isChat ? "Are you sure you want to remove this chat from your list?" : "Are you sure you want to delete this recycle request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (isChat) {
                        for (int i = 0; i < allItems.size(); i++) {
                            if (allItems.get(i).getId().equals(requestId)) {
                                allItems.remove(i);
                                break;
                            }
                        }
                        Toast.makeText(getContext(), "Chat removed from list", Toast.LENGTH_SHORT).show();
                        applyFilter(); 
                    } else {
                        deleteRequest(requestId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmComplete(RecycleRequest request) {
        updateStatus(request.getId(), "Verified");
        
        // Point scoring only for regular users (if needed)
        // Since you want to remove it for Admin, I'm ensuring we check userRole or just omit it.
        if (!"Admin".equalsIgnoreCase(userRole)) {
            firestoreManager.addPoints(userId, 50, "Recycle", new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    if (isAdded()) Toast.makeText(getContext(), "Points earned! +50", Toast.LENGTH_LONG).show();
                }
                @Override
                public void onFailure(String error) {}
            });

            // Count materials
            String categories = request.getCategory();
            if (categories != null && !categories.isEmpty()) {
                String[] cats = categories.split(",\\s*");
                for (String cat : cats) {
                    firestoreManager.incrementMaterialCount(userId, cat, null);
                }
            }
        }
    }

    private void deleteRequest(String requestId) {
        firestoreManager.deleteRecycleRequest(requestId, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String error) {}
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
