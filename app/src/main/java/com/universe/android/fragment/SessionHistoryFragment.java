package com.universe.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.universe.android.R;
import com.universe.android.SessionDetailsActivity;
import com.universe.android.StudySessionActivity;
import com.universe.android.adapter.SessionHistoryAdapter;
import com.universe.android.model.StudySession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SessionHistoryFragment extends Fragment implements SessionHistoryAdapter.OnSessionClickListener {
    private static final String TAG = "SessionHistoryFragment";

    // UI components
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyStateView;
    private TextView sessionsCountView;
    private TextView hoursCountView;
    private TextView pointsCountView;
    private MaterialButton startButton;

    // Adapter
    private SessionHistoryAdapter adapter;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Stats
    private int totalSessions = 0;
    private double totalHours = 0;
    private int totalPoints = 0;

    public SessionHistoryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews(view);

        // Set up RecyclerView
        setupRecyclerView();

        // Load session history
        loadSessionHistory();

        // Set up button listeners
        setupButtonListeners();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        sessionsCountView = view.findViewById(R.id.sessionsCountView);
        hoursCountView = view.findViewById(R.id.hoursCountView);
        pointsCountView = view.findViewById(R.id.pointsCountView);
        startButton = view.findViewById(R.id.startButton);
    }

    private void setupRecyclerView() {
        adapter = new SessionHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadSessionHistory() {
        Log.d(TAG, "loadSessionHistory called, userId: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "null"));

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            showEmptyState("Please sign in to view your session history");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Create a list to hold all sessions
        List<StudySession> allSessions = new ArrayList<>();

        // First, get sessions where user is host
        db.collection("sessions")
                .whereEqualTo("hostId", userId)
                .get()
                .addOnCompleteListener(hostTask -> {
                    if (hostTask.isSuccessful()) {
                        for (QueryDocumentSnapshot document : hostTask.getResult()) {
                            StudySession session = document.toObject(StudySession.class);
                            allSessions.add(session);
                        }

                        // Then, get sessions where user is a participant but not the host
                        db.collection("sessions")
                                .whereNotEqualTo("hostId", userId)  // To avoid duplicates with the previous query
                                .get()
                                .addOnCompleteListener(participantTask -> {
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

                                        // Process results after both queries are complete
                                        processSessionResults(allSessions);
                                    } else {
                                        // Error loading sessions where user is participant
                                        Log.e(TAG, "Error getting sessions as participant: ", participantTask.getException());
                                        // Still process host sessions even if participant query fails
                                        processSessionResults(allSessions);
                                    }
                                });

                        Log.d(TAG, "Host sessions count: " + allSessions.size());
                    } else {
                        // Error loading sessions where user is host
                        Log.e(TAG, "Error getting sessions as host: ", hostTask.getException());
                        showEmptyState("Couldn't load session history.\nTap to retry.");
                    }
                });

    }

    private void processSessionResults(List<StudySession> sessions) {
        Log.d(TAG, "Processing " + sessions.size() + " sessions");

        if (getActivity() == null || !isAdded()) return;

        progressBar.setVisibility(View.GONE);

        // Reset stats
        totalSessions = 0;
        totalHours = 0;
        totalPoints = 0;

        if (sessions.isEmpty()) {
            showEmptyState("You haven't completed any study sessions yet");
            updateStats();
            return;
        }

        // Accumulate stats
        for (StudySession session : sessions) {
            totalSessions++;

            if (session.getDurationSeconds() > 0) {
                totalHours += session.getDurationSeconds() / 3600.0; // Convert seconds to hours
            }

            totalPoints += session.getPointsAwarded();
        }

        // Sort results by start time (newest first)
        Collections.sort(sessions, (a, b) -> {
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return b.getStartTime().compareTo(a.getStartTime());
        });

        Log.d(TAG, "Final sessions count: " + sessions.size());

        // Update UI
        adapter.setSessions(sessions);
        recyclerView.setVisibility(View.VISIBLE);
        updateStats();
    }
    private void updateStats() {
        sessionsCountView.setText(String.valueOf(totalSessions));
        hoursCountView.setText(String.format("%.1f", totalHours));
        pointsCountView.setText(String.valueOf(totalPoints));
    }

    private void showEmptyState(String message) {
        Log.d(TAG, "Showing empty state with message: " + message);
        recyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);

        // Try to find the TextView in the empty state layout to set the message
        TextView emptyStateText = emptyStateView.findViewById(R.id.emptyStateText);
        if (emptyStateText != null) {
            emptyStateText.setText(message);
        }
    }

    private void setupButtonListeners() {
        // Start session button in empty state
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), StudySessionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onSessionClick(StudySession session) {
        Intent intent = new Intent(getContext(), SessionDetailsActivity.class);
        intent.putExtra("session_id", session.getId());
        startActivity(intent);
    }

    @Override
    public void onDetailsClick(StudySession session) {
        onSessionClick(session);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload session history when returning to the fragment
        loadSessionHistory();
    }


}