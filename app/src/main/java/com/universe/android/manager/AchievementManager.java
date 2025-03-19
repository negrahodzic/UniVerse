package com.universe.android.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.R;
import com.universe.android.model.Achievement;
import com.universe.android.model.User;

import java.util.HashMap;
import java.util.Map;

public class AchievementManager {
    private static final String TAG = "AchievementManager";

    private static volatile AchievementManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Map of all achievements
    private final Map<String, Achievement> achievements;

    private AchievementManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize achievement definitions
        achievements = new HashMap<>();
        initializeAchievements();
    }

    public static synchronized AchievementManager getInstance() {
        if (instance == null) {
            instance = new AchievementManager();
        }
        return instance;
    }

    private void initializeAchievements() {
        // First Session
        achievements.put(Achievement.ACHIEVEMENT_FIRST_SESSION,
                new Achievement(
                        Achievement.ACHIEVEMENT_FIRST_SESSION,
                        "First Steps",
                        "Complete your first study session",
                        R.drawable.ic_launcher_foreground
                ));

        // 3-Day Streak
        achievements.put(Achievement.ACHIEVEMENT_STUDY_STREAK_3,
                new Achievement(
                        Achievement.ACHIEVEMENT_STUDY_STREAK_3,
                        "Consistency Starter",
                        "Maintain a 3-day study streak",
                        R.drawable.ic_launcher_foreground
                ));

        // 7-Day Streak
        achievements.put(Achievement.ACHIEVEMENT_STUDY_STREAK_7,
                new Achievement(
                        Achievement.ACHIEVEMENT_STUDY_STREAK_7,
                        "Weekly Warrior",
                        "Maintain a 7-day study streak",
                        R.drawable.ic_launcher_foreground
                ));

        // 14-Day Streak
        achievements.put(Achievement.ACHIEVEMENT_STUDY_STREAK_14,
                new Achievement(
                        Achievement.ACHIEVEMENT_STUDY_STREAK_14,
                        "Focus Fortnight",
                        "Maintain a 14-day study streak",
                        R.drawable.ic_launcher_foreground
                ));

        // Study Marathon
        achievements.put(Achievement.ACHIEVEMENT_STUDY_MARATHON,
                new Achievement(
                        Achievement.ACHIEVEMENT_STUDY_MARATHON,
                        "Study Marathon",
                        "Complete a study session lasting 3+ hours",
                        R.drawable.ic_launcher_foreground
                ));

        // Social Butterfly
        achievements.put(Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY,
                new Achievement(
                        Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY,
                        "Social Butterfly",
                        "Add 5 or more friends",
                        R.drawable.ic_launcher_foreground
                ));

        // Community Leader
        achievements.put(Achievement.ACHIEVEMENT_COMMUNITY_LEADER,
                new Achievement(
                        Achievement.ACHIEVEMENT_COMMUNITY_LEADER,
                        "Community Leader",
                        "Host 10 or more study sessions",
                        R.drawable.ic_launcher_foreground
                ));

        // Event Enthusiast
        achievements.put(Achievement.ACHIEVEMENT_EVENT_ENTHUSIAST,
                new Achievement(
                        Achievement.ACHIEVEMENT_EVENT_ENTHUSIAST,
                        "Event Enthusiast",
                        "Attend 5 or more events",
                        R.drawable.ic_launcher_foreground
                ));

        // Point Collector
        achievements.put(Achievement.ACHIEVEMENT_POINT_COLLECTOR,
                new Achievement(
                        Achievement.ACHIEVEMENT_POINT_COLLECTOR,
                        "Point Collector",
                        "Earn 5000 or more points",
                        R.drawable.ic_launcher_foreground
                ));

        // Consistency King
        achievements.put(Achievement.ACHIEVEMENT_CONSISTENCY_KING,
                new Achievement(
                        Achievement.ACHIEVEMENT_CONSISTENCY_KING,
                        "Consistency King",
                        "Achieve a consistency score of 90+",
                        R.drawable.ic_launcher_foreground
                ));
    }

    public Achievement getAchievement(String achievementId) {
        return achievements.get(achievementId);
    }

    public Map<String, Achievement> getAllAchievements() {
        return achievements;
    }

    public void checkSessionAchievements(Context context, User user, int sessionDurationMinutes) {
        if (user == null || auth.getCurrentUser() == null) return;

        // Check first session achievement
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_FIRST_SESSION)) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_FIRST_SESSION);
        }

        // Check study marathon achievement (3+ hours)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_STUDY_MARATHON) && sessionDurationMinutes >= 180) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_STUDY_MARATHON);
        }

        // Check community leader achievement (10+ sessions hosted)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_COMMUNITY_LEADER) && user.getSessionsCompleted() >= 10) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_COMMUNITY_LEADER);
        }

        // Check point collector achievement (5000+ points)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_POINT_COLLECTOR) && user.getPoints() >= 5000) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_POINT_COLLECTOR);
        }
    }

    public void checkStreakAchievements(Context context, User user) {
        if (user == null || auth.getCurrentUser() == null) return;

        // Check 3-day streak achievement
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_STUDY_STREAK_3) && user.getStreakDays() >= 3) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_STUDY_STREAK_3);
        }

        // Check 7-day streak achievement
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_STUDY_STREAK_7) && user.getStreakDays() >= 7) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_STUDY_STREAK_7);
        }

        // Check 14-day streak achievement
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_STUDY_STREAK_14) && user.getStreakDays() >= 14) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_STUDY_STREAK_14);
        }
    }

    public void checkSocialAchievements(Context context, User user) {
        if (user == null || auth.getCurrentUser() == null) return;

        // Check social butterfly achievement (5+ friends)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY) &&
                user.getFriends() != null && user.getFriends().size() >= 5) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY);
        }
    }

    public void checkEventAchievements(Context context, User user) {
        if (user == null || auth.getCurrentUser() == null) return;

        // Check event enthusiast achievement (5+ events)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_EVENT_ENTHUSIAST) && user.getEventsAttended() >= 5) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_EVENT_ENTHUSIAST);
        }
    }

    public void checkConsistencyAchievements(Context context, User user) {
        if (user == null || auth.getCurrentUser() == null) return;

        // Check consistency king achievement (90+ score)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_CONSISTENCY_KING) && user.getConsistencyScore() >= 90) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_CONSISTENCY_KING);
        }
    }


    public void awardAchievement(Context context, User user, String achievementId) {
        if (user == null || auth.getCurrentUser() == null) return;

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return;

        String userId = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // Add achievement to user's achievements list in Firestore
        userRef.update("achievements", FieldValue.arrayUnion(achievementId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Achievement awarded: " + achievementId);

                    // Update local user object
                    user.addAchievement(achievementId);

                    // Show achievement notification
                    showAchievementNotification(context, achievement);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error awarding achievement", e));
    }

    private void showAchievementNotification(Context context, Achievement achievement) {
        if (context == null || achievement == null) return;

        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Achievement Unlocked!")
                    .setMessage(achievement.getTitle() + "\n\n" + achievement.getDescription())
                    .setPositiveButton("Nice!", null)
                    .setIcon(achievement.getIconResource())
                    .show();
        } else {
            View rootView = ((Activity) context).findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, "Achievement Unlocked: " + achievement.getTitle(),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }
}