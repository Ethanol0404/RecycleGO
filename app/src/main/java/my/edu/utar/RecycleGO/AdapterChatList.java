package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.List;

import my.edu.utar.RecycleGO.database.RecycleRequest;

public class AdapterChatList extends RecyclerView.Adapter<AdapterChatList.ViewHolder> {
    private List<RecycleRequest> chats;
    private OnChatActionListener listener;
    private String userRole;

    public interface OnChatActionListener {
        void onChatClick(RecycleRequest request);
        void onDeleteChat(RecycleRequest request);
    }

    public AdapterChatList(List<RecycleRequest> chats, String userRole, OnChatActionListener listener) {
        this.chats = chats;
        this.userRole = userRole;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecycleRequest req = chats.get(position);
        holder.tvTitle.setText("Request: " + req.getCategory());
        holder.tvLastMsg.setText("ID: " + (req.getId().length() > 8 ? req.getId().substring(0, 8) : req.getId()));

        // Unread Dot Logic
        Timestamp lastMsg = req.getLastMessageTime();
        Timestamp lastRead = "Admin".equals(userRole) ? req.getLastReadAdmin() : req.getLastReadUser();

        if (lastMsg != null) {
            if (lastRead == null || lastMsg.compareTo(lastRead) > 0) {
                holder.dotUnread.setVisibility(View.VISIBLE);
            } else {
                holder.dotUnread.setVisibility(View.GONE);
            }
        } else {
            holder.dotUnread.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(req));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteChat(req));
    }

    @Override
    public int getItemCount() { return chats.size(); }

    public void updateList(List<RecycleRequest> newList) {
        this.chats = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLastMsg;
        ImageButton btnDelete;
        View dotUnread;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.chat_request_title);
            tvLastMsg = v.findViewById(R.id.chat_last_message);
            btnDelete = v.findViewById(R.id.btn_delete_chat);
            dotUnread = v.findViewById(R.id.dot_unread_item);
        }
    }
}