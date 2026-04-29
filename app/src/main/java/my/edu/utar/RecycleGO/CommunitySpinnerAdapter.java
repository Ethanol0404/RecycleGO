package my.edu.utar.RecycleGO;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.CommunityModel;

public class CommunitySpinnerAdapter extends ArrayAdapter<CommunityModel> {

    public CommunitySpinnerAdapter(@NonNull Context context, @NonNull List<CommunityModel> communities) {
        super(context, 0, communities);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_community_spinner, parent, false);
        }

        CommunityModel community = getItem(position);

        CircleImageView icon = convertView.findViewById(R.id.spinner_comm_icon);
        TextView name = convertView.findViewById(R.id.spinner_comm_name);

        if (community != null) {
            name.setText(community.getName());
            if (community.getIconUrl() != null && !community.getIconUrl().isEmpty()) {
                Glide.with(getContext()).load(community.getIconUrl()).into(icon);
            } else {
                icon.setImageResource(R.drawable.place); // Default icon
            }
        }

        return convertView;
    }
}
