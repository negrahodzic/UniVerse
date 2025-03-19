package com.universe.android.util;

import com.universe.android.model.User;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class StatsHelper {
    private static final String TAG = "StatsHelper";
    private static final long ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1);

    public static int calculateLevel(int points) {
        return Math.max(1, points / 1000 + 1);
    }

    public static String formatStudyTime(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0) {
            return hours + "h " + (mins > 0 ? mins + "m" : "");
        } else {
            return mins + "m";
        }
    }

    public static String formatSessionDuration(int durationSeconds) {
        if (durationSeconds < 60) {
            return durationSeconds + " seconds"; // For testing
        } else if (durationSeconds < 3600) {
            return (durationSeconds / 60) + " minutes";
        } else {
            int hours = durationSeconds / 3600;
            int minutes = (durationSeconds % 3600) / 60;
            return hours + "h " + (minutes > 0 ? minutes + "m" : "");
        }
    }

    public static int calculateDaysSinceLastStudy(long lastStudyDate) {
        if (lastStudyDate == 0) return -1; // No previous study

        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - lastStudyDate;

        return (int) (diffMillis / ONE_DAY_MILLIS);
    }

    public static int calculateConsistencyScore(User user) {
        if (user == null || user.getWeeklyStats() == null) return 0;

        // Get study days in the last 2 weeks
        int studyDaysInPast2Weeks = countStudyDaysInPast2Weeks(user);

        // Calculate consistency score (study days / 14) * 100
        return Math.min(100, (studyDaysInPast2Weeks * 100) / 14);
    }

    public static int countStudyDaysInPast2Weeks(User user) {
        if (user == null || user.getWeeklyStats() == null) return 0;

        // Get current and previous week IDs
        String currentWeekId = getCurrentWeekId();
        String previousWeekId = getPreviousWeekId();

        // Get weekly stats
        Map<String, Integer> weeklyStats = user.getWeeklyStats();

        // Count days in the past 2 weeks
        int daysCount = 0;

        // Count days in current week
        if (weeklyStats.containsKey(currentWeekId)) {
            daysCount += Math.min(7, weeklyStats.get(currentWeekId));
        }

        // Count days in previous week
        if (weeklyStats.containsKey(previousWeekId)) {
            daysCount += Math.min(7, weeklyStats.get(previousWeekId));
        }

        return daysCount;
    }

    public static String getCurrentWeekId() {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-" + String.format(Locale.US, "%02d", week);
    }

    public static String getPreviousWeekId() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-" + String.format(Locale.US, "%02d", week);
    }

    public static int calculateAvgSessionLength(long totalStudyTime, int sessionsCompleted) {
        if (sessionsCompleted == 0) return 0;
        return (int) (totalStudyTime / sessionsCompleted);
    }

    public static String formatDate(long timestamp, String pattern) {
        if (timestamp == 0) return "Never";

        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


    public static int getUserRank(String userId, Map<String, User> allUsers) {
        if (userId == null || allUsers == null || allUsers.isEmpty()) return -1;

        // Convert to array for sorting
        User[] users = allUsers.values().toArray(new User[0]);

        // Sort by points
        java.util.Arrays.sort(users, (u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));

        // Find user's position
        for (int i = 0; i < users.length; i++) {
            if (users[i].getUid().equals(userId)) {
                return i + 1; // 1-based rank
            }
        }

        return -1; // Not found
    }
}