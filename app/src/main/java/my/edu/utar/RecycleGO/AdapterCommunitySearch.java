package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.FirestoreManager;

public class AdapterCommunitySearch extends RecyclerView.Adapter<AdapterCommunitySearch.ViewHolder> {

    private List<CommunityModel> communityList;
    private String userUid;
    private FirestoreManager firestoreManager;

    public AdapterCommunitySearch(List<CommunityModel> communityList, String userUid) {
        this.communityList = communityList;
        this.userUid = userUid;
        this.firestoreManager = new FirestoreManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityModel community = communityList.get(position);
        holder.tvName.setText(community.getName());
        
        // Use Glide for safe loading into ImageView
        if (community.getIconUrl() != null && !community.getIconUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(community.getIconUrl())
                .placeholder(R.drawable.profile_circle)
                .error(R.drawable.profile_circle)
                .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageResource(R.drawable.profile_circle);
        }

        holder.btnSubscribe.setOnClickListener(v -> {
            firestoreManager.subscribeToCommunity(userUid, community.getCommunityID(), new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(holder.itemView.getContext(), "Subscribed to " + community.getName(), Toast.LENGTH_SHORT).show();
                    holder.btnSubscribe.setText("Subscribed");
                    holder.btnSubscribe.setEnabled(false);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(holder.itemView.getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return communityList.size();
    }

    public void updateList(List<CommunityModel> newList) {
        this.communityList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        Button btnSubscribe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.community_icon);
            tvName = itemView.findViewById(R.id.community_name);
            btnSubscribe = itemView.findViewById(R.id.btn_subscribe);
        }
    }
}
