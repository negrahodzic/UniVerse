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

    public Task<Void> completeSession(String sessionId) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return db.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .update(
                        "completed", true,
                        "endTime", new Date()
                );
    }

    public Task<Void> cancelSession(String sessionId) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return db.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .update(
                        "completed", false,
                        "endTime", new Date()
                );
    }

    public Task<List<StudySession>> getUserSessions(int limit) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        String userId = getCurrentUserId();

        return db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("hostId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .continueWith(task -> {
                    List<StudySession> sessions = new ArrayList<>();

                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            StudySession session = document.toObject(StudySession.class);
                            if (session != null) {
                                sessions.add(session);
                            }
                        }
                    }

                    return sessions;
                });
    }

    public Task<List<StudySession>> getAllUserSessions() {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        String userId = getCurrentUserId();
        List<StudySession> allSessions = new ArrayList<>();

        // First, get sessions where user is host
        return db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("hostId", userId)
                .get()
                .continueWithTask(hostTask -> {
                    if (hostTask.isSuccessful()) {
                        for (QueryDocumentSnapshot document : hostTask.getResult()) {
                            StudySession session = document.toObject(StudySession.class);
                            allSessions.add(session);
                        }

                        // Then, get sessions where user is a participant but not the host
                        return db.collection(COLLECTION_SESSIONS)
                                .whereNotEqualTo("hostId", userId)  // To avoid duplicates with the previous query
                                .get();
                    } else {
                        return Tasks.forException(hostTask.getException());
                    }
                })
                .continueWith(participantTask -> {
                    if (participantTask.isSuccessful()) {
                        for (QueryDocumentSnapshot document : participantTask.getResult()) {
                            // Check if user is in participants list
                            StudySession session = document.toObject(StudySession.class);
                            boolean isParticipant = false;

                            if (session.getParticipants() != null) {
                                for (Map<String, String> participant : session.getParticipants()) {
                                    if (userId.equals(participant.get("userId"))) {
                                        isParticipant = true;
                                        break;
                                    }
                                }
                            }

                            if (isParticipant) {
                                allSessions.add(session);
                            }
                        }
                    }

                    return allSessions;
                });
    }

    public Task<List<StudySession>> getRecentSessions(int limit) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        String userId = getCurrentUserId();

        return db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("hostId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .continueWith(task -> {
                    List<StudySession> sessions = new ArrayList<>();

                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            StudySession session = document.toObject(StudySession.class);
                            if (session != null) {
                                sessions.add(session);
                            }
                        }
                    }

                    return sessions;
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