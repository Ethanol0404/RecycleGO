package my.edu.utar.RecycleGO;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import my.edu.utar.RecycleGO.database.NewsRecord;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsRecord> newsList;

    public NewsAdapter(List<NewsRecord> newsList) {
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsRecord news = newsList.get(position);
        holder.txtTitle.setText(news.getTitleAsString());
        
        // Use default white color for title
        holder.txtTitle.setTextColor(Color.WHITE);

        // Load image with Glide
        Glide.with(holder.itemView.getContext())
                .load(news.getPicurlAsString())
                .placeholder(R.drawable.launchbg)
                .into(holder.imgNews);

        // Open URL on click
        holder.itemView.setOnClickListener(v -> {
            String url = news.getNewsurlAsString();
            if (!url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imgNews;
        TextView txtTitle;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNews = itemView.findViewById(R.id.img_news);
            txtTitle = itemView.findViewById(R.id.txt_news_title);
        }
    }
}
