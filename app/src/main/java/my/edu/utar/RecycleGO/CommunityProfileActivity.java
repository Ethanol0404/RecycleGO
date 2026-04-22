package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class CommunityProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView tvUsername, tvPostCount;
    private RecyclerView rvUserPosts;
    private FirestoreManager firestoreManager;
    private String userUid;
    private AdapterCommunityPost adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        userUid = getIntent().getStringExtra("uid");
        if (userUid == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestoreManager = new FirestoreManager();

        profileImage = findViewById(R.id.profile_image);
        tvUsername = findViewById(R.id.tv_username);
        tvPostCount = findViewById(R.id.tv_post_count);
        rvUserPosts = findViewById(R.id.rv_user_posts);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterCommunityPost(new ArrayList<>());
        rvUserPosts.setAdapter(adapter);

        loadUserData();
        loadUserPosts();
    }

    private void loadUserData() {
        firestoreManager.getUser(userUid, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null) {
                    tvUsername.setText(user.getUsername());
                    if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                        Glide.with(CommunityProfileActivity.this)
                            .load(user.getProfilePicUrl())
                            .placeholder(R.drawable.profile_circle)
                            .into(profileImage);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CommunityProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserPosts() {
        firestoreManager.getUserPosts(userUid, new FirestoreManager.OnListFetchListener<CommunityPost>() {
            @Override
            public void onListFetched(java.util.List<CommunityPost> list) {
                adapter.updateList(list);
                tvPostCount.setText(list.size() + " Posts");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CommunityProfileActivity.this, "Error loading posts: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
