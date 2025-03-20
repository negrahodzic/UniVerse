package com.universe.android.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.universe.android.R;
import com.universe.android.model.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardEntry> entries = new ArrayList<>();
    private OnLeaderboardEntryClickListener listener;
    private String displayMode = "points"; // Default display mode (points, hours, streak)

    public interface OnLeaderboardEntryClickListener {
        void onEntryClick(LeaderboardEntry entry);
        void onEntryLongClick(LeaderboardEntry entry);
    }

    public LeaderboardAdapter(OnLeaderboardEntryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setEntries(List<LeaderboardEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    public void setDisplayMode(String mode) {
        this.displayMode = mode;
        notifyDataSetChanged();
    }

    public int getCurrentUserPosition() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isCurrentUser()) {
                return i;
            }
        }
        return -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView rankNumber;
        private final ImageView userAvatar;
        private final TextView username;
        private final TextView userLevel;
        private final TextView pointsValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankNumber = itemView.findViewById(R.id.rankNumber);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            username = itemView.findViewById(R.id.username);
            userLevel = itemView.findViewById(R.id.userLevel);
            pointsValue = itemView.findViewById(R.id.pointsValue);

            // Normal click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEntryClick(entries.get(position));
                }
            });

            // Long click listener for friend removal
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEntryLongClick(entries.get(position));
                    return true;
                }
                return false;
            });
        }

        void bind(LeaderboardEntry entry) {
            rankNumber.setText(String.valueOf(entry.getRank()));

            // Set username and highlight if it's the current user
            if (entry.isCurrentUser()) {
                username.setText(entry.getUsername() + " (You)");
                username.setTextColor(Color.parseColor("#1976D2")); // Highlight color
                itemView.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue background
            } else {
                username.setText(entry.getUsername());
                username.setTextColor(Color.BLACK);
                itemView.setBackgroundColor(Color.WHITE);
            }

            // Set level
            userLevel.setText("Level " + entry.getLevel());

            // Set value based on display mode
            switch (displayMode) {
                case "hours":
                    pointsValue.setText(entry.getFormattedStudyTime() + " hours");
                    break;
                case "streak":
                    pointsValue.setText(entry.getStreakDays() + " day streak");
                    break;
                case "points":
                default:
                    pointsValue.setText(String.format("%,d points", entry.getPoints()));
                    break;
            }

            // Set avatar image if available
            if (entry.getProfileImageBase64() != null && !entry.getProfileImageBase64().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(entry.getProfileImageBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    userAvatar.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } else {
                userAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Customize background of top 3 ranks
            if (entry.getRank() <= 3) {
                switch (entry.getRank()) {
                    case 1:
                        rankNumber.setBackgroundResource(R.drawable.circle_gold);
                        rankNumber.setTextColor(Color.WHITE);
                        break;
                    case 2:
                        rankNumber.setBackgroundResource(R.drawable.circle_silver);
                        rankNumber.setTextColor(Color.WHITE);
                        break;
                    case 3:
                        rankNumber.setBackgroundResource(R.drawable.circle_bronze);
                        rankNumber.setTextColor(Color.WHITE);
                        break;
                }
            } else {
                rankNumber.setBackgroundResource(R.drawable.circle_background);
                rankNumber.setTextColor(Color.BLACK);
            }
        }
    }
}