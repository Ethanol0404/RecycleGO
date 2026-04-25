package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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

        listenForMessages();
        markAsRead();
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
                markAsRead(); // Update last read when sending too
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

                        // If activity is in foreground, mark as read
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