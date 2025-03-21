package com.universe.android.model;

import com.universe.android.util.StatsHelper;

/**
 * Model class for a leaderboard entry
 */
public class LeaderboardEntry {
    private String userId;
    private String username;
    private String profileImageBase64;
    private int rank;
    private int points;
    private int level;
    private long studyTime; // in minutes
    private int streakDays;
    private int achievementCount;
    private boolean isCurrentUser;

    // Required for Firestore
    public LeaderboardEntry() {
    }

    // Constructor from User object
    public LeaderboardEntry(User user, int rank) {
        this.userId = user.getUid();
        this.username = user.getUsername();
        this.profileImageBase64 = user.getProfileImageBase64();
        this.rank = rank;
        this.points = user.getPoints();
        this.level = user.calculateLevel();
        this.studyTime = user.getTotalStudyTime();
        this.streakDays = user.getStreakDays();
        this.achievementCount = user.getAchievementCount();
        this.isCurrentUser = false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
        this.level = calculateLevel(points);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getStudyTime() {
        return studyTime;
    }

    public void setStudyTime(long studyTime) {
        this.studyTime = studyTime;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public int getAchievementCount() {
        return achievementCount;
    }

    public void setAchievementCount(int achievementCount) {
        this.achievementCount = achievementCount;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }

    // Helper methods
    private int calculateLevel(int points) {
        return StatsHelper.calculateLevel(points);
    }

    public String getFormattedStudyTime() {
        double hours = studyTime / 60.0;
        return String.format("%.1f", hours);
    }

}