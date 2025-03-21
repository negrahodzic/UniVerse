package com.universe.android.repository;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.universe.android.R;
import com.universe.android.model.Achievement;
import com.universe.android.model.User;

import java.util.HashMap;
import java.util.Map;

public class AchievementRepository extends FirebaseRepository {
    private static final String TAG = "AchievementRepository";

    private static AchievementRepository instance;
    private final Map<String, Achievement> achievements;

    private AchievementRepository() {
        super();
        this.achievements = new HashMap<>();
        initializeAchievements();
    }

    public static synchronized AchievementRepository getInstance() {
        if (instance == null) {
            instance = new AchievementRepository();
        }
        return instance;
    }

    private void initializeAchievements() {
        // First Session
        addAchievement(
                Achievement.ACHIEVEMENT_FIRST_SESSION,
                "First Steps",
                "Complete your first study session",
                R.drawable.ic_launcher_foreground
        );

        // 3-Day Streak
        addAchievement(
                Achievement.ACHIEVEMENT_STUDY_STREAK_3,
                "Consistency Starter",
                "Maintain a 3-day study streak",
                R.drawable.ic_launcher_foreground
        );

        // 7-Day Streak
        addAchievement(
                Achievement.ACHIEVEMENT_STUDY_STREAK_7,
                "Weekly Warrior",
                "Maintain a 7-day study streak",
                R.drawable.ic_launcher_foreground
        );

        // 14-Day Streak
        addAchievement(
                Achievement.ACHIEVEMENT_STUDY_STREAK_14,
                "Focus Fortnight",
                "Maintain a 14-day study streak",
                R.drawable.ic_launcher_foreground
        );

        // Study Marathon
        addAchievement(
                Achievement.ACHIEVEMENT_STUDY_MARATHON,
                "Study Marathon",
                "Complete a study session lasting 3+ hours",
                R.drawable.ic_launcher_foreground
        );

        // Social Butterfly
        addAchievement(
                Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY,
                "Social Butterfly",
                "Add 5 or more friends",
                R.drawable.ic_launcher_foreground
        );

        // Community Leader
        addAchievement(
                Achievement.ACHIEVEMENT_COMMUNITY_LEADER,
                "Community Leader",
                "Host 10 or more study sessions",
                R.drawable.ic_launcher_foreground
        );

        // Event Enthusiast
        addAchievement(
                Achievement.ACHIEVEMENT_EVENT_ENTHUSIAST,
                "Event Enthusiast",
                "Attend 5 or more events",
                R.drawable.ic_launcher_foreground
        );

        // Point Collector
        addAchievement(
                Achievement.ACHIEVEMENT_POINT_COLLECTOR,
                "Point Collector",
                "Earn 5000 or more points",
                R.drawable.ic_launcher_foreground
        );

        // Consistency King
        addAchievement(
                Achievement.ACHIEVEMENT_CONSISTENCY_KING,
                "Consistency King",
                "Achieve a consistency score of 90+",
                R.drawable.ic_launcher_foreground
        );
    }

    private void addAchievement(String id, String title, String description, int iconResource) {
        achievements.put(id, new Achievement(id, title, description, iconResource));
    }

    public Achievement getAchievement(String achievementId) {
        return achievements.get(achievementId);
    }

    public Map<String, Achievement> getAllAchievements() {
        return achievements;
    }

    public Task<Void> awardAchievement(Context context, User user, String achievementId) {
        if (!isLoggedIn() || user == null) {
            return Tasks.forException(new IllegalStateException("User not logged in or null"));
        }

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            return Tasks.forException(new IllegalArgumentException("Achievement not found: " + achievementId));
        }

        // Don't award if already earned
        if (user.hasAchievement(achievementId)) {
            return Tasks.forResult(null);
        }

        DocumentReference userRef = db.collection("users").document(user.getUid());

        // Add achievement to Firestore
        return userRef.update("achievements", FieldValue.arrayUnion(achievementId))
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Achievement awarded: " + achievementId);

                        // Update local user object
                        user.addAchievement(achievementId);

                        // Show notification
                        showAchievementNotification(context, achievement);
                    } else {
                        Log.e(TAG, "Error awarding achievement", task.getException());
                    }
                    return null;
                });
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
            try {
                View rootView = ((Activity) context).findViewById(android.R.id.content);
                if (rootView != null) {
                    Snackbar.make(rootView, "Achievement Unlocked: " + achievement.getTitle(),
                            Snackbar.LENGTH_LONG).show();
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Context is not an Activity", e);
            }
        }
    }

    public void checkSocialAchievements(Context context, User user) {
        if (user == null || user.getFriends() == null) return;

        // Social butterfly achievement (5+ friends)
        if (!user.hasAchievement(Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY) && user.getFriends().size() >= 5) {
            awardAchievement(context, user, Achievement.ACHIEVEMENT_SOCIAL_BUTTERFLY);
        }
    }

}