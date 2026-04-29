package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
    private List<String> requestedCenters = new java.util.ArrayList<>();
    private List<String> selectedCenterIds = new java.util.ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(RecycleCenter center);
        void onRecycleClick(RecycleCenter center);
        void onViewRequestsClick(RecycleCenter center);
        void onDetailsClick(RecycleCenter center);
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

    public void setRequestedCenters(List<String> requestedCenters) {
        if (requestedCenters != null) {
            this.requestedCenters = requestedCenters;
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
            listener.onDetailsClick(center);
        });

        // Theme Support
        SharedPreferences appPrefs = holder.itemView.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bottomColorCode = appPrefs.getString("bottom_color", "#265200");
        int bottomColor = Color.parseColor(bottomColorCode);
        
        holder.title.setTextColor(bottomColor);
        holder.btnMore.setBackgroundTintList(ColorStateList.valueOf(bottomColor));

        if (isAdmin) {
            if (joinedCenters.contains(center.id)) {
                holder.btnRecycle.setText("Joined");
                holder.btnRecycle.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                holder.btnRecycle.setEnabled(true);
                holder.btnViewRequests.setVisibility(View.VISIBLE);
                holder.btnViewRequests.setTextColor(bottomColor);
            } else if (requestedCenters.contains(center.id)) {
                holder.btnRecycle.setText("Requested");
                holder.btnRecycle.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                holder.btnRecycle.setEnabled(true);
                holder.btnViewRequests.setVisibility(View.GONE);
            } else {
                holder.btnRecycle.setText("Join");
                holder.btnRecycle.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
                holder.btnRecycle.setEnabled(true);
                holder.btnViewRequests.setVisibility(View.GONE);
            }
        } else {
            if (selectedCenterIds.contains(center.id)) {
                holder.btnRecycle.setText("Selected");
                holder.btnRecycle.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            } else {
                holder.btnRecycle.setText("Recycle");
                holder.btnRecycle.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            }
            holder.btnRecycle.setEnabled(true);
            holder.btnViewRequests.setVisibility(View.GONE);
        }
        
        holder.btnRecycle.setOnClickListener(v -> listener.onRecycleClick(center));
        holder.btnViewRequests.setOnClickListener(v -> listener.onViewRequestsClick(center));
    }

    @Override
    public int getItemCount() { return centers.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, subDescription;
        Button btnMore, btnRecycle, btnViewRequests;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.bubble_title);
            description = itemView.findViewById(R.id.bubble_description);
            subDescription = itemView.findViewById(R.id.bubble_subdescription);
            btnMore = itemView.findViewById(R.id.bubble_moreinfo);
            btnRecycle = itemView.findViewById(R.id.bubble_recycle);
            btnViewRequests = itemView.findViewById(R.id.btn_view_requests);
        }
    }
}
