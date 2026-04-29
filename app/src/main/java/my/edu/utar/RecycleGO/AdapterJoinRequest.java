package my.edu.utar.RecycleGO;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class AdapterJoinRequest extends RecyclerView.Adapter<AdapterJoinRequest.ViewHolder> {

    private List<Map<String, Object>> requests;
    private OnJoinActionListener listener;
    private FirestoreManager firestoreManager;

    public interface OnJoinActionListener {
        void onApprove(String requestId, String userId, String centerId);
        void onReject(String requestId, String userId, String centerId);
    }

    public AdapterJoinRequest(List<Map<String, Object>> requests, OnJoinActionListener listener) {
        this.requests = requests;
        this.listener = listener;
        this.firestoreManager = new FirestoreManager();
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.requests = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> request = requests.get(position);
        String requestId = (String) request.get("id");
        String userId = (String) request.get("userId");
        String centerId = (String) request.get("centerId");
        String status = (String) request.get("status");
        String staticContact = (String) request.get("contactInfo");

        // Initial setup with data from request document
        holder.tvUsername.setText((String) request.get("username"));
        holder.tvContact.setText("Contact: " + (staticContact != null ? staticContact : "Not provided"));
        holder.tvCenterName.setText("Center: " + request.get("centerName"));
        holder.tvStatus.setText(status);

        // Fetch latest user info (username, contact info, and profile pic)
        if (userId != null) {
            firestoreManager.getUser(userId, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        holder.tvUsername.setText(user.getUsername());
                        
                        // Retrieve latest contact info (phone or email)
                        String contact = user.getPhone();
                        if (contact == null || contact.isEmpty()) {
                            contact = user.getEmail();
                        }
                        holder.tvContact.setText("Contact: " + (contact != null ? contact : "Not provided"));

                        if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                            Glide.with(holder.ivProfilePic.getContext())
                                    .load(user.getProfilePicUrl())
                                    .placeholder(R.drawable.ic_user)
                                    .into(holder.ivProfilePic);
                        } else {
                            holder.ivProfilePic.setImageResource(R.drawable.ic_user);
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    // Fallback to static data already set
                }
            });
        }

        // UI feedback based on status
        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#F57C00")); // Orange
            holder.layoutActions.setVisibility(View.VISIBLE);
        } else if ("Approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#388E3C")); // Green
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setTextColor(Color.RED);
            holder.layoutActions.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(requestId, userId, centerId);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(requestId, userId, centerId);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvContact, tvCenterName, tvStatus;
        Button btnApprove, btnReject;
        ImageView ivProfilePic;
        View layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvJoinUsername);
            tvContact = itemView.findViewById(R.id.tvJoinContact);
            tvCenterName = itemView.findViewById(R.id.tvJoinCenterName);
            tvStatus = itemView.findViewById(R.id.tvJoinStatus);
            ivProfilePic = itemView.findViewById(R.id.ivJoinProfilePic);
            btnApprove = itemView.findViewById(R.id.btnApproveJoin);
            btnReject = itemView.findViewById(R.id.btnRejectJoin);
            layoutActions = itemView.findViewById(R.id.layout_join_actions);
        }
    }
}
