package my.edu.utar.RecycleGO;

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
import java.util.stream.Collectors;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class CommunityAdminFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdapterManageCommunity adapter;
    private FirestoreManager firestoreManager;
    private String currentUid;
    private View noDataView;
    private android.net.Uri selectedIconUri;
    private android.widget.ImageView dialogIconPreview;

    private final androidx.activity.result.ActivityResultLauncher<String> pickIconLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedIconUri = uri;
                    if (dialogIconPreview != null) {
                        dialogIconPreview.setImageURI(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_community, container, false);
        
        firestoreManager = new FirestoreManager();
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        currentUid = prefs.getString("loggedInUid", "");

        recyclerView = view.findViewById(R.id.rv_manage_communities);
        noDataView = view.findViewById(R.id.tv_no_communities);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_community);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterManageCommunity(new ArrayList<>(), new AdapterManageCommunity.OnCommunityActionListener() {
            @Override
            public void onEdit(CommunityModel community) {
                showAddEditDialog(community);
            }

            @Override
            public void onDelete(CommunityModel community) {
                showDeleteConfirmationDialog(community);
            }

            @Override
            public void onItemClick(CommunityModel community) {
                // Optional: view community details
            }
        });
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddEditDialog(null));
        
        loadMyCommunities();
        
        return view;
    }

    private void loadMyCommunities() {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                List<CommunityModel> myComms = list.stream()
                        .filter(c -> currentUid.equals(c.getCreatorUid()))
                        .collect(Collectors.toList());
                adapter.updateList(myComms);
                noDataView.setVisibility(myComms.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog(CommunityModel community) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Community")
                .setMessage("Are you sure you want to delete '" + community.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestoreManager.deleteCommunity(community.getCommunityID(), new FirestoreManager.OnTaskCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Community deleted", Toast.LENGTH_SHORT).show();
                            loadMyCommunities();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(getContext(), "Failed to delete: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showAddEditDialog(@Nullable CommunityModel existing) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_edit_community, null);
        
        EditText etName = view.findViewById(R.id.dialog_comm_name);
        EditText etIcon = view.findViewById(R.id.dialog_comm_icon);
        dialogIconPreview = view.findViewById(R.id.dialog_comm_icon_preview);
        android.widget.Button btnPick = view.findViewById(R.id.btn_pick_comm_icon);
        
        selectedIconUri = null;

        if (existing != null) {
            builder.setTitle("Edit Community");
            etName.setText(existing.getName());
            etIcon.setText(existing.getIconUrl());
            // Load existing icon into preview
            my.edu.utar.RecycleGO.utils.ImageManager.loadImage(requireContext(), existing.getIconUrl(), dialogIconPreview);
        } else {
            builder.setTitle("New Community");
        }
        
        btnPick.setOnClickListener(v -> pickIconLauncher.launch("image/*"));
        
        builder.setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String icon = etIcon.getText().toString().trim();
                    
                    if (selectedIconUri != null) {
                        icon = my.edu.utar.RecycleGO.utils.ImageManager.saveImageToInternalStorage(requireContext(), selectedIconUri);
                    }
                    
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (existing == null) {
                        CommunityModel newComm = new CommunityModel();
                        newComm.setName(name);
                        newComm.setIconUrl(icon);
                        newComm.setCreatorUid(currentUid);
                        firestoreManager.addCommunity(newComm, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Community created", Toast.LENGTH_SHORT).show();
                                loadMyCommunities();
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        existing.setName(name);
                        existing.setIconUrl(icon);
                        firestoreManager.updateCommunity(existing, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Community updated", Toast.LENGTH_SHORT).show();
                                loadMyCommunities();
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
