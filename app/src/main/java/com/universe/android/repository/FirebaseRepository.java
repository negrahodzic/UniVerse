package com.universe.android.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class FirebaseRepository {
    protected final FirebaseFirestore db;
    protected final FirebaseAuth auth;

    public FirebaseRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}