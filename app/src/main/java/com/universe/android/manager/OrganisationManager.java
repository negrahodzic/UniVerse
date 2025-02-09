package com.universe.android.manager;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.universe.android.model.Organisation;

public class OrganisationManager {
    private static OrganisationManager instance;
    private final FirebaseFirestore db;

    private OrganisationManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized OrganisationManager getInstance() {
        if (instance == null) {
            instance = new OrganisationManager();
        }
        return instance;
    }

    // Get all organisations
    public Task<QuerySnapshot> getAllOrganisations() {
        return db.collection("organisations").get();
    }

    // Get organisation by ID
    public Task<Organisation> getOrganisationById(String orgId) {
        return db.collection("organisations")
                .whereEqualTo("id", orgId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Get first document that matches
                        return task.getResult().getDocuments().get(0).toObject(Organisation.class);
                    }
                    return null;
                });
    }

    // Validate email domain against organisation
    public Task<Boolean> validateEmailDomain(String orgId, String email) {
        Log.d("OrganisationManager", "Validating email: " + email + " for org: " + orgId);
        return getOrganisationById(orgId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Organisation org = task.getResult();
                        Log.d("OrganisationManager", "Found org: " + org.getId() +
                                ", domains: " + org.getDomains()); // Add this
                        return org.isValidDomain(email);
                    }
                    Log.d("OrganisationManager", "No org found or task failed");
                    return false;
                });
    }
}