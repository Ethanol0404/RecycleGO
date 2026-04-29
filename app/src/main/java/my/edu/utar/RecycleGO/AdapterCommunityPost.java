package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.CommunityPost;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class AdapterCommunityPost extends RecyclerView.Adapter<AdapterCommunityPost.PostViewHolder> {

    private List<CommunityPost> postList;
    private final FirestoreManager firestoreManager;

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

        String currentUserId = FirebaseAuth.getInstance().getUid();
        boolean isLiked = currentUserId != null && post.getLikedBy() != null && post.getLikedBy().contains(currentUserId);

        // Update Like Button appearance
        if (isLiked) {
            holder.btnLike.setColorFilter(Color.RED); // Or any color indicating liked state
        } else {
            holder.btnLike.setColorFilter(Color.parseColor("#757575")); // Default gray
        }

        // Fetch and load author's profile picture
        firestoreManager.getUser(post.getAuthorUID(), new FirestoreManager.OnUserFetchListener() {
            @Override
            public void onUserFetched(UserRecord user) {
                if (user != null && user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(user.getProfilePicUrl())
                            .placeholder(R.drawable.useravatar2)
                            .into(holder.ivAvatar);
                } else {
                    holder.ivAvatar.setImageResource(R.drawable.useravatar2);
                }
            }
            @Override
            public void onFailure(String error) {}
        });

        // Handle Post Image
        if (post.getPhotoUrl() != null && !post.getPhotoUrl().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            ImageManager.loadImage(holder.itemView.getContext(), post.getPhotoUrl(), holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Profile Click
        holder.ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), CommunityProfileActivity.class);
            intent.putExtra("uid", post.getAuthorUID());
            holder.itemView.getContext().startActivity(intent);
        });

        // Like Toggle
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == null) return;

            boolean currentlyLiked = post.getLikedBy() != null && post.getLikedBy().contains(currentUserId);
            boolean newLikedState = !currentlyLiked;

            // Optimistic UI Update
            if (newLikedState) {
                post.setLikes(post.getLikes() + 1);
                if (post.getLikedBy() != null) post.getLikedBy().add(currentUserId);
                holder.btnLike.setColorFilter(Color.RED);
            } else {
                post.setLikes(post.getLikes() - 1);
                if (post.getLikedBy() != null) post.getLikedBy().remove(currentUserId);
                holder.btnLike.setColorFilter(Color.parseColor("#757575"));
            }
            holder.tvLikes.setText(String.valueOf(post.getLikes()));

            firestoreManager.toggleLike(post.getPostID(), newLikedState, currentUserId, new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {}
                @Override
                public void onFailure(String error) {
                    // Revert UI on failure
                    if (newLikedState) {
                        post.setLikes(post.getLikes() - 1);
                        if (post.getLikedBy() != null) post.getLikedBy().remove(currentUserId);
                        holder.btnLike.setColorFilter(Color.parseColor("#757575"));
                    } else {
                        post.setLikes(post.getLikes() + 1);
                        if (post.getLikedBy() != null) post.getLikedBy().add(currentUserId);
                        holder.btnLike.setColorFilter(Color.RED);
                    }
                    holder.tvLikes.setText(String.valueOf(post.getLikes()));
                }
            });
        });

        // Comment Button
        View.OnClickListener openComments = v -> {
            CommentBottomSheet bottomSheet = new CommentBottomSheet(post.getPostID());
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
        CircleImageView ivAvatar;

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
            ivAvatar = itemView.findViewById(R.id.post_avatar);
        }
    }
}
