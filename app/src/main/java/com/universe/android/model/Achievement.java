package com.universe.android.model;

import java.io.Serializable;

public class Achievement implements Serializable {
    // Achievement IDs
    public static final String ACHIEVEMENT_FIRST_SESSION = "first_session";
    public static final String ACHIEVEMENT_STUDY_STREAK_3 = "study_streak_3";
    public static final String ACHIEVEMENT_STUDY_STREAK_7 = "study_streak_7";
    public static final String ACHIEVEMENT_STUDY_STREAK_14 = "study_streak_14";
    public static final String ACHIEVEMENT_STUDY_MARATHON = "study_marathon"; // 3+ hour session
    public static final String ACHIEVEMENT_SOCIAL_BUTTERFLY = "social_butterfly"; // 5+ friends
    public static final String ACHIEVEMENT_COMMUNITY_LEADER = "community_leader"; // Host 10+ sessions
    public static final String ACHIEVEMENT_EVENT_ENTHUSIAST = "event_enthusiast"; // Attend 5+ events
    public static final String ACHIEVEMENT_POINT_COLLECTOR = "point_collector"; // 5000+ points
    public static final String ACHIEVEMENT_CONSISTENCY_KING = "consistency_king"; // 90+ consistency score

    private String id;
    private String title;
    private String description;
    private int iconResource;
    private boolean isEarned;
    private long earnedTimestamp;

    public Achievement() {
    }

    public Achievement(String id, String title, String description, int iconResource) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResource = iconResource;
        this.isEarned = false;
        this.earnedTimestamp = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public boolean isEarned() {
        return isEarned;
    }

    public void setEarned(boolean earned) {
        this.isEarned = earned;

        // Set timestamp when earning achievement
        if (earned && earnedTimestamp == 0) {
            this.earnedTimestamp = System.currentTimeMillis();
        }
    }

    public long getEarnedTimestamp() {
        return earnedTimestamp;
    }

    public void setEarnedTimestamp(long earnedTimestamp) {
        this.earnedTimestamp = earnedTimestamp;
    }
}