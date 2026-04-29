package my.edu.utar.RecycleGO;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import my.edu.utar.RecycleGO.database.DirectMessage;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class ChatDirectActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterDirectChat adapter;
    private List<DirectMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvTitle;

    private FirestoreManager firestoreManager;
    private String requestId, userId, username, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_chat_direct);

        requestId = getIntent().getStringExtra("requestId");
        if (requestId == null) {
            Toast.makeText(this, "Error: No request ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestoreManager = new FirestoreManager();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("loggedInUid", "");
        username = prefs.getString("loggedInUsername", "User");
        userRole = prefs.getString("loggedInRole", "User");

        recyclerView = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.chat_title);

        tvTitle.setText("Request Chat (" + (requestId.length() > 5 ? requestId.substring(0, 5) : requestId) + ")");

        adapter = new AdapterDirectChat(messages, userId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });

        applyCustomTheme();
        listenForMessages();
        markAsRead();
    }

    private void applyCustomTheme() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String bgColorCode = prefs.getString("theme_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bgColor = Color.parseColor(bgColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        // Set Status Bar and Navigation Bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(bottomColor);
        window.setNavigationBarColor(bottomColor);

        findViewById(android.R.id.content).getRootView().setBackgroundColor(bgColor);
        
        android.view.View toolbar = findViewById(R.id.chat_toolbar);
        android.view.View inputArea = findViewById(R.id.layout_input);
        
        if (toolbar != null) toolbar.setBackgroundColor(bottomColor);
        if (inputArea != null) inputArea.setBackgroundColor(bottomColor);
        if (recyclerView != null) recyclerView.setBackgroundColor(bgColor);
    }

    private void markAsRead() {
        firestoreManager.updateLastRead(requestId, userRole, null);
    }

    private void sendMessage(String text) {
        DirectMessage msg = new DirectMessage(userId, username, text);
        firestoreManager.sendMessage(requestId, msg, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                etMessage.setText("");
                markAsRead();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(ChatDirectActivity.this, "Error sending: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForMessages() {
        firestoreManager.getChatMessages(requestId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<DirectMessage> newList = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            newList.add(doc.toObject(DirectMessage.class));
                        }
                        adapter.updateList(newList);
                        recyclerView.scrollToPosition(newList.size() - 1);
                        markAsRead();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        markAsRead();
    }
}
