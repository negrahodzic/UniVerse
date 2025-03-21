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
}