package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import my.edu.utar.RecycleGO.database.CampaignRecord;
import my.edu.utar.RecycleGO.database.FirestoreManager;
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

    private static void openParticipantsFragment(Context context, List<String> participantIds) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            ParticipantsFragment fragment = ParticipantsFragment.newInstance(participantIds);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
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
            
            ImageManager.loadImage(itemView.getContext(), item.getImageUrl(), ivCampaign, R.drawable.campaign);

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());
                String formattedDate = sdf.format(item.getDate().toDate());
                tvDate.setText(formattedDate);
                tvCountdown.setText(getCountdownText(item.getDate().toDate().getTime()));
            }

            SharedPreferences prefs = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");
            String userRole = prefs.getString("loggedInRole", "User");

            // Apply Theme
            SharedPreferences appPrefs = itemView.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String bottomColorCode = appPrefs.getString("bottom_color", "#265200");
            int bottomColor = Color.parseColor(bottomColorCode);
            
            btnJoin.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            tvTitle.setTextColor(bottomColor);

            if ("Admin".equalsIgnoreCase(userRole)) {
                // Check if this admin is the creator of the campaign
                if (currentUid.equals(item.getCreatedBy())) {
                    btnJoin.setText("View Participants");
                    btnJoin.setEnabled(true);
                    btnJoin.setAlpha(1.0f);
                    btnJoin.setOnClickListener(v -> openParticipantsFragment(itemView.getContext(), item.getParticipants()));
                    btnJoin.setVisibility(View.VISIBLE);
                } else {
                    // Hide for other admins
                    btnJoin.setVisibility(View.GONE);
                }
            } else {
                btnJoin.setVisibility(View.VISIBLE);
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
            
            ivCampaign.getLayoutParams().height = (int) (180 * itemView.getContext().getResources().getDisplayMetrics().density);
        }

        public void bind(CampaignRecord item) {
            tvTitle.setText(item.getTitle());
            tvLoc.setText(item.getLocation());

            ImageManager.loadImage(itemView.getContext(), item.getImageUrl(), ivCampaign, R.drawable.campaign);

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                String formattedDate = sdf.format(item.getDate().toDate());
                tvDate.setText(formattedDate);
                tvCountdown.setText(getCountdownText(item.getDate().toDate().getTime()));
            }

            SharedPreferences prefs = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String currentUid = prefs.getString("loggedInUid", "");
            String userRole = prefs.getString("loggedInRole", "User");

            // Apply Theme
            SharedPreferences appPrefs = itemView.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String bottomColorCode = appPrefs.getString("bottom_color", "#265200");
            int bottomColor = Color.parseColor(bottomColorCode);
            
            btnJoin.setBackgroundTintList(ColorStateList.valueOf(bottomColor));
            tvTitle.setTextColor(bottomColor);

            if ("Admin".equalsIgnoreCase(userRole)) {
                // Check if this admin is the creator of the campaign
                if (currentUid.equals(item.getCreatedBy())) {
                    btnJoin.setText("View Participants");
                    btnJoin.setEnabled(true);
                    btnJoin.setAlpha(1.0f);
                    btnJoin.setOnClickListener(v -> openParticipantsFragment(itemView.getContext(), item.getParticipants()));
                    btnJoin.setVisibility(View.VISIBLE);
                } else {
                    // Hide for other admins
                    btnJoin.setVisibility(View.GONE);
                }
            } else {
                btnJoin.setVisibility(View.VISIBLE);
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
