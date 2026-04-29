package my.edu.utar.RecycleGO;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
    private String manualIconUrl = null;

    private final androidx.activity.result.ActivityResultLauncher<String> pickIconLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedIconUri = uri;
                    manualIconUrl = null;
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
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
        
        applyCustomTheme(view);
        loadMyCommunities();
        
        return view;
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bottomColor = Color.parseColor(bottomColorCode);

        TextView tvTitle = view.findViewById(R.id.tv_manage_title);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_community);

        if (tvTitle != null) tvTitle.setTextColor(bottomColor);
        if (fabAdd != null) fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_edit_community, null);
        
        EditText etName = view.findViewById(R.id.dialog_comm_name);
        EditText etIcon = view.findViewById(R.id.dialog_comm_icon);
        dialogIconPreview = view.findViewById(R.id.dialog_comm_icon_preview);
        Button btnPick = view.findViewById(R.id.btn_pick_comm_icon);
        
        // Theme Support for Dialog
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String headerColorCode = prefs.getString("header_color", "#def9ac");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int headerColor = Color.parseColor(headerColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        view.setBackgroundColor(headerColor);
        
        if (etName != null) {
            etName.setTextColor(bottomColor);
            etName.setHintTextColor(Color.argb(128, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor)));
        }
        if (btnPick != null) {
            btnPick.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
            btnPick.setTextColor(Color.WHITE);
        }

        ViewGroup root = (ViewGroup) view;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView && !(child instanceof EditText)) {
                ((TextView) child).setTextColor(bottomColor);
            }
        }

        selectedIconUri = null;
        manualIconUrl = null;

        if (existing != null) {
            etName.setText(existing.getName());
            etIcon.setText(existing.getIconUrl());
            manualIconUrl = existing.getIconUrl();
            my.edu.utar.RecycleGO.utils.ImageManager.loadImage(requireContext(), existing.getIconUrl(), dialogIconPreview, R.drawable.place);
        }
        
        btnPick.setOnClickListener(v -> showIconSourceOptions(etIcon));

        builder.setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(bottomColor);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(bottomColor);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String icon = etIcon.getText().toString().trim();
            
            if (selectedIconUri != null) {
                icon = my.edu.utar.RecycleGO.utils.ImageManager.saveImageToInternalStorage(requireContext(), selectedIconUri);
            } else if (manualIconUrl != null) {
                icon = manualIconUrl;
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
                        dialog.dismiss();
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
                        dialog.dismiss();
                    }
                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showIconSourceOptions(EditText etIconField) {
        String[] options = {"Choose from Gallery", "Insert Image URL"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Icon Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickIconLauncher.launch("image/*");
                    } else {
                        showUrlInputDialog(etIconField);
                    }
                })
                .show();
    }

    private void showUrlInputDialog(EditText etIconField) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_url_input, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Theme Support
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String headerColorCode = prefs.getString("header_color", "#def9ac");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int headerColor = Color.parseColor(headerColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        dialogView.setBackgroundColor(headerColor);
        
        TextView title = dialogView.findViewById(R.id.dialog_title);
        EditText input = dialogView.findViewById(R.id.dialog_input);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        if (title != null) title.setTextColor(bottomColor);
        if (input != null) {
            input.setTextColor(bottomColor);
            input.setHintTextColor(Color.argb(128, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor)));
        }
        if (btnOk != null) btnOk.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bottomColor));
        if (btnCancel != null) btnCancel.setTextColor(bottomColor);

        btnOk.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                manualIconUrl = url;
                selectedIconUri = null;
                etIconField.setText(url);
                Glide.with(this).load(url).placeholder(R.drawable.place).into(dialogIconPreview);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        
        // Fix "so small" issue: ensure dialog takes up appropriate width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
