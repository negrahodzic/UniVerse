package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.universe.android.adapter.EventPreviewAdapter;
import com.universe.android.adapter.SessionHistoryAdapter;
import com.universe.android.manager.UserManager;
import com.universe.android.model.Event;
import com.universe.android.model.StudySession;
import com.universe.android.model.User;
import com.universe.android.util.StatsHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements SessionHistoryAdapter.OnSessionClickListener {
    private static final String TAG = "DashboardActivity";
    private static final int MAX_RECENT_SESSIONS = 3; // Maximum number of recent sessions to show

    // User stats views
    private TextView pointsText;
    private TextView levelText;
    private TextView rankText;
    private TextView sessionsText;

    // Session views
    private RecyclerView recentSessionsList;
    private TextView noSessionsText;
    private TextView viewAllSessionsButton;

    // Adapters
    private SessionHistoryAdapter sessionAdapter;
    private EventPreviewAdapter eventAdapter;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase and UserManager
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance();

        // Initialize views
        initializeViews();
        setupAdapters();

        // Load user data
        loadUserData();

        // Load events
        loadSampleEvents();

        // Load recent sessions
        loadRecentSessions();
    }

    private void initializeViews() {
        // User stats views
        pointsText = findViewById(R.id.pointsText);
        levelText = findViewById(R.id.levelText);
        rankText = findViewById(R.id.rankText);
        sessionsText = findViewById(R.id.sessionsText);

        // Find the container views for each statistic by their IDs
        View pointsContainer = findViewById(R.id.pointsContainer);
        View levelContainer = findViewById(R.id.levelContainer);
        View rankContainer = findViewById(R.id.rankContainer);
        View sessionsContainer = findViewById(R.id.sessionsContainer);

        // Event views
        RecyclerView eventsPreviewList = findViewById(R.id.eventsPreviewList);
        eventsPreviewList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Session views
        try {
            recentSessionsList = findViewById(R.id.recentSessionsList);
            recentSessionsList.setLayoutManager(new LinearLayoutManager(this));

            noSessionsText = findViewById(R.id.noSessionsText);
            viewAllSessionsButton = findViewById(R.id.viewAllSessionsButton);
        } catch (Exception e) {
            Log.e(TAG, "Session history views not found", e);
        }

        MaterialButton startSessionButton = findViewById(R.id.startSessionButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Set up stat containers click listeners
        if (pointsContainer != null) {
            pointsContainer.setOnClickListener(v -> {
                // Navigate to Profile
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (levelContainer != null) {
            levelContainer.setOnClickListener(v -> {
                // Navigate to Profile
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (rankContainer != null) {
            rankContainer.setOnClickListener(v -> {
                // Navigate to Leaderboard
                Intent intent = new Intent(this, LeaderboardActivity.class);
                startActivity(intent);
            });
        }

        if (sessionsContainer != null) {
            sessionsContainer.setOnClickListener(v -> {
                // Navigate to Session History
                Intent intent = new Intent(this, SessionHistoryActivity.class);
                startActivity(intent);
            });
        }

        // Set up button clicks
        startSessionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudySessionActivity.class);
            startActivity(intent);
        });

        if (viewAllSessionsButton != null) {
            viewAllSessionsButton.setOnClickListener(v -> {
                // Navigate to proper SessionHistoryActivity
                Intent intent = new Intent(this, SessionHistoryActivity.class);
                startActivity(intent);
            });
        }

        // Set up bottom navigation
        bottomNav.setSelectedItemId(R.id.navigation_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;

            if (itemId == R.id.navigation_dashboard) {
                return true;
            } else if (itemId == R.id.navigation_study) {
                intent = new Intent(this, StudySessionActivity.class);
            } else if (itemId == R.id.navigation_events) {
                intent = new Intent(this, EventsActivity.class);
            } else if (itemId == R.id.navigation_leaderboard) {
                intent = new Intent(this, LeaderboardActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void setupAdapters() {
        // Event adapter
        eventAdapter = new EventPreviewAdapter(event -> {
            Intent intent = new Intent(this, EventsActivity.class);
            startActivity(intent);
        });

        RecyclerView eventsPreviewList = findViewById(R.id.eventsPreviewList);
        eventsPreviewList.setAdapter(eventAdapter);

        // Session adapter (if view exists)
        if (recentSessionsList != null) {
            sessionAdapter = new SessionHistoryAdapter(this);
            recentSessionsList.setAdapter(sessionAdapter);
        }
    }

    private void loadSampleEvents() {
        List<Event> events = new ArrayList<>();
        events.add(new Event(
                "1",
                "Campus Music Festival",
                "Join us for a night of amazing performances by student bands and professional artists.",
                "March 15, 2025",
                "7:30 PM",
                "University Main Square",
                "123 University Avenue, Nottingham NG1 1AA",
                500,
                R.drawable.ic_launcher_background
        ));
        events.add(new Event(
                "2",
                "Career Fair 2025",
                "Connect with top employers and explore internship and job opportunities.",
                "March 20, 2025",
                "10:00 AM",
                "Student Union Building",
                "456 University Boulevard, Nottingham NG7 2RD",
                200,
                R.drawable.ic_launcher_background
        ));
        eventAdapter.setEvents(events);
    }

    private void loadUserData() {
        // Initialize user stats then load data
        userManager.initializeUserStats()
                .addOnSuccessListener(aVoid -> {
                    // Now load current user data
                    userManager.getCurrentUserData()
                            .addOnSuccessListener(user -> {
                                if (user != null) {
                                    updateDashboardStats(user);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updateDashboardStats(User user) {
        // Set points
        pointsText.setText(String.valueOf(user.getPoints()));

        // Set level (calculate from points)
        int level = StatsHelper.calculateLevel(user.getPoints());
        levelText.setText(String.valueOf(level));

        // Set completed sessions count (only successful ones)
        sessionsText.setText(String.valueOf(user.getCompletedSessions()));

        // Then query for actual rank if we have an organization ID
        if (user.getOrganisationId() != null && !user.getOrganisationId().isEmpty()) {
            loadUserRank(user.getUid(), user.getOrganisationId());
        }
    }

    private void loadUserRank(String userId, String orgId) {
        db.collection("users")
                .whereEqualTo("organisationId", orgId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        users.add(doc.toObject(User.class));
                    }

                    Collections.sort(users, (u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()));

                    int rank = 1;
                    for (User user : users) {
                        if (user.getUid().equals(userId)) {
                            // Found the user, set rank
                            rankText.setText("#" + rank);
                            break;
                        }
                        rank++;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading rank", e);
                });
    }

    private void loadRecentSessions() {
        // Check if views exist
        if (recentSessionsList == null || noSessionsText == null) {
            Log.w(TAG, "Session views not found, skipping session loading");
            return;
        }

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            noSessionsText.setVisibility(View.VISIBLE);
            recentSessionsList.setVisibility(View.GONE);
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Option 1: Just filter by hostId without ordering
        db.collection("sessions")
                .whereEqualTo("hostId", userId)
                .limit(MAX_RECENT_SESSIONS)  // Still limit to 3
                .get()  // Using get() instead of addSnapshotListener() to avoid index errors
                .addOnCompleteListener(task -> {
                    if (isDestroyed() || isFinishing()) return;

                    if (task.isSuccessful()) {
                        List<StudySession> sessions = new ArrayList<>();

                        if (task.getResult().isEmpty()) {
                            // No sessions found
                            noSessionsText.setVisibility(View.VISIBLE);
                            recentSessionsList.setVisibility(View.GONE);
                            return;
                        }

                        // Process each session document
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            StudySession session = document.toObject(StudySession.class);
                            sessions.add(session);
                        }

                        // Sort the results in memory instead of in the query
                        Collections.sort(sessions, (a, b) -> {
                            if (a.getStartTime() == null) return 1;
                            if (b.getStartTime() == null) return -1;
                            return b.getStartTime().compareTo(a.getStartTime());
                        });

                        // Limit to MAX_RECENT_SESSIONS
                        if (sessions.size() > MAX_RECENT_SESSIONS) {
                            sessions = sessions.subList(0, MAX_RECENT_SESSIONS);
                        }

                        // Update UI
                        sessionAdapter.setSessions(sessions);
                        noSessionsText.setVisibility(View.GONE);
                        recentSessionsList.setVisibility(View.VISIBLE);

                    } else {
                        // Error loading sessions
                        Log.e(TAG, "Error getting recent sessions: ", task.getException());
                        noSessionsText.setVisibility(View.VISIBLE);
                        recentSessionsList.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onSessionClick(StudySession session) {
        Intent intent = new Intent(this, SessionDetailsActivity.class);
        intent.putExtra("session_id", session.getId());
        startActivity(intent);
    }

    @Override
    public void onDetailsClick(StudySession session) {
        onSessionClick(session);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the dashboard
        loadUserData();
        loadRecentSessions();
    }

}