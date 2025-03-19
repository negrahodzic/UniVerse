package com.universe.android.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.universe.android.model.LeaderboardEntry;
import com.universe.android.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class UserManager {
    private static final String TAG = "UserManager";
    private static final long ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1);

    private static UserManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private User currentUser;

    private UserManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public Task<User> getCurrentUserData() {
        if (auth.getCurrentUser() == null) return null;

        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        currentUser = task.getResult().toObject(User.class);
                        return currentUser;
                    }
                    return null;
                });
    }

    public Task<Void> updatePoints(int points) {
        if (auth.getCurrentUser() == null) return null;

        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .update("points", points);
    }

    public Task<Void> updateStudyTime(long totalMinutes) {
        if (auth.getCurrentUser() == null) return null;

        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .update("totalStudyTime", totalMinutes);
    }

    public Task<Void> updateStatsAfterSession(Context context, int pointsEarned, int sessionDurationMinutes) {
        if (auth.getCurrentUser() == null) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to get user data"));
            }

            User user = task.getResult();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            // Calculate new values
            int newPoints = user.getPoints() + pointsEarned;
            long newStudyTime = user.getTotalStudyTime() + sessionDurationMinutes;

            // Always increment total sessions counter
            int newSessionsCompleted = user.getSessionsCompleted() + 1;

            // Check and update streak
            boolean isStreak = updateStreak(user);

            // Update consistency score
            int newConsistencyScore = calculateConsistencyScore(user);

            // Get current week ID for stats
            String weekId = getCurrentWeekId();

            // Create update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("points", newPoints);
            updates.put("totalStudyTime", newStudyTime);
            updates.put("sessionsCompleted", newSessionsCompleted);
            updates.put("lastStudyDate", System.currentTimeMillis());
            updates.put("streakDays", user.getStreakDays());
            updates.put("maxStreakDays", user.getMaxStreakDays());
            updates.put("consistencyScore", newConsistencyScore);

            // Update completed sessions count only if points were earned (successful session)
            if (pointsEarned > 0) {
                int newCompletedSessions = user.getCompletedSessions() + 1;
                updates.put("completedSessions", newCompletedSessions);
            }

            // Only increment weekly stats if points are being awarded
            if (pointsEarned > 0) {
                updates.put("weeklyStats." + weekId, FieldValue.increment(pointsEarned));
            }

            // Update in Firestore
            return userRef.update(updates).continueWithTask(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    return updateTask;
                }

                // Check for achievements after update
                AchievementManager achievementManager = AchievementManager.getInstance();
                achievementManager.checkSessionAchievements(context, user, sessionDurationMinutes);

                if (isStreak) {
                    achievementManager.checkStreakAchievements(context, user);
                }

                achievementManager.checkConsistencyAchievements(context, user);

                return updateTask;
            });
        });
    }

    private boolean updateStreak(User user) {
        if (user == null) return false;

        long lastStudyDate = user.getLastStudyDate();
        long currentTime = System.currentTimeMillis();

        // If this is the first study session ever
        if (lastStudyDate == 0) {
            user.setStreakDays(1);
            return true;
        }

        // Calculate days between last study and now
        long dayDifference = calculateDayDifference(lastStudyDate, currentTime);

        if (dayDifference == 1) {
            // Consecutive day, increment streak
            user.setStreakDays(user.getStreakDays() + 1);
            return true;
        } else if (dayDifference == 0) {
            // Same day, maintain streak
            return false;
        } else {
            // Streak broken, reset
            user.setStreakDays(1);
            return false;
        }
    }

    private long calculateDayDifference(long timestamp1, long timestamp2) {
        // Convert timestamps to Calendar instances
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);

        // Clear time fields to compare only dates
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        // Calculate days difference
        long diffMillis = cal2.getTimeInMillis() - cal1.getTimeInMillis();
        return diffMillis / ONE_DAY_MILLIS;
    }

    private int calculateConsistencyScore(User user) {
        if (user == null) return 0;

        // Get study days in the last 2 weeks
        int studyDaysInPast2Weeks = countStudyDaysInPast2Weeks(user);

        // Calculate consistency score (study days / 14) * 100
        return Math.min(100, (studyDaysInPast2Weeks * 100) / 14);
    }

    private int countStudyDaysInPast2Weeks(User user) {
        if (user == null || user.getWeeklyStats() == null) return 0;

        // Get current and previous week IDs
        String currentWeekId = getCurrentWeekId();
        String previousWeekId = getPreviousWeekId();

        // Count days in the past 2 weeks
        int daysCount = 0;

        // Count days in current week
        if (user.getWeeklyStats().containsKey(currentWeekId)) {
            daysCount += Math.min(7, user.getWeeklyStats().get(currentWeekId));
        }

        // Count days in previous week
        if (user.getWeeklyStats().containsKey(previousWeekId)) {
            daysCount += Math.min(7, user.getWeeklyStats().get(previousWeekId));
        }

        return daysCount;
    }

    private String getCurrentWeekId() {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-" + String.format(Locale.US, "%02d", week);
    }

    private String getPreviousWeekId() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-" + String.format(Locale.US, "%02d", week);
    }

    public Task<Void> updateEventAttendance(Context context) {
        if (auth.getCurrentUser() == null) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to get user data"));
            }

            User user = task.getResult();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            // Increment events attended
            int newEventsAttended = user.getEventsAttended() + 1;

            // Update in Firestore
            return userRef.update("eventsAttended", newEventsAttended)
                    .continueWithTask(updateTask -> {
                        if (!updateTask.isSuccessful()) {
                            return updateTask;
                        }

                        // Update local user object
                        user.setEventsAttended(newEventsAttended);

                        // Check for achievements
                        AchievementManager achievementManager = AchievementManager.getInstance();
                        achievementManager.checkEventAchievements(context, user);

                        return updateTask;
                    });
        });
    }

    public Task<Object> addFriend(Context context, String friendId) {
        if (auth.getCurrentUser() == null) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to get user data"));
            }

            User user = task.getResult();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            // First check if already friends
            if (user.isFriendWith(friendId)) {
                return Tasks.forException(new Exception("Already friends with this user"));
            }

            // Update both users' friends lists (for bidirectional friendship)
            return db.runTransaction(transaction -> {
                // Add friend to current user's friends list
                transaction.update(userRef, "friends", FieldValue.arrayUnion(friendId));

                // Add current user to friend's friends list
                DocumentReference friendRef = db.collection("users").document(friendId);
                transaction.update(friendRef, "friends", FieldValue.arrayUnion(user.getUid()));

                return null;
            }).continueWithTask(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    return updateTask;
                }

                // Update local user object
                user.addFriend(friendId);

                // Check for social achievements
                AchievementManager achievementManager = AchievementManager.getInstance();
                achievementManager.checkSocialAchievements(context, user);

                return updateTask;
            });
        });
    }

    public Task<List<LeaderboardEntry>> getGlobalLeaderboard(String sortField, int limit) {
        return db.collection("users")
                .orderBy(sortField, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .continueWith(task -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();

                    if (task.isSuccessful()) {
                        int rank = 1;
                        for (DocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                LeaderboardEntry entry = new LeaderboardEntry(user, rank);

                                // Mark if this is the current user
                                if (auth.getCurrentUser() != null &&
                                        user.getUid().equals(auth.getCurrentUser().getUid())) {
                                    entry.setCurrentUser(true);
                                }

                                entries.add(entry);
                                rank++;
                            }
                        }
                    }

                    return entries;
                });
    }

    public Task<List<LeaderboardEntry>> getFriendsLeaderboard(String sortField) {
        if (auth.getCurrentUser() == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        return getCurrentUserData().continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                return Tasks.forException(new Exception("Failed to get user data"));
            }

            User currentUser = userTask.getResult();
            List<String> friendIds = currentUser.getFriends();

            if (friendIds == null || friendIds.isEmpty()) {
                // Return just the current user if no friends
                List<LeaderboardEntry> entries = new ArrayList<>();
                entries.add(new LeaderboardEntry(currentUser, 1));
                entries.get(0).setCurrentUser(true);
                return Tasks.forResult(entries);
            }

            // Include current user ID in the list
            friendIds.add(currentUser.getUid());

            // Get all users data
            return db.collection("users")
                    .whereIn("uid", friendIds)
                    .get()
                    .continueWith(task -> {
                        List<LeaderboardEntry> entries = new ArrayList<>();
                        Map<String, User> usersMap = new HashMap<>();

                        if (task.isSuccessful()) {
                            // First collect all users
                            for (DocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    usersMap.put(user.getUid(), user);
                                }
                            }

                            // Sort users based on the sort field
                            List<User> sortedUsers = new ArrayList<>(usersMap.values());

                            if (sortField.equals("totalStudyTime")) {
                                sortedUsers.sort((u1, u2) -> Long.compare(u2.getTotalStudyTime(), u1.getTotalStudyTime()));
                            } else if (sortField.equals("streakDays")) {
                                sortedUsers.sort((u1, u2) -> Integer.compare(u2.getStreakDays(), u1.getStreakDays()));
                            } else {
                                sortedUsers.sort((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));
                            }

                            // Create leaderboard entries
                            int rank = 1;
                            for (User user : sortedUsers) {
                                LeaderboardEntry entry = new LeaderboardEntry(user, rank);

                                // Mark if this is the current user
                                if (user.getUid().equals(currentUser.getUid())) {
                                    entry.setCurrentUser(true);
                                }

                                entries.add(entry);
                                rank++;
                            }
                        }

                        return entries;
                    });
        });
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
        currentUser = null;
    }


    // Get user by username
    public Task<User> getUserByUsername(String username) {
        return db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        return task.getResult().getDocuments().get(0).toObject(User.class);
                    }
                    return null;
                });
    }

    // Add points to a specific user
    public Task<Void> addPointsToUser(String userId, int pointsToAdd) {
        return db.collection("users")
                .document(userId)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        int currentPoints = doc.getLong("points") != null ?
                                doc.getLong("points").intValue() : 0;
                        return db.collection("users")
                                .document(userId)
                                .update("points", currentPoints + pointsToAdd);
                    }
                    return null;
                });
    }

    // Award points to multiple users
    public Task<Object> awardSessionPoints(List<String> usernames, int pointsPerUser) {
        Log.d("UserManager", "Awarding session points to: " + usernames.toString() + ", points per user: " + pointsPerUser);

        return db.runTransaction(transaction -> {
            for (String username : usernames) {
                String cleanUsername = username.trim(); // Make sure usernames are trimmed

                // Skip empty usernames
                if (cleanUsername.isEmpty()) {
                    Log.w("UserManager", "Empty username, skipping");
                    continue;
                }

                Log.d("UserManager", "Looking for user with username: '" + cleanUsername + "'");

                // Query for user by username
                Query userQuery = db.collection("users")
                        .whereEqualTo("username", cleanUsername)
                        .limit(1);
                try {
                    // Get the query snapshot synchronously
                    QuerySnapshot querySnapshot = Tasks.await(userQuery.get());

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String userId = userDoc.getId();
                        DocumentReference userRef = userDoc.getReference();

                        // Calculate new points
                        int currentPoints = userDoc.getLong("points") != null
                                ? userDoc.getLong("points").intValue()
                                : 0;
                        int newPoints = currentPoints + pointsPerUser;

                        Log.d("UserManager", "Found user " + cleanUsername + " (ID: " + userId + "), current points: " +
                                currentPoints + ", new points: " + newPoints);

                        // Update points in transaction
                        transaction.update(userRef, "points", newPoints);

                        // Weekly stats update
                        String weekId = getCurrentWeekId();
                        transaction.update(userRef, "weeklyStats." + weekId, FieldValue.increment(pointsPerUser));


                        Log.d("UserManager", "Awarded " + pointsPerUser + " points to " + cleanUsername);
                    } else {
                        Log.w("UserManager", "NO USER FOUND for username: '" + cleanUsername + "'");
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("UserManager", "Error in transaction for username " + cleanUsername, e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("UserManager", "Transaction completed successfully for point awarding");
            } else {
                Log.e("UserManager", "Transaction failed for point awarding", task.getException());
            }
        });
    }

    public Task<Void> updateUsername(String newUsername) {
        if (auth.getCurrentUser() == null) return null;

        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .update("username", newUsername);
    }

    public Task<Void> updatePassword(String newPassword) {
        if (auth.getCurrentUser() == null) return null;

        return auth.getCurrentUser().updatePassword(newPassword);
    }

    public Task<Void> uploadProfileImage(Uri imageUri, Context context) {
        if (auth.getCurrentUser() == null) return null;

        try {
            // Convert Uri to Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Compress and resize bitmap
            Bitmap resizedBitmap = getResizedBitmap(bitmap, 300); // max 300px

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Save to Firestore
            return db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .update("profileImageBase64", base64Image);
        } catch (IOException e) {
            Log.e("UserManager", "Error processing image", e);
            return Tasks.forException(e);
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void updateOrganisation(String newOrgId) {
        // TODO / Task<Void>
    }


    public Task<Void> updateNfcId(String newNfcId) {
        if (auth.getCurrentUser() == null) return null;

        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .update("nfcId", newNfcId);
    }

    public Task<Void> initializeUserStats() {
        if (auth.getCurrentUser() == null) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to get user data"));
            }

            User user = task.getResult();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            // Prepare initialization data
            Map<String, Object> updates = new HashMap<>();

            // Only set fields that are not already set
            if (user.getAchievements() == null) {
                updates.put("achievements", new ArrayList<String>());
            }

            if (user.getFriends() == null) {
                updates.put("friends", new ArrayList<String>());
            }

            if (user.getWeeklyStats() == null) {
                updates.put("weeklyStats", new HashMap<String, Integer>());
            }

            // Initialize other stats if they are 0 or null
            if (user.getStreakDays() == 0) {
                updates.put("streakDays", 0);
            }

            if (user.getMaxStreakDays() == 0) {
                updates.put("maxStreakDays", 0);
            }

            if (user.getSessionsCompleted() == 0) {
                updates.put("sessionsCompleted", 0);
            }

            if (user.getCompletedSessions() == 0) {
                updates.put("completedSessions", 0);
            }

            if (user.getEventsAttended() == 0) {
                updates.put("eventsAttended", 0);
            }

            if (user.getConsistencyScore() == 0) {
                updates.put("consistencyScore", 0);
            }

            if (user.getLastStudyDate() == 0) {
                updates.put("lastStudyDate", 0);
            }

            // Only update if there are fields to initialize
            if (updates.isEmpty()) {
                return Tasks.forResult(null);
            }

            return userRef.update(updates);
        });
    }
}