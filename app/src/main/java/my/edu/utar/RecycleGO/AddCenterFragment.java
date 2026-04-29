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

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleCenter;

public class AddCenterFragment extends Fragment {

    private EditText etName, etAddress, etLat, etLon, etHours, etServices, etPhone, etPic;
    private Button btnSave;
    private FirestoreManager firestoreManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_center, container, false);
        
        firestoreManager = new FirestoreManager();
        
        etName = view.findViewById(R.id.et_center_name);
        etAddress = view.findViewById(R.id.et_center_address);
        etLat = view.findViewById(R.id.et_center_lat);
        etLon = view.findViewById(R.id.et_center_lon);
        etHours = view.findViewById(R.id.et_center_hours);
        etServices = view.findViewById(R.id.et_center_services);
        etPhone = view.findViewById(R.id.et_center_phone);
        etPic = view.findViewById(R.id.et_center_pic);
        btnSave = view.findViewById(R.id.btn_save_center);
        
        btnSave.setOnClickListener(v -> saveCenter());
        
        return view;
    }

    private void saveCenter() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        RecycleCenter center = new RecycleCenter();
        center.name = name;
        center.address = etAddress.getText().toString().trim();
        try {
            center.latitude = Double.parseDouble(etLat.getText().toString().trim());
            center.longitude = Double.parseDouble(etLon.getText().toString().trim());
        } catch (NumberFormatException e) {
            center.latitude = 0;
            center.longitude = 0;
        }
        center.operatingHours = etHours.getText().toString().trim();
        center.supportedServices = etServices.getText().toString().trim();
        center.phoneNumber = etPhone.getText().toString().trim();
        center.pictureUrl = etPic.getText().toString().trim();
        
        // Generate a new ID if needed or let addRecycleCenter handle it
        // To ensure it's "saved by ID" as requested:
        firestoreManager.addRecycleCenter(center, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Center added successfully with ID: " + center.id, Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) Toast.makeText(getContext(), "Failed to add center: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
