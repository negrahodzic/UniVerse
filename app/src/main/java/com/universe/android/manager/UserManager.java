package com.universe.android.manager;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.universe.android.model.User;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class UserManager {
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
        Log.d("UserManager", "Awarding session points");

        return db.runTransaction(transaction -> {
            for (String username : usernames) {
                // Sanitize username
                String cleanUsername = username.replace(" (Host)", "")
                        .replace("Anonymous", "")
                        .trim();

                // Skip empty or Anonymous usernames
                if (cleanUsername.isEmpty() || cleanUsername.contains("Anonymous")) {
                    continue;
                }

                // Query for user by username
                Query userQuery = db.collection("users")
                        .whereEqualTo("username", cleanUsername)
                        .limit(1);
                try {

                    // Get the query snapshot synchronously
                    QuerySnapshot querySnapshot = Tasks.await(userQuery.get());


                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        DocumentReference userRef = userDoc.getReference();

                        // Calculate new points
                        int currentPoints = userDoc.getLong("points") != null
                                ? userDoc.getLong("points").intValue()
                                : 0;
                        int newPoints = currentPoints + pointsPerUser;

                        // Update points in transaction
                        transaction.update(userRef, "points", newPoints);

                        Log.d("UserManager", "Awarded " + pointsPerUser + " points to " + cleanUsername);
                    } else {
                        Log.w("UserManager", "No user found for username: " + cleanUsername);
                    }

                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }

            return null;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("UserManager", "Successfully awarded session points");
            } else {
                Log.e("UserManager", "Failed to award session points", task.getException());
            }
        });
    }

}