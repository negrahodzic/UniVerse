package com.universe.android.model;

import com.universe.android.util.StatsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String email;
    private String username;
    private String organisationId;
    private int points;
    private long totalStudyTime; // in minutes

    private String nfcId;
    private String profileImageBase64;
    private boolean emailVerified;

    // New fields for enhanced stats
    private int streakDays;            // Current consecutive study days
    private int maxStreakDays;         // Longest streak achieved
    private int eventsAttended;        // Total events attended
    private List<String> achievements; // IDs of earned achievements
    private List<String> friends;      // List of friend user IDs

    // Weekly tracking
    private long lastStudyDate;           // Timestamp of last study
    private Map<String, Integer> weeklyStats; // Map of week ID → points earned

    // Consistency tracking
    private int consistencyScore;      // 0-100 score of study consistency

    private int sessionsCompleted;     // Total sessions count

    private int completedSessions;

    // Required for Firestore
    public User() {
        this.achievements = new ArrayList<>();
        this.friends = new ArrayList<>();
        this.weeklyStats = new HashMap<>();
    }

    public User(String uid, String email, String username, String organisationId) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.organisationId = organisationId;
        this.points = 0;
        this.totalStudyTime = 0;
        this.streakDays = 0;
        this.maxStreakDays = 0;
        this.sessionsCompleted = 0;
        this.eventsAttended = 0;
        this.consistencyScore = 0;
        this.achievements = new ArrayList<>();
        this.friends = new ArrayList<>();
        this.weeklyStats = new HashMap<>();
    }

    // Existing getters and setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public long getTotalStudyTime() {
        return totalStudyTime;
    }

    public void setTotalStudyTime(long totalStudyTime) {
        this.totalStudyTime = totalStudyTime;
    }

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String base64) {
        this.profileImageBase64 = base64;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean verified) {
        this.emailVerified = verified;
    }

    // New getters and setters for enhanced stats
    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
        // Update max streak if current streak is higher
        if (streakDays > maxStreakDays) {
            this.maxStreakDays = streakDays;
        }
    }

    public int getMaxStreakDays() {
        return maxStreakDays;
    }

    public void setMaxStreakDays(int maxStreakDays) {
        this.maxStreakDays = maxStreakDays;
    }

    public void incrementSessionsCompleted() {
        this.sessionsCompleted++;
    }

    public int getEventsAttended() {
        return eventsAttended;
    }

    public void setEventsAttended(int eventsAttended) {
        this.eventsAttended = eventsAttended;
    }

    public void incrementEventsAttended() {
        this.eventsAttended++;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public void addAchievement(String achievementId) {
        if (this.achievements == null) {
            this.achievements = new ArrayList<>();
        }
        if (!this.achievements.contains(achievementId)) {
            this.achievements.add(achievementId);
        }
    }

    public boolean hasAchievement(String achievementId) {
        return this.achievements != null && this.achievements.contains(achievementId);
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public void addFriend(String friendId) {
        if (this.friends == null) {
            this.friends = new ArrayList<>();
        }
        if (!this.friends.contains(friendId)) {
            this.friends.add(friendId);
        }
    }

    public boolean isFriendWith(String friendId) {
        return this.friends != null && this.friends.contains(friendId);
    }

    public long getLastStudyDate() {
        return lastStudyDate;
    }

    public void setLastStudyDate(long lastStudyDate) {
        this.lastStudyDate = lastStudyDate;
    }

    public Map<String, Integer> getWeeklyStats() {
        return weeklyStats;
    }

    public void setWeeklyStats(Map<String, Integer> weeklyStats) {
        this.weeklyStats = weeklyStats;
    }

    public void addWeeklyPoints(String weekId, int pointsToAdd) {
        if (this.weeklyStats == null) {
            this.weeklyStats = new HashMap<>();
        }

        int currentPoints = this.weeklyStats.getOrDefault(weekId, 0);
        this.weeklyStats.put(weekId, currentPoints + pointsToAdd);
    }

    public int getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(int consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    // Helper methods

    /**
     * Calculate user's level based on points
     *
     * @return The calculated level (starting from 1)
     */
    public int calculateLevel() {
        return StatsHelper.calculateLevel(points);
    }

    /**
     * Get formatted study time in hours and minutes
     *
     * @return Formatted study time string
     */
    public String getFormattedStudyTime() {
        long hours = totalStudyTime / 60;
        long minutes = totalStudyTime % 60;

        if (hours > 0) {
            return hours + "h " + (minutes > 0 ? minutes + "m" : "");
        } else {
            return minutes + "m";
        }
    }

    /**
     * Get the number of achievements earned
     *
     * @return Count of achievements
     */
    public int getAchievementCount() {
        return achievements != null ? achievements.size() : 0;
    }

    /**
     * Get the number of friends
     *
     * @return Count of friends
     */
    public int getFriendCount() {
        return friends != null ? friends.size() : 0;
    }

    public void incrementCompletedSessions() {
        this.completedSessions++;
    }

    public int getSessionsCompleted() {
        return sessionsCompleted;
    }

    public void setSessionsCompleted(int sessionsCompleted) {
        this.sessionsCompleted = sessionsCompleted;
    }

    public int getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }
}