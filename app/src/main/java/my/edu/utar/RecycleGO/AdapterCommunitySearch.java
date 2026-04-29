package my.edu.utar.RecycleGO;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class AdapterCommunitySearch extends RecyclerView.Adapter<AdapterCommunitySearch.ViewHolder> {

    private List<CommunityModel> communityList;
    private List<String> subscribedCommunities;
    private String userUid;
    private FirestoreManager firestoreManager;

    public AdapterCommunitySearch(List<CommunityModel> communityList, String userUid) {
        this.communityList = communityList;
        this.userUid = userUid;
        this.firestoreManager = new FirestoreManager();
        this.subscribedCommunities = new ArrayList<>();
    }

    public void setSubscribedCommunities(List<String> subscribedCommunities) {
        this.subscribedCommunities = subscribedCommunities != null ? subscribedCommunities : new ArrayList<>();
        notifyDataSetChanged();
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
        
        // Use R.drawable.place for community icon loading as per request
        ImageManager.loadImage(holder.itemView.getContext(), community.getIconUrl(), holder.ivIcon, R.drawable.place);

        boolean isSubscribed = subscribedCommunities.contains(community.getCommunityID());

        if (isSubscribed) {
            holder.btnSubscribe.setText("Subscribed");
            holder.btnSubscribe.setOnClickListener(v -> showUnsubscribeConfirmation(holder, community));
        } else {
            holder.btnSubscribe.setText("Subscribe");
            holder.btnSubscribe.setOnClickListener(v -> subscribe(holder, community));
        }
    }

    private void subscribe(ViewHolder holder, CommunityModel community) {
        firestoreManager.subscribeToCommunity(userUid, community.getCommunityID(), new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(holder.itemView.getContext(), "Subscribed to " + community.getName(), Toast.LENGTH_SHORT).show();
                subscribedCommunities.add(community.getCommunityID());
                notifyItemChanged(holder.getAdapterPosition());
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(holder.itemView.getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUnsubscribeConfirmation(ViewHolder holder, CommunityModel community) {
        new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Unsubscribe")
                .setMessage("Are you sure you want to unsubscribe from " + community.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> unsubscribe(holder, community))
                .setNegativeButton("No", null)
                .show();
    }

    private void unsubscribe(ViewHolder holder, CommunityModel community) {
        firestoreManager.unsubscribeFromCommunity(userUid, community.getCommunityID(), new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(holder.itemView.getContext(), "Unsubscribed from " + community.getName(), Toast.LENGTH_SHORT).show();
                subscribedCommunities.remove(community.getCommunityID());
                notifyItemChanged(holder.getAdapterPosition());
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(holder.itemView.getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
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
