package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class AdapterCommunityProfile extends RecyclerView.Adapter<AdapterCommunityProfile.ViewHolder> {

    private List<CommunityModel> communityList;
    private OnCommunityClickListener listener;

    public interface OnCommunityClickListener {
        void onCommunityClick(CommunityModel community);
    }

    public AdapterCommunityProfile(List<CommunityModel> communityList, OnCommunityClickListener listener) {
        this.communityList = communityList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_profilelist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityModel community = communityList.get(position);
        holder.tvName.setText(community.getName());

        // Use R.drawable.place as default for community icon
        ImageManager.loadImage(holder.itemView.getContext(), community.getIconUrl(), holder.ivIcon, R.drawable.place);

        holder.itemView.setOnClickListener(v -> listener.onCommunityClick(community));
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
        CircleImageView ivIcon;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.community_icon);
            tvName = itemView.findViewById(R.id.community_name);
        }
    }
}
