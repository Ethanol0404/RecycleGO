package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class CommunitySearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvResults;
    private AdapterCommunitySearch adapter;
    private FirestoreManager firestoreManager;
    private List<CommunityModel> allCommunities = new ArrayList<>();
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_search);

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        userUid = prefs.getString("loggedInUid", "");
        firestoreManager = new FirestoreManager();

        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_search_results);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        adapter = new AdapterCommunitySearch(new ArrayList<>(), userUid);
        rvResults.setAdapter(adapter);

        loadUserData();
        loadCommunities();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserData() {
        if (userUid.isEmpty()) return;
        firestoreManager.getUser(userUid, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null) {
                    adapter.setSubscribedCommunities(user.getSubscribedCommunities());
                }
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    private void loadCommunities() {
        firestoreManager.getAllCommunities(new FirestoreManager.OnListFetchListener<CommunityModel>() {
            @Override
            public void onListFetched(List<CommunityModel> list) {
                allCommunities = list;
                adapter.updateList(list);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CommunitySearchActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        List<CommunityModel> filteredList = allCommunities.stream()
                .filter(c -> c.getName().toLowerCase().contains(text.toLowerCase()))
                .collect(Collectors.toList());
        adapter.updateList(filteredList);
    }
}
