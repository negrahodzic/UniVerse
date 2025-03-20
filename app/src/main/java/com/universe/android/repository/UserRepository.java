package com.universe.android.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.universe.android.model.User;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class UserRepository extends FirebaseRepository {
    private static final String TAG = "UserRepository";
    private static UserRepository instance;
    private User cachedCurrentUser;

    private UserRepository() {
        super();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public Task<User> getCurrentUserData() {
        if (!isLoggedIn()) return null;

        return db.collection("users")
                .document(getCurrentUserId())
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        cachedCurrentUser = task.getResult().toObject(User.class);
                        return cachedCurrentUser;
                    }
                    return null;
                });
    }

    public void clearCache() {
        cachedCurrentUser = null;
    }

    public Task<User> getUserById(String userId) {
        return db.collection("users")
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(User.class);
                    }
                    return null;
                });
    }

    public Task<User> getUserByNfcId(String nfcId) {
        return db.collection("users")
                .whereEqualTo("nfcId", nfcId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        return task.getResult().getDocuments().get(0).toObject(User.class);
                    }
                    return null;
                });
    }

    public Task<Void> updatePoints(int points) {
        if (!isLoggedIn()) return null;

        return db.collection("users")
                .document(getCurrentUserId())
                .update("points", points);
    }

    public Task<Void> updateStudyTime(long totalMinutes) {
        if (!isLoggedIn()) return null;

        return db.collection("users")
                .document(getCurrentUserId())
                .update("totalStudyTime", totalMinutes);
    }

    public Task<Void> uploadProfileImage(Uri imageUri, Context context) {
        if (!isLoggedIn()) return null;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            Bitmap resizedBitmap = getResizedBitmap(bitmap, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            return db.collection("users")
                    .document(getCurrentUserId())
                    .update("profileImageBase64", base64Image);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            return Tasks.forException(e);
        }
    }

    public Task<Void> updateUsername(String newUsername) {
        if (!isLoggedIn()) return null;

        return db.collection("users")
                .document(getCurrentUserId())
                .update("username", newUsername);
    }

    public Task<Void> updateNfcId(String newNfcId) {
        if (!isLoggedIn()) return null;

        return db.collection("users")
                .document(getCurrentUserId())
                .update("nfcId", newNfcId);
    }

    public Task<Void> updatePassword(String currentPassword, String newPassword) {
        if (!isLoggedIn()) return null;

        String email = auth.getCurrentUser().getEmail();
        if (email == null) {
            return Tasks.forException(new IllegalStateException("User email not available"));
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        return auth.getCurrentUser().reauthenticate(credential)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return auth.getCurrentUser().updatePassword(newPassword);
                    } else {
                        return Tasks.forException(task.getException());
                    }
                });
    }

    public Task<List<User>> getGlobalLeaderboard(String sortField, int limit) {
        return db.collection("users")
                .orderBy(sortField, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .continueWith(task -> {
                    List<User> users = new ArrayList<>();

                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                    }

                    return users;
                });
    }

    public Task<List<User>> getFriendsLeaderboard(String sortField) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return getCurrentUserData().continueWithTask(userTask -> {
            if (!userTask.isSuccessful() || userTask.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User currentUser = userTask.getResult();
            List<String> friendIds = currentUser.getFriends();

            if (friendIds == null || friendIds.isEmpty()) {
                List<User> users = new ArrayList<>();
                users.add(currentUser);
                return Tasks.forResult(users);
            }

            if (!friendIds.contains(currentUser.getUid())) {
                friendIds.add(currentUser.getUid());
            }

            return db.collection("users")
                    .whereIn("uid", friendIds)
                    .get()
                    .continueWith(task -> {
                        List<User> users = new ArrayList<>();

                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    users.add(user);
                                }
                            }
                        }

                        return users;
                    });
        });
    }

    public Task<Object> addFriend(Context context, String friendId) {
        if (!isLoggedIn()) return null;

        String currentUserId = getCurrentUserId();

        if (currentUserId.equals(friendId)) {
            return Tasks.forException(new IllegalArgumentException("Cannot add yourself as a friend"));
        }

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User user = task.getResult();
            if (user.isFriendWith(friendId)) {
                return Tasks.forException(new IllegalArgumentException("Already friends with this user"));
            }

            return db.runTransaction(transaction -> {
                DocumentReference userRef = db.collection("users").document(currentUserId);
                transaction.update(userRef, "friends", FieldValue.arrayUnion(friendId));

                DocumentReference friendRef = db.collection("users").document(friendId);
                transaction.update(friendRef, "friends", FieldValue.arrayUnion(currentUserId));

                return null;
            }).continueWithTask(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    return updateTask;
                }

                // Update local user object
                user.addFriend(friendId);

                // Check for social achievements
                AchievementRepository achievementRepository = AchievementRepository.getInstance();
                achievementRepository.checkSocialAchievements(context, user);

                return updateTask;
            });
        });
    }

    public Task<Object> removeFriend(String friendId) {
        if (!isLoggedIn()) return null;

        String currentUserId = getCurrentUserId();

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User user = task.getResult();
            if (!user.isFriendWith(friendId)) {
                return Tasks.forException(new IllegalArgumentException("Not friends with this user"));
            }

            return db.runTransaction(transaction -> {
                // Remove friend from current user's friend list
                DocumentReference userRef = db.collection("users").document(currentUserId);
                transaction.update(userRef, "friends", FieldValue.arrayRemove(friendId));

                // Remove current user from friend's friend list
                DocumentReference friendRef = db.collection("users").document(friendId);
                transaction.update(friendRef, "friends", FieldValue.arrayRemove(currentUserId));

                return null;
            }).continueWith(updateTask -> {
                if (updateTask.isSuccessful()) {
                    // Update local user object
                    if (user.getFriends() != null) {
                        user.getFriends().remove(friendId);
                    }
                }
                return null;
            });
        });
    }

    public Task<Void> updateStatsAfterSession(int pointsEarned, int sessionDurationMinutes) {
        if (!isLoggedIn()) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User user = task.getResult();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            int newPoints = user.getPoints() + pointsEarned;
            long newStudyTime = user.getTotalStudyTime() + sessionDurationMinutes;
            int newSessionsCompleted = user.getSessionsCompleted() + 1;

            updateStreak(user);
            int newConsistencyScore = calculateConsistencyScore(user);
            String weekId = getCurrentWeekId();

            Map<String, Object> updates = new HashMap<>();
            updates.put("points", newPoints);
            updates.put("totalStudyTime", newStudyTime);
            updates.put("sessionsCompleted", newSessionsCompleted);
            updates.put("lastStudyDate", System.currentTimeMillis());
            updates.put("streakDays", user.getStreakDays());
            updates.put("maxStreakDays", user.getMaxStreakDays());
            updates.put("consistencyScore", newConsistencyScore);

            if (pointsEarned > 0) {
                int newCompletedSessions = user.getCompletedSessions() + 1;
                updates.put("completedSessions", newCompletedSessions);
                updates.put("weeklyStats." + weekId, FieldValue.increment(pointsEarned));
            }

            return userRef.update(updates);
        });
    }

    public Task<Void> awardSessionPoints(List<String> usernames, int pointsPerUser) {
        return db.runTransaction(transaction -> {
            for (String username : usernames) {
                String cleanUsername = username.trim();
                if (cleanUsername.isEmpty()) continue;

                Query userQuery = db.collection("users")
                        .whereEqualTo("username", cleanUsername)
                        .limit(1);
                try {
                    QuerySnapshot querySnapshot = Tasks.await(userQuery.get());

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String userId = userDoc.getId();
                        DocumentReference userRef = userDoc.getReference();

                        int currentPoints = userDoc.getLong("points") != null
                                ? userDoc.getLong("points").intValue() : 0;
                        int newPoints = currentPoints + pointsPerUser;

                        transaction.update(userRef, "points", newPoints);
                        transaction.update(userRef, "weeklyStats." + getCurrentWeekId(),
                                FieldValue.increment(pointsPerUser));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error in transaction", e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
    }

    public Task<Void> updateEventAttendance() {
        if (!isLoggedIn()) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User user = task.getResult();
            int newEventsAttended = user.getEventsAttended() + 1;

            return db.collection("users")
                    .document(user.getUid())
                    .update("eventsAttended", newEventsAttended);
        });
    }

    public Task<Void> initializeUserStats() {
        if (!isLoggedIn()) return null;

        return getCurrentUserData().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get user data"));
            }

            User user = task.getResult();
            Map<String, Object> updates = new HashMap<>();

            if (user.getAchievements() == null) {
                updates.put("achievements", new ArrayList<String>());
            }

            if (user.getFriends() == null) {
                updates.put("friends", new ArrayList<String>());
            }

            if (user.getWeeklyStats() == null) {
                updates.put("weeklyStats", new HashMap<String, Integer>());
            }

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

            if (updates.isEmpty()) {
                return Tasks.forResult(null);
            }

            return db.collection("users")
                    .document(user.getUid())
                    .update(updates);
        });
    }

    public void signOut() {
        auth.signOut();
        cachedCurrentUser = null;
    }

    // Helper methods
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

    private boolean updateStreak(User user) {
        if (user == null) return false;

        long lastStudyDate = user.getLastStudyDate();
        long currentTime = System.currentTimeMillis();

        if (lastStudyDate == 0) {
            user.setStreakDays(1);
            return true;
        }

        long dayDifference = calculateDayDifference(lastStudyDate, currentTime);

        if (dayDifference == 1) {
            user.setStreakDays(user.getStreakDays() + 1);
            return true;
        } else if (dayDifference == 0) {
            return false;
        } else {
            user.setStreakDays(1);
            return false;
        }
    }

    private long calculateDayDifference(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);

        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        long diffMillis = cal2.getTimeInMillis() - cal1.getTimeInMillis();
        return diffMillis / (24 * 60 * 60 * 1000);
    }

    private int calculateConsistencyScore(User user) {
        if (user == null || user.getWeeklyStats() == null) return 0;

        int studyDaysInPast2Weeks = countStudyDaysInPast2Weeks(user);
        return Math.min(100, (studyDaysInPast2Weeks * 100) / 14);
    }

    private int countStudyDaysInPast2Weeks(User user) {
        if (user == null || user.getWeeklyStats() == null) return 0;

        String currentWeekId = getCurrentWeekId();
        String previousWeekId = getPreviousWeekId();
        Map<String, Integer> weeklyStats = user.getWeeklyStats();
        int daysCount = 0;

        if (weeklyStats.containsKey(currentWeekId)) {
            daysCount += Math.min(7, weeklyStats.get(currentWeekId));
        }

        if (weeklyStats.containsKey(previousWeekId)) {
            daysCount += Math.min(7, weeklyStats.get(previousWeekId));
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
}