package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.DirectMessage;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class AdapterDirectChat extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private List<DirectMessage> messages;
    private String currentUserId;
    private FirestoreManager firestoreManager;

    public AdapterDirectChat(List<DirectMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.firestoreManager = new FirestoreManager();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DirectMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            sentHolder.tvMessage.setText(msg.getMessage());
            loadAvatar(msg.getSenderId(), sentHolder.ivAvatar);
      } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            receivedHolder.tvMessage.setText(msg.getMessage());
            loadAvatar(msg.getSenderId(), receivedHolder.ivAvatar);
        }
    }

    private void loadAvatar(String uid, CircleImageView iv) {
        firestoreManager.getUser(uid, new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null && user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                    Glide.with(iv.getContext())
                            .load(user.getProfilePicUrl())
                            .placeholder(R.drawable.useravatar)
                            .into(iv);
                } else {
                    // Fallback icons
                    if (uid.equals(currentUserId)) {
                        iv.setImageResource(R.drawable.useravatar);
                    } else {
                        // If we are regular user and receiver is admin
                        iv.setImageResource(R.drawable.aiavatar);
                    }
                }
            }
            @Override
            public void onFailure(String error) {}
        });
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public void updateList(List<DirectMessage> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        CircleImageView ivAvatar;
        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.text_message_user);
            ivAvatar = v.findViewById(R.id.image_user_avatar);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        CircleImageView ivAvatar;
        ReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.text_message_ai);
            ivAvatar = v.findViewById(R.id.image_ai_avatar);
        }
    }
}
