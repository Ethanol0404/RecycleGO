package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class HomeFragment extends Fragment {

    private TextView txtPoints, txtUserName, txtEmail, txtTotalRecycled, txtTotalPoints;
    private FirestoreManager firestoreManager;
    private FirebaseAuth auth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化视图
        txtPoints = view.findViewById(R.id.txt_points);
        txtUserName = view.findViewById(R.id.txt_user_name);
        txtEmail = view.findViewById(R.id.txt_email);
        txtTotalRecycled = view.findViewById(R.id.txt_total_recycled);
        txtTotalPoints = view.findViewById(R.id.txt_total_points);

        // 初始化 FirestoreManager 和 Firebase Auth
        firestoreManager = new FirestoreManager();
        auth = FirebaseAuth.getInstance();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        // 获取当前登录用户
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            // 未登录状态
            txtUserName.setText("Guest User");
            txtPoints.setText("0 pts");
            txtEmail.setText("Not logged in");
            txtTotalRecycled.setText("0 times");
            txtTotalPoints.setText("0 pts");
            return;
        }

        currentUserId = user.getUid();

        // 从 Firestore 获取用户数据
        firestoreManager.getUser(currentUserId, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord userRecord) {
                if (userRecord != null) {
                    // 用户存在，显示数据
                    String userName = userRecord.getUsername();
                    String email = userRecord.getEmail();
                    int points = userRecord.getPoints();
                    int totalRecycled = userRecord.getTotalRecycled();

                    txtUserName.setText(userName != null ? userName : "Recycler");
                    txtEmail.setText(email != null ? email : "");
                    txtPoints.setText(points + " pts");
                    txtTotalRecycled.setText(totalRecycled + " times");
                    txtTotalPoints.setText(points + " pts");

                } else {
                    // 新用户，创建默认数据
                    createNewUser();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "加载失败: " + error, Toast.LENGTH_SHORT).show();
                setDefaultValues();
            }
        });
    }

    private void createNewUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String username = email != null ? email.split("@")[0] : "Recycler";

        UserRecord newUser = new UserRecord(
                currentUserId,
                username,
                email != null ? email : "",
                "",
                "User"
        );
        newUser.setPoints(0);
        newUser.setTotalRecycled(0);

        firestoreManager.saveUser(newUser, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                txtUserName.setText(username);
                txtEmail.setText(email != null ? email : "");
                txtPoints.setText("0 pts");
                txtTotalRecycled.setText("0 times");
                txtTotalPoints.setText("0 pts");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "创建用户失败: " + error, Toast.LENGTH_SHORT).show();
                setDefaultValues();
            }
        });
    }

    private void setDefaultValues() {
        txtUserName.setText("Recycler");
        txtEmail.setText("");
        txtPoints.setText("0 pts");
        txtTotalRecycled.setText("0 times");
        txtTotalPoints.setText("0 pts");
    }
}