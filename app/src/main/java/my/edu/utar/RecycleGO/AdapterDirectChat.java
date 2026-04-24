package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import my.edu.utar.RecycleGO.database.DirectMessage;

public class AdapterDirectChat extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private List<DirectMessage> messages;
    private String currentUserId;

    public AdapterDirectChat(List<DirectMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
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
            ((SentViewHolder) holder).tvMessage.setText(msg.getMessage());
        } else {
            ((ReceivedViewHolder) holder).tvMessage.setText(msg.getMessage());
            ((ReceivedViewHolder) holder).tvSender.setText(msg.getSenderName());
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public void updateList(List<DirectMessage> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.text_message_user);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvSender;
        ReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.text_message_ai);
            tvSender = v.findViewById(R.id.welcome_text); // Reusing AI layout but might need tweaking
        }
    }
}
