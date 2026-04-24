package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import my.edu.utar.RecycleGO.database.RecycleCenter;

public class AdapterRecycleCenter extends RecyclerView.Adapter<AdapterRecycleCenter.ViewHolder> {

    private List<RecycleCenter> centers;
    private OnItemClickListener listener;
    private boolean isAdmin = false;
    private List<String> joinedCenters = new java.util.ArrayList<>();
    private List<String> selectedCenterIds = new java.util.ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(RecycleCenter center);
        void onRecycleClick(RecycleCenter center);
    }

    public AdapterRecycleCenter(List<RecycleCenter> centers, OnItemClickListener listener) {
        this.centers = centers;
        this.listener = listener;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    public void setJoinedCenters(List<String> joinedCenters) {
        if (joinedCenters != null) {
            this.joinedCenters = joinedCenters;
            notifyDataSetChanged();
        }
    }

    public void setSelectedCenterIds(List<String> selectedCenterIds) {
        if (selectedCenterIds != null) {
            this.selectedCenterIds = selectedCenterIds;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_map_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecycleCenter center = centers.get(position);
        holder.title.setText(center.name);
        holder.description.setText(center.supportedServices);
        if (center.distance > 0) {
            holder.subDescription.setText(String.format("Hours: %s • %.2f km", center.operatingHours, center.distance / 1000.0));
        } else {
            holder.subDescription.setText("Hours: " + center.operatingHours);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(center));
        holder.btnMore.setOnClickListener(v -> {
            listener.onItemClick(center); // Trigger focus
        });

        if (isAdmin) {
            if (joinedCenters.contains(center.id)) {
                holder.btnRecycle.setText("Joined");
                holder.btnRecycle.setEnabled(false);
            } else {
                holder.btnRecycle.setText("Join");
                holder.btnRecycle.setEnabled(true);
            }
        } else {
            if (selectedCenterIds.contains(center.id)) {
                holder.btnRecycle.setText("Selected");
                holder.btnRecycle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
            } else {
                holder.btnRecycle.setText("Recycle");
                holder.btnRecycle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E6A5E")));
            }
            holder.btnRecycle.setEnabled(true);
        }
        
        holder.btnRecycle.setOnClickListener(v -> listener.onRecycleClick(center));
    }

    @Override
    public int getItemCount() { return centers.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, subDescription;
        Button btnMore, btnRecycle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.bubble_title);
            description = itemView.findViewById(R.id.bubble_description);
            subDescription = itemView.findViewById(R.id.bubble_subdescription);
            btnMore = itemView.findViewById(R.id.bubble_moreinfo);
            btnRecycle = itemView.findViewById(R.id.bubble_recycle);
        }
    }
}
