package my.edu.utar.RecycleGO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;

public class ParticipantsFragment extends Fragment {

    private static final String ARG_PARTICIPANT_IDS = "participant_ids";
    private ArrayList<String> participantIds;
    private ParticipantAdapter adapter;
    private List<UserRecord> userList = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private TextView tvNoParticipants;

    public static ParticipantsFragment newInstance(List<String> participantIds) {
        ParticipantsFragment fragment = new ParticipantsFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PARTICIPANT_IDS, new ArrayList<>(participantIds));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            participantIds = getArguments().getStringArrayList(ARG_PARTICIPANT_IDS);
        }
        firestoreManager = new FirestoreManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_participants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyCustomTheme(view);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rv_participants);
        tvNoParticipants = view.findViewById(R.id.tv_no_participants);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ParticipantAdapter(userList);
        rv.setAdapter(adapter);

        loadParticipants();
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bgColorCode = prefs.getString("theme_color", "#D1E29B");
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bgColor = Color.parseColor(bgColorCode);
        int bottomColor = Color.parseColor(bottomColorCode);

        view.setBackgroundColor(bgColor);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextColor(bottomColor);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(bottomColor);
            }
        }

        TextView tvNo = view.findViewById(R.id.tv_no_participants);
        if (tvNo != null) tvNo.setTextColor(bottomColor);
    }

    private void loadParticipants() {
        if (participantIds == null || participantIds.isEmpty()) {
            if (tvNoParticipants != null) tvNoParticipants.setVisibility(View.VISIBLE);
            return;
        }

        for (String id : participantIds) {
            firestoreManager.getUser(id, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null) {
                        userList.add(user);
                        adapter.notifyItemInserted(userList.size() - 1);
                    }
                }

                @Override
                public void onFailure(String error) {
                    // Fail silently or log
                }
            });
        }
    }

    private static class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.VH> {
        List<UserRecord> list;

        ParticipantAdapter(List<UserRecord> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_participant, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            UserRecord u = list.get(p);
            h.name.setText(u.getUsername());
            
            String email = u.getEmail();
            if (email != null && !email.isEmpty()) {
                h.email.setText(email);
                h.email.setVisibility(View.VISIBLE);
            } else {
                h.email.setVisibility(View.GONE);
            }

            String phone = u.getPhone();
            if (phone != null && !phone.isEmpty()) {
                h.phone.setText(phone);
                h.phone.setVisibility(View.VISIBLE);
            } else {
                h.phone.setVisibility(View.GONE);
            }

            if (u.getProfilePicUrl() != null && !u.getProfilePicUrl().isEmpty()) {
                Glide.with(h.itemView.getContext())
                        .load(u.getProfilePicUrl())
                        .placeholder(R.drawable.useravatar)
                        .into(h.avatar);
            } else {
                h.avatar.setImageResource(R.drawable.useravatar);
            }

            // Apply Theme to item
            SharedPreferences prefs = h.itemView.getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String bottomColorCode = prefs.getString("bottom_color", "#265200");
            int bottomColor = Color.parseColor(bottomColorCode);

            h.name.setTextColor(bottomColor);
            h.email.setTextColor(Color.argb(179, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor)));
            h.phone.setTextColor(Color.argb(179, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor)));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView avatar;
            TextView name, email, phone;

            VH(View v) {
                super(v);
                avatar = v.findViewById(R.id.iv_avatar);
                name = v.findViewById(R.id.tv_username);
                email = v.findViewById(R.id.tv_email);
                phone = v.findViewById(R.id.tv_phone);
            }
        }
    }
}
