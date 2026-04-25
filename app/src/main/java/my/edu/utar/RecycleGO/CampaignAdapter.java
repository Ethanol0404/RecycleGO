package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import my.edu.utar.RecycleGO.database.CampaignRecord;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.ImageManager;

public class CampaignAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FEATURED = 0;
    private static final int VIEW_TYPE_REGULAR = 1;

    private List<CampaignRecord> campaignList;
    private FirestoreManager firestoreManager;

    public CampaignAdapter(List<CampaignRecord> campaignList) {
        this.campaignList = campaignList;
        this.firestoreManager = new FirestoreManager();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_FEATURED : VIEW_TYPE_REGULAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campaign, parent, false);
        if (viewType == VIEW_TYPE_FEATURED) {
            return new FeaturedViewHolder(view, firestoreManager);
        } else {
            return new RegularViewHolder(view, firestoreManager);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CampaignRecord item = campaignList.get(position);
        
        if (holder instanceof FeaturedViewHolder) {
            ((FeaturedViewHolder) holder).bind(item);
        } else if (holder instanceof RegularViewHolder) {
            ((RegularViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return campaignList.size();
    }

    private static String getCountdownText(long targetTimeMillis) {
        long diff = targetTimeMillis - System.currentTimeMillis();
        if (diff <= 0) return "Started";

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        
        if (days > 0) {
            return days + "D " + hours + "H";
        } else if (hours > 0) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
            return hours + "H " + minutes + "M";
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "M";
        }
    }

    private static void showParticipantsDialog(Context context, List<String> participantIds, FirestoreManager firestoreManager) {
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setPadding(20, 20, 20, 20);

        List<UserRecord> users = new ArrayList<>();
        ParticipantAdapter adapter = new ParticipantAdapter(users);
        rv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Participants")
                .setView(rv)
                .setPositiveButton("Close", null)
                .create();

        dialog.show();

        if (participantIds != null && !participantIds.isEmpty()) {
            for (String id : participantIds) {
                firestoreManager.getUser(id, new FirestoreManager.OnUserFetchListener() {
                    @Override
                    public void onUserFetched(UserRecord user) {
                        if (user != null) {
                            users.add(user);
                            adapter.notifyItemInserted(users.size() - 1);
                        }
                    }
                    @Override
                    public void onFailure(String error) {}
                });
            }
        }
    }

    private static class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.VH> {
        List<UserRecord> list;
        ParticipantAdapter(List<UserRecord> list) { this.list = list; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_participant, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            UserRecord u = list.get(p);
            h.name.setText(u.getUsername());
            if (u.getProfilePicUrl() != null && !u.getProfilePicUrl().isEmpty()) {
                Glide.with(h.itemView.getContext()).load(u.getProfilePicUrl()).placeholder(R.drawable.useravatar).into(h.avatar);
            } else {
                h.avatar.setImageResource(R.drawable.useravatar);
            }
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            ImageView avatar; TextView name;
            VH(View v) { super(v); avatar = v.findViewById(R.id.iv_avatar); name = v.findViewById(R.id.tv_username); }
        }
    }

    public static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLoc, tvCountdown;
        ImageView ivCampaign;
        Button btnJoin;
        FirestoreManager firestoreManager;

        public FeaturedViewHolder(@NonNull View itemView, FirestoreManager firestoreManager) {
            super(itemView);
            this.firestoreManager = firestoreManager;
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLoc = itemView.findViewById(R.id.tv_location);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);
            ivCampaign = itemView.findViewById(R.id.iv_campaign);
            btnJoin = itemView.findViewById(R.id.btn_join);
        }

        public void bind(CampaignRecord item) {
            tvTitle.setText(item.getTitle());
            tvLoc.setText(item.getLocation());
            
            ImageManager.loadImage(itemView.getContext(), item.getImageUrl(), ivCampaign);

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());
                String formattedDate = sdf.format(item.getDate().toDate());
                tvDate.setText(formattedDate);
                tvCountdown.setText(getCountdownText(item.getDate().toDate().getTime()));
            }

            SharedPreferences prefs = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");
            String userRole = prefs.getString("loggedInRole", "User");

            if ("Admin".equalsIgnoreCase(userRole)) {
                btnJoin.setText("View Participants");
                btnJoin.setEnabled(true);
                btnJoin.setAlpha(1.0f);
                btnJoin.setOnClickListener(v -> showParticipantsDialog(itemView.getContext(), item.getParticipants(), firestoreManager));
            } else {
                if (item.getParticipants() != null && item.getParticipants().contains(currentUid)) {
                    btnJoin.setText("Joined");
                    btnJoin.setEnabled(false);
                    btnJoin.setAlpha(0.6f);
                } else {
                    btnJoin.setText("Join");
                    btnJoin.setEnabled(true);
                    btnJoin.setAlpha(1.0f);
                    btnJoin.setOnClickListener(v -> {
                        if (currentUid.isEmpty()) {
                            Toast.makeText(itemView.getContext(), "Please login first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        firestoreManager.joinCampaign(item.getId(), currentUid, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(itemView.getContext(), "Joined successfully!", Toast.LENGTH_SHORT).show();
                                if (item.getParticipants() == null) item.setParticipants(new ArrayList<>());
                                item.getParticipants().add(currentUid);
                                btnJoin.setText("Joined");
                                btnJoin.setEnabled(false);
                                btnJoin.setAlpha(0.6f);
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(itemView.getContext(), "Failed to join: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            }
        }
    }

    public static class RegularViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLoc, tvCountdown;
        ImageView ivCampaign;
        Button btnJoin;
        FirestoreManager firestoreManager;

        public RegularViewHolder(@NonNull View itemView, FirestoreManager firestoreManager) {
            super(itemView);
            this.firestoreManager = firestoreManager;
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLoc = itemView.findViewById(R.id.tv_location);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);
            ivCampaign = itemView.findViewById(R.id.iv_campaign);
            btnJoin = itemView.findViewById(R.id.btn_join);
            
            ivCampaign.getLayoutParams().height = (int) (120 * itemView.getContext().getResources().getDisplayMetrics().density);
        }

        public void bind(CampaignRecord item) {
            tvTitle.setText(item.getTitle());
            tvLoc.setText(item.getLocation());

            ImageManager.loadImage(itemView.getContext(), item.getImageUrl(), ivCampaign);

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                String formattedDate = sdf.format(item.getDate().toDate());
                tvDate.setText(formattedDate);
                tvCountdown.setText(getCountdownText(item.getDate().toDate().getTime()));
            }

            SharedPreferences prefs = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");
            String userRole = prefs.getString("loggedInRole", "User");

            if ("Admin".equalsIgnoreCase(userRole)) {
                btnJoin.setText("View Participants");
                btnJoin.setEnabled(true);
                btnJoin.setAlpha(1.0f);
                btnJoin.setOnClickListener(v -> showParticipantsDialog(itemView.getContext(), item.getParticipants(), firestoreManager));
            } else {
                if (item.getParticipants() != null && item.getParticipants().contains(currentUid)) {
                    btnJoin.setText("Joined");
                    btnJoin.setEnabled(false);
                    btnJoin.setAlpha(0.6f);
                } else {
                    btnJoin.setText("Join");
                    btnJoin.setEnabled(true);
                    btnJoin.setAlpha(1.0f);
                    btnJoin.setOnClickListener(v -> {
                        if (currentUid.isEmpty()) {
                            Toast.makeText(itemView.getContext(), "Please login first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        firestoreManager.joinCampaign(item.getId(), currentUid, new FirestoreManager.OnTaskCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(itemView.getContext(), "Joined successfully!", Toast.LENGTH_SHORT).show();
                                if (item.getParticipants() == null) item.setParticipants(new ArrayList<>());
                                item.getParticipants().add(currentUid);
                                btnJoin.setText("Joined");
                                btnJoin.setEnabled(false);
                                btnJoin.setAlpha(0.6f);
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(itemView.getContext(), "Failed to join: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            }
        }
    }
}
