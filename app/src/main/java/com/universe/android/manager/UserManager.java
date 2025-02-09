package com.universe.android.manager;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.model.User;

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
}