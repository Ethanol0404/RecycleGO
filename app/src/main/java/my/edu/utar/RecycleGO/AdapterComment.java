package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import my.edu.utar.RecycleGO.database.CommunityComment;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.ViewHolder> {

    private List<CommunityComment> commentList;

    public AdapterComment(List<CommunityComment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityComment comment = commentList.get(position);
        holder.tvAuthor.setText(comment.getAuthorName());
        holder.tvText.setText(comment.getText());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(comment.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void updateList(List<CommunityComment> newList) {
        this.commentList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvText, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.comment_author);
            tvText = itemView.findViewById(R.id.comment_text);
            tvDate = itemView.findViewById(R.id.comment_date);
        }
    }
}
