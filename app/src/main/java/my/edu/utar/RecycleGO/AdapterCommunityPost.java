package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class AdapterCommunityPost extends RecyclerView.Adapter<AdapterCommunityPost.PostViewHolder> {

    private List<CommunityPost> postList;
    private FirestoreManager firestoreManager;

    public AdapterCommunityPost(List<CommunityPost> postList) {
        this.postList = postList;
        this.firestoreManager = new FirestoreManager();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPost post = postList.get(position);

        holder.tvUsername.setText(post.getAuthorName());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(post.getTimestamp())));
        
        holder.tvContent.setText(post.getContent());
        holder.tvLikes.setText(String.valueOf(post.getLikes()));
        holder.tvComments.setText(String.valueOf(post.getCommentsCount()));

        // Handle Image
        if (post.getPhotoUrl() != null && !post.getPhotoUrl().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            File imgFile = ImageManager.getImageFile(holder.itemView.getContext(), post.getPhotoUrl());
            if (imgFile != null && imgFile.exists()) {
                Glide.with(holder.itemView.getContext())
                    .load(imgFile)
                    .into(holder.ivPostImage);
            } else {
                // For demo, if file not found locally, maybe show a placeholder
                holder.ivPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Profile Click
        holder.profileArea.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), CommunityProfileActivity.class);
            intent.putExtra("uid", post.getAuthorUID());
            holder.itemView.getContext().startActivity(intent);
        });

        // Like Toggle (Simplified)
        holder.btnLike.setOnClickListener(v -> {
            // Optimistic UI update
            int newLikes = post.getLikes() + 1;
            post.setLikes(newLikes);
            holder.tvLikes.setText(String.valueOf(newLikes));
            
            firestoreManager.toggleLike(post.getPostID(), true, new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {}
                @Override
                public void onFailure(String error) {
                    // Revert on failure
                    post.setLikes(post.getLikes() - 1);
                    holder.tvLikes.setText(String.valueOf(post.getLikes()));
                }
            });
        });

        // Comment Button
        View.OnClickListener openComments = v -> {
            CommentBottomSheet bottomSheet = new CommentBottomSheet(post.getPostID(), post.getAuthorName());
            bottomSheet.show(((AppCompatActivity)holder.itemView.getContext()).getSupportFragmentManager(), "CommentBottomSheet");
        };
        holder.btnComment.setOnClickListener(openComments);
        holder.btnAddComment.setOnClickListener(openComments);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void updateList(List<CommunityPost> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvDate, tvContent, tvLikes, tvComments, btnAddComment;
        ImageView ivPostImage, btnLike, btnComment;
        View profileArea;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.post_username);
            tvDate = itemView.findViewById(R.id.post_date);
            tvContent = itemView.findViewById(R.id.post_text);
            tvLikes = itemView.findViewById(R.id.tv_likes);
            tvComments = itemView.findViewById(R.id.tv_comments);
            ivPostImage = itemView.findViewById(R.id.post_image);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnAddComment = itemView.findViewById(R.id.btn_add_comment);
            profileArea = itemView.findViewById(R.id.post_avatar);
        }
    }
}