package my.edu.utar.RecycleGO;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.widget.Button;

import my.edu.utar.RecycleGO.database.RecycleRequest;

public class AdapterRecycleStatus extends RecyclerView.Adapter<AdapterRecycleStatus.ViewHolder> {
    private List<RecycleRequest> list;
    private OnStatusActionListener actionListener;

    public interface OnStatusActionListener {
        void onAccept(RecycleRequest request);
        void onComplete(RecycleRequest request);
    }

    public AdapterRecycleStatus(List<RecycleRequest> list, OnStatusActionListener actionListener) { 
        this.list = list; 
        this.actionListener = actionListener;
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
        holder.tvId.setText("ID: " + (request.getId() != null ? request.getId().substring(0, 8) : "N/A"));
        holder.tvItem.setText(request.getCategory());
        holder.tvDate.setText(request.getDate());
        holder.tvStatus.setText(request.getStatus());
        
        // Center info
        if (request.getCenterName() != null) {
            holder.tvCenter.setText(request.getCenterName());
            holder.tvCenter.setVisibility(View.VISIBLE);
        } else {
            holder.tvCenter.setVisibility(View.GONE);
        }

        // Color & Button Logic
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnCompleted.setVisibility(View.GONE);

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Requesting":
                    holder.card.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    break;
                case "Accepted":
                    holder.card.setCardBackgroundColor(Color.parseColor("#A7FFEB"));
                    holder.btnCompleted.setVisibility(View.VISIBLE);
                    break;
                case "Completed":
                    holder.card.setCardBackgroundColor(Color.parseColor("#FFF176"));
                    break;
                default:
                    holder.card.setCardBackgroundColor(Color.WHITE);
                    break;
            }
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAccept(request);
        });

        holder.btnCompleted.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onComplete(request);
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
        Button btnAccept, btnCompleted;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.recycle_status_id);
            tvItem = itemView.findViewById(R.id.recycle_status_item);
            tvDate = itemView.findViewById(R.id.recycle_status_date);
            tvStatus = itemView.findViewById(R.id.recycle_status_status);
            tvCenter = itemView.findViewById(R.id.recycle_status_center);
            img = itemView.findViewById(R.id.recycle_status_image);
            card = itemView.findViewById(R.id.recycle_status_card);
            btnAccept = itemView.findViewById(R.id.btnSimulateAccept);
            btnCompleted = itemView.findViewById(R.id.btnSimulateDone);
        }
    }
}
