package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import my.edu.utar.RecycleGO.database.CommunityModel;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class AdapterManageCommunity extends RecyclerView.Adapter<AdapterManageCommunity.ViewHolder> {

    private List<CommunityModel> list;
    private OnCommunityActionListener listener;

    public interface OnCommunityActionListener {
        void onEdit(CommunityModel community);
        void onDelete(CommunityModel community);
        void onItemClick(CommunityModel community);
    }

    public AdapterManageCommunity(List<CommunityModel> list, OnCommunityActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<CommunityModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityModel community = list.get(position);
        holder.tvName.setText(community.getName());
        holder.tvId.setText("ID: " + community.getCommunityID());

        // Use R.drawable.place as default for community icon
        ImageManager.loadImage(holder.itemView.getContext(), community.getIconUrl(), holder.imgIcon, R.drawable.place);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(community);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(community);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(community);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName, tvId;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.manage_comm_icon);
            tvName = itemView.findViewById(R.id.manage_comm_name);
            tvId = itemView.findViewById(R.id.manage_comm_id);
            btnEdit = itemView.findViewById(R.id.btn_edit_community);
            btnDelete = itemView.findViewById(R.id.btn_delete_community);
        }
    }
}
