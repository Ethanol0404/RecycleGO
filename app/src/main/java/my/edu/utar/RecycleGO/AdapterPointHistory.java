package my.edu.utar.RecycleGO;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdapterPointHistory extends RecyclerView.Adapter<AdapterPointHistory.ViewHolder> {

    private final List<String> activities;
    private final List<Long> points;
    private final List<Timestamp> timestamps;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public AdapterPointHistory(List<String> activities, List<Long> points, List<Timestamp> timestamps) {
        this.activities = activities;
        this.points = points;
        this.timestamps = timestamps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_point_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Show newest history first
        int reversePos = activities.size() - 1 - position;

        holder.txtActivity.setText(activities.get(reversePos));
        holder.txtPoints.setText("+" + points.get(reversePos));
        
        Timestamp ts = timestamps.get(reversePos);
        if (ts != null) {
            holder.txtDate.setText(sdf.format(ts.toDate()));
        } else {
            holder.txtDate.setText("Unknown Date");
        }
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtActivity, txtDate, txtPoints;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtActivity = itemView.findViewById(R.id.txt_history_activity);
            txtDate = itemView.findViewById(R.id.txt_history_date);
            txtPoints = itemView.findViewById(R.id.txt_history_points);
        }
    }
}
