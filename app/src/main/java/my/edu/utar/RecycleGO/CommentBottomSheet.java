package my.edu.utar.RecycleGO;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import my.edu.utar.RecycleGO.database.CommunityComment;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class CommentBottomSheet extends BottomSheetDialogFragment {

    private String postID;
    private String authorName; // Logged in user name
    private RecyclerView rvComments;
    private EditText etComment;
    private AdapterComment adapter;
    private FirestoreManager firestoreManager;

    public CommentBottomSheet(String postID, String authorName) {
        this.postID = postID;
        this.authorName = authorName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_comment, container, false);

        firestoreManager = new FirestoreManager();
        rvComments = view.findViewById(R.id.rv_comments);
        etComment = view.findViewById(R.id.et_comment);
        ImageButton btnSend = view.findViewById(R.id.btn_send_comment);

        adapter = new AdapterComment(new ArrayList<>());
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(adapter);

        loadComments();

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                addComment(text);
            }
        });

        return view;
    }

    private void loadComments() {
        firestoreManager.getComments(postID, new FirestoreManager.OnListFetchListener<CommunityComment>() {
            @Override
            public void onListFetched(List<CommunityComment> list) {
                adapter.updateList(list);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addComment(String text) {
        String commentID = UUID.randomUUID().toString();
        CommunityComment comment = new CommunityComment(commentID, postID, authorName, text);

        firestoreManager.addComment(comment, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                etComment.setText("");
                loadComments();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error adding comment: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
