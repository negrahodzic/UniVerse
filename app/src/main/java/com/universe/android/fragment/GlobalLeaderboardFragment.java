package com.universe.android.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.universe.android.R;
import com.universe.android.adapter.LeaderboardAdapter;
import com.universe.android.model.LeaderboardEntry;
import com.universe.android.model.User;

import java.util.ArrayList;
import java.util.List;

public class GlobalLeaderboardFragment extends Fragment implements LeaderboardAdapter.OnLeaderboardEntryClickListener {
    private static final String TAG = "GlobalLeaderboard";
    private static final int LEADERBOARD_LIMIT = 100; // Maximum number of users to show

    private RecyclerView leaderboardRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private MaterialCardView userRankCard;
    private TextView userRankText;
    private TextView userScoreText;
    private MaterialButton scrollToUserButton;

    private LeaderboardAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String displayMode = "points"; // Default display mode (points, hours, streak)

    public GlobalLeaderboardFragment() {
        // Required empty public constructor
    }

    public static GlobalLeaderboardFragment newInstance(String displayMode) {
        GlobalLeaderboardFragment fragment = new GlobalLeaderboardFragment();
        Bundle args = new Bundle();
        args.putString("displayMode", displayMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            displayMode = getArguments().getString("displayMode", "points");
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_global_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        userRankCard = view.findViewById(R.id.userRankCard);
        userRankText = view.findViewById(R.id.userRankText);
        userScoreText = view.findViewById(R.id.userScoreText);
        scrollToUserButton = view.findViewById(R.id.scrollToUserButton);

        // Set up RecyclerView
        adapter = new LeaderboardAdapter(this);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        // Set display mode
        adapter.setDisplayMode(displayMode);

        // Hide user rank card initially
        userRankCard.setVisibility(View.GONE);

        // Set up scroll to user button
        scrollToUserButton.setOnClickListener(v -> scrollToCurrentUser());

        // Load leaderboard data
        loadLeaderboardData();
    }

    @Override
    public void onEntryClick(LeaderboardEntry entry) {
        // Handle click on leaderboard entry
        // Could show user profile or achievement details
    }

    public void updateDisplayMode(String mode) {
        this.displayMode = mode;
        if (adapter != null) {
            adapter.setDisplayMode(mode);
        }
        // Reload data with new sort
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        if (getActivity() == null) return;

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        userRankCard.setVisibility(View.GONE);

        // Determine which field to sort by based on display mode
        String sortField;
        switch (displayMode) {
            case "hours":
                sortField = "totalStudyTime";
                break;
            case "streak":
                sortField = "streakDays";
                break;
            case "points":
            default:
                sortField = "points";
                break;
        }

        // Query Firestore for users, sorted by selected field
        db.collection("users")
                .orderBy(sortField, Query.Direction.DESCENDING)
                .limit(LEADERBOARD_LIMIT)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    int userRank = -1;
                    LeaderboardEntry currentUserEntry = null;

                    // Process results
                    int rank = 1;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        LeaderboardEntry entry = new LeaderboardEntry(user, rank);

                        // Check if this is the current user
                        if (user.getUid().equals(currentUserId)) {
                            userRank = rank;
                            entry.setCurrentUser(true);
                            currentUserEntry = entry;
                        }

                        entries.add(entry);
                        rank++;
                    }

                    // Update UI
                    updateLeaderboardUI(entries, userRank, currentUserEntry);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading leaderboard", e);
                    showError("Failed to load leaderboard data");
                });
    }

    private void updateLeaderboardUI(List<LeaderboardEntry> entries, int userRank, LeaderboardEntry currentUserEntry) {
        if (getActivity() == null) return;

        progressBar.setVisibility(View.GONE);

        if (entries.isEmpty()) {
            showEmptyState("No leaderboard data available");
            return;
        }

        // Update adapter
        adapter.setEntries(entries);
        leaderboardRecyclerView.setVisibility(View.VISIBLE);

        if (userRank > 0 && currentUserEntry != null) {
            userRankCard.setVisibility(View.VISIBLE);
            userRankText.setText("Your rank: #" + userRank);

            // Set score text based on display mode
            switch (displayMode) {
                case "hours":
                    userScoreText.setText(currentUserEntry.getFormattedStudyTime() + " hours");
                    break;
                case "streak":
                    userScoreText.setText(currentUserEntry.getStreakDays() + " day streak");
                    break;
                case "points":
                default:
                    userScoreText.setText(String.format("%,d points", currentUserEntry.getPoints()));
                    break;
            }
        } else {
            userRankCard.setVisibility(View.GONE);
        }
    }

    private void scrollToCurrentUser() {
        int position = adapter.getCurrentUserPosition();
        if (position >= 0) {
            leaderboardRecyclerView.smoothScrollToPosition(position);
        }
    }

    private void showEmptyState(String message) {
        if (getActivity() == null) return;

        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
        userRankCard.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (getActivity() == null) return;

        showEmptyState(message);
    }
}