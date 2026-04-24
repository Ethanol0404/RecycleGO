package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class AddCommunityFragment extends Fragment {

    private EditText etName, etIcon;
    private Button btnSave;
    private FirestoreManager firestoreManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_community, container, false);
        
        firestoreManager = new FirestoreManager();
        
        etName = view.findViewById(R.id.et_comm_name);
        etIcon = view.findViewById(R.id.et_comm_icon);
        btnSave = view.findViewById(R.id.btn_save_community);
        
        btnSave.setOnClickListener(v -> saveCommunity());
        
        return view;
    }

    private void saveCommunity() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        CommunityModel community = new CommunityModel();
        community.setName(name);
        community.setIconUrl(etIcon.getText().toString().trim());

        firestoreManager.addCommunity(community, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Community created", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
