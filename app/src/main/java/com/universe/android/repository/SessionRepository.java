package com.universe.android.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.universe.android.model.StudySession;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SessionRepository extends FirebaseRepository {
    private static final String TAG = "SessionRepository";
    private static final String COLLECTION_SESSIONS = "sessions";

    private static SessionRepository instance;

    private SessionRepository() {
        super();
    }

    public static synchronized SessionRepository getInstance() {
        if (instance == null) {
            instance = new SessionRepository();
        }
        return instance;
    }

    public Task<StudySession> getSessionById(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            Log.e(TAG, "Session ID is null or empty");
            return Tasks.forException(new IllegalArgumentException("Session ID cannot be empty"));
        }

        Log.d(TAG, "Getting session by EXACT ID: '" + sessionId + "'");

        return db.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().exists()) {
                            StudySession session = task.getResult().toObject(StudySession.class);
                            Log.d(TAG, "Successfully retrieved session: " + sessionId);
                            return session;
                        } else {
                            Log.e(TAG, "Session not found in Firestore: " + sessionId);
                            return null;
                        }
                    } else {
                        Log.e(TAG, "Error retrieving session: " + sessionId, task.getException());
                        return null;
                    }
                });
    }

    public Task<Void> createSession(StudySession session) {
        return db.collection("sessions")
                .document(session.getId())
                .set(session)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session created successfully");
                });
    }

    public Task<Void> addParticipantToSession(String sessionId, Map<String, String> participant) {
        return db.collection("sessions")
                .document(sessionId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw new Exception("Failed to retrieve session");
                    }

                    StudySession session = task.getResult().toObject(StudySession.class);
                    if (session == null) {
                        throw new Exception("Session not found");
                    }

                    // Check if participant already exists
                    boolean participantExists = session.getParticipants().stream()
                            .anyMatch(p -> p.get("userId").equals(participant.get("userId")));

                    if (!participantExists) {
                        // Add participant to list
                        List<Map<String, String>> updatedParticipants = new ArrayList<>(session.getParticipants());
                        updatedParticipants.add(participant);

                        // Update the session document with new participants list
                        return db.collection("sessions")
                                .document(sessionId)
                                .update("participants", updatedParticipants);
                    }

                    // If participant already exists, just return a completed task
                    return Tasks.forResult(null);
                })
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Participant added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add participant", e);
                });
    }

    public ListenerRegistration listenToSessionUpdates(String sessionId, OnSessionUpdateListener listener) {
        return db.collection("sessions").document(sessionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        StudySession session = snapshot.toObject(StudySession.class);
                        if (session != null) {
                            listener.onSessionUpdate(session);
                        }
                    }
                });
    }
    public interface OnSessionUpdateListener {
        void onSessionUpdate(StudySession session);
    }


    public Task<Void> startSession(String sessionId) {
        return db.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .update("started", true);
    }
}