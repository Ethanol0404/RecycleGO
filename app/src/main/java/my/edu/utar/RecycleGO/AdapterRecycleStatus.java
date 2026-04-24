package my.edu.utar.RecycleGO;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import my.edu.utar.RecycleGO.database.RecycleRequest;

public class AdapterRecycleStatus extends RecyclerView.Adapter<AdapterRecycleStatus.ViewHolder> {
    private List<RecycleRequest> list;
    private OnStatusActionListener actionListener;
    private boolean isAdmin;

    public interface OnStatusActionListener {
        void onAccept(RecycleRequest request);
        void onComplete(RecycleRequest request);
        void onVerify(RecycleRequest request);
        void onItemClick(RecycleRequest request);
    }

    public AdapterRecycleStatus(List<RecycleRequest> list, boolean isAdmin, OnStatusActionListener actionListener) { 
        this.list = list; 
        this.isAdmin = isAdmin;
        this.actionListener = actionListener;
    }

    public void updateList(List<RecycleRequest> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecycleRequest request = list.get(position);
        holder.tvId.setText( (request.getId() != null && request.getId().length() > 8 ? request.getId().substring(0, 8) : "N/A"));
        holder.tvItem.setText(request.getCategory());
        holder.tvDate.setText(request.getDate());
        holder.tvStatus.setText(request.getStatus());
        
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onItemClick(request);
        });
        
        // Image loading
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            my.edu.utar.RecycleGO.utils.ImageManager.loadImage(holder.itemView.getContext(), request.getPhotoUrl(), holder.img);
        } else {
            holder.img.setImageResource(R.drawable.request); // Fallback
        }

        // Hide simulation buttons to focus interaction on the detail pop-up as requested
        holder.btnConfirmComplete.setVisibility(View.GONE);
        holder.tvPointsEarned.setVisibility(View.GONE);

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Requesting":
                    holder.card.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
                    break;
                case "Accepted":
                    holder.card.setCardBackgroundColor(Color.parseColor("#A7FFEB"));
                    break;
                case "Completed":
                    holder.card.setCardBackgroundColor(Color.parseColor("#FFF176"));
                    if (!isAdmin) holder.btnConfirmComplete.setVisibility(View.VISIBLE);
                    break;
                case "Verified":
                    holder.card.setCardBackgroundColor(Color.parseColor("#FFF176"));
                    holder.tvPointsEarned.setVisibility(View.VISIBLE);
                    break;
                default:
                    holder.card.setCardBackgroundColor(Color.WHITE);
                    break;
            }
        }

        holder.btnConfirmComplete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onVerify(request);
        });
    }

    @Override
    public int getItemCount() { 
        return list == null ? 0 : list.size(); 
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvItem, tvDate, tvStatus, tvCenter;
        ImageView img;
        CardView card;
        Button btnConfirmComplete;
        TextView tvPointsEarned;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.recycle_status_id);
            tvItem = itemView.findViewById(R.id.recycle_status_item);
            tvDate = itemView.findViewById(R.id.recycle_status_date);
            tvStatus = itemView.findViewById(R.id.recycle_status_status);
            tvCenter = itemView.findViewById(R.id.recycle_status_center);
            img = itemView.findViewById(R.id.recycle_status_image);
            card = itemView.findViewById(R.id.recycle_status_card);
            btnConfirmComplete = itemView.findViewById(R.id.btnConfirmComplete);
            tvPointsEarned = itemView.findViewById(R.id.tvPointsEarned);
        }
    }
}
