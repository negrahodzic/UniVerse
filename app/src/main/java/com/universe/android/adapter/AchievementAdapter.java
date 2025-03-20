package com.universe.android.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.universe.android.R;
import com.universe.android.model.Achievement;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private List<Achievement> achievements = new ArrayList<>();
    private final OnAchievementClickListener listener;

    public interface OnAchievementClickListener {
        void onAchievementClick(Achievement achievement);
    }

    public AchievementAdapter(OnAchievementClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        holder.bind(achievements.get(position));
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public void setAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
        notifyDataSetChanged();
    }

    class AchievementViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView achievementIcon;
        private final TextView achievementTitle;
        private final TextView achievementStatus;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            achievementIcon = itemView.findViewById(R.id.achievementIcon);
            achievementTitle = itemView.findViewById(R.id.achievementTitle);
            achievementStatus = itemView.findViewById(R.id.achievementStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAchievementClick(achievements.get(position));
                }
            });
        }

        void bind(Achievement achievement) {
            achievementTitle.setText(achievement.getTitle());
            achievementIcon.setImageResource(achievement.getIconResource());

            if (achievement.isEarned()) {
                achievementStatus.setText("Unlocked");
                achievementStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_green_dark));
                achievementIcon.setAlpha(1.0f);
                cardView.setCardBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.achievement_earned_bg)));
                cardView.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.achievement_earned_stroke)));
                cardView.setStrokeWidth(2);
            } else {
                achievementStatus.setText("Locked");
                achievementStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.darker_gray));
                achievementIcon.setAlpha(0.5f);
                cardView.setCardBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.achievement_locked_bg)));
                cardView.setStrokeWidth(0);
            }
        }
    }
}