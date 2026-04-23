package my.edu.utar.RecycleGO;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import my.edu.utar.RecycleGO.database.CampaignImageHelper;
import my.edu.utar.RecycleGO.database.CampaignRecord;

public class CampaignAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FEATURED = 0;
    private static final int VIEW_TYPE_REGULAR = 1;

    private List<CampaignRecord> campaignList;
    private CampaignImageHelper imageHelper;

    public CampaignAdapter(List<CampaignRecord> campaignList) {
        this.campaignList = campaignList;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_FEATURED : VIEW_TYPE_REGULAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (imageHelper == null) {
            imageHelper = new CampaignImageHelper(parent.getContext());
        }
        
        if (viewType == VIEW_TYPE_FEATURED) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campaign, parent, false);
            return new FeaturedViewHolder(view, imageHelper);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campaign, parent, false);
            return new RegularViewHolder(view, imageHelper);
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

    public static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLoc, tvCountdown;
        ImageView ivCampaign;
        CampaignImageHelper imageHelper;

        public FeaturedViewHolder(@NonNull View itemView, CampaignImageHelper imageHelper) {
            super(itemView);
            this.imageHelper = imageHelper;
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLoc = itemView.findViewById(R.id.tv_location);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);
            ivCampaign = itemView.findViewById(R.id.iv_campaign);
        }

        public void bind(CampaignRecord item) {
            tvTitle.setText(item.getTitle());
            tvLoc.setText(item.getLocation());
            
            // Load image from SQLite
            byte[] imageData = imageHelper.getImage(item.getId());
            if (imageData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                ivCampaign.setImageBitmap(bitmap);
            } else {
                ivCampaign.setImageResource(R.drawable.ic_recycle); // Default
            }

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());
                tvDate.setText(sdf.format(item.getDate().toDate()));
                
                long diff = item.getDate().toDate().getTime() - System.currentTimeMillis();
                if (diff > 0) {
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                    tvCountdown.setText(String.format(Locale.getDefault(), "%dD %dH", days, hours));
                } else {
                    tvCountdown.setText("STARTED");
                }
            }
        }
    }

    public static class RegularViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLoc, tvCountdown;
        ImageView ivCampaign;
        CampaignImageHelper imageHelper;

        public RegularViewHolder(@NonNull View itemView, CampaignImageHelper imageHelper) {
            super(itemView);
            this.imageHelper = imageHelper;
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLoc = itemView.findViewById(R.id.tv_location);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);
            ivCampaign = itemView.findViewById(R.id.iv_campaign);
            
            ivCampaign.getLayoutParams().height = (int) (120 * itemView.getContext().getResources().getDisplayMetrics().density);
        }

        public void bind(CampaignRecord item) {
            tvTitle.setText(item.getTitle());
            tvLoc.setText(item.getLocation());

            // Load image from SQLite
            byte[] imageData = imageHelper.getImage(item.getId());
            if (imageData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                ivCampaign.setImageBitmap(bitmap);
            } else {
                ivCampaign.setImageResource(R.drawable.ic_recycle); // Default
            }

            if (item.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(item.getDate().toDate()));
                
                long diff = item.getDate().toDate().getTime() - System.currentTimeMillis();
                if (diff > 0) {
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    tvCountdown.setText(days + "D LEFT");
                } else {
                    tvCountdown.setText("LIVE");
                }
            }
        }
    }
}
