package my.edu.utar.RecycleGO;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new AIViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            
            // Handle Text
            String text = message.getMessage();
            if (text == null || text.trim().isEmpty()) {
                userHolder.messageText.setVisibility(View.GONE);
            } else {
                userHolder.messageText.setVisibility(View.VISIBLE);
                userHolder.messageText.setText(
                        android.text.Html.fromHtml(
                                text,
                                android.text.Html.FROM_HTML_MODE_LEGACY
                        )
                );
            }

            // Handle Image
            if (message.hasImage()) {
                userHolder.imageCard.setVisibility(View.VISIBLE);
                if (message.getImageBitmap() != null) {
                    userHolder.messageImage.setImageBitmap(message.getImageBitmap());
                } else if (message.getImageUri() != null) {
                    userHolder.messageImage.setImageURI(message.getImageUri());
                }
            } else {
                userHolder.imageCard.setVisibility(View.GONE);
            }
        } else {
            ((AIViewHolder) holder).messageText.setText(
                    android.text.Html.fromHtml(
                            message.getMessage(),
                            android.text.Html.FROM_HTML_MODE_LEGACY
                    )
            );
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView messageImage;
        CardView imageCard;
        UserViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_user);
            messageImage = itemView.findViewById(R.id.image_message_user);
            imageCard = itemView.findViewById(R.id.card_message_image);
        }
    }

    static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        AIViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_ai);
        }
    }
}
