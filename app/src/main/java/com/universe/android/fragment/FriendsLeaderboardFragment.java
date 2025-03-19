package com.universe.android.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.R;
import com.universe.android.adapter.LeaderboardAdapter;
import com.universe.android.model.LeaderboardEntry;
import com.universe.android.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FriendsLeaderboardFragment extends Fragment implements LeaderboardAdapter.OnLeaderboardEntryClickListener {
    private static final String TAG = "FriendsLeaderboard";

    private RecyclerView leaderboardRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private MaterialButton addFriendQrButton;
    private MaterialButton addFriendNfcButton;

    private LeaderboardAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String displayMode = "points"; // Default display mode (points, hours, streak)

    public FriendsLeaderboardFragment() {
    }

    public static FriendsLeaderboardFragment newInstance(String displayMode) {
        FriendsLeaderboardFragment fragment = new FriendsLeaderboardFragment();
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
        return inflater.inflate(R.layout.fragment_friends_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        addFriendQrButton = view.findViewById(R.id.addFriendQrButton);
        addFriendNfcButton = view.findViewById(R.id.addFriendNfcButton);

        // Set up RecyclerView
        adapter = new LeaderboardAdapter(this);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        // Set display mode
        adapter.setDisplayMode(displayMode);

        // Set up buttons
        addFriendQrButton.setOnClickListener(v -> showQRCodeFriendDialog());
        addFriendNfcButton.setOnClickListener(v -> showNFCFriendDialog());

        // Load friends leaderboard data
        loadFriendsLeaderboardData();
    }

    @Override
    public void onEntryClick(LeaderboardEntry entry) {
        // Handle click on leaderboard entry
        // Could show friend profile or achievement details
    }

    public void updateDisplayMode(String mode) {
        this.displayMode = mode;
        if (adapter != null) {
            adapter.setDisplayMode(mode);
        }
        // Reload data with new sort
        loadFriendsLeaderboardData();
    }

    private void loadFriendsLeaderboardData() {
        if (getActivity() == null || currentUserId == null) return;

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // First, get current user's friends list
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser == null || currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
                        // No friends yet
                        showEmptyState("You haven't added any friends yet");
                        return;
                    }

                    // Include current user in the list
                    List<String> userIds = new ArrayList<>(currentUser.getFriends());
                    userIds.add(currentUserId); // Add current user

                    // Get friends data
                    loadFriendsData(userIds, currentUser);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    showError("Failed to load friends data");
                });
    }

    private void loadFriendsData(List<String> friendIds, User currentUser) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        List<User> usersData = new ArrayList<>();

        // Add current user
        LeaderboardEntry currentUserEntry = new LeaderboardEntry(currentUser, 0); // Rank will be set later
        currentUserEntry.setCurrentUser(true);
        usersData.add(currentUser);

        // If there are no other friends, just show current user
        if (friendIds.size() <= 1) {
            entries.add(currentUserEntry);
            currentUserEntry.setRank(1);
            updateLeaderboardUI(entries);
            return;
        }

        // Remove current user ID from the IDs list since its already added it
        friendIds.remove(currentUserId);

        // Now fetch data for each friend
        for (String friendId : friendIds) {
            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User friend = documentSnapshot.toObject(User.class);
                        if (friend != null) {
                            usersData.add(friend);

                            // If collected all users, sort and display
                            if (usersData.size() == friendIds.size() + 1) { // +1 for current user
                                createSortedLeaderboard(usersData);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading friend data", e);
                        // Continue with other friends
                    });
        }
    }

    private void createSortedLeaderboard(List<User> users) {
        if (getActivity() == null) return;

        // Sort users based on display mode
        Comparator<User> comparator;
        switch (displayMode) {
            case "hours":
                comparator = (u1, u2) -> Long.compare(u2.getTotalStudyTime(), u1.getTotalStudyTime());
                break;
            case "streak":
                comparator = (u1, u2) -> Integer.compare(u2.getStreakDays(), u1.getStreakDays());
                break;
            case "points":
            default:
                comparator = (u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints());
                break;
        }

        Collections.sort(users, comparator);

        // Create leaderboard entries with ranks
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            LeaderboardEntry entry = new LeaderboardEntry(user, i + 1);

            // Mark current user
            if (user.getUid().equals(currentUserId)) {
                entry.setCurrentUser(true);
            }

            entries.add(entry);
        }

        // Update UI with sorted entries
        updateLeaderboardUI(entries);
    }


    private void updateLeaderboardUI(List<LeaderboardEntry> entries) {
        if (getActivity() == null) return;

        progressBar.setVisibility(View.GONE);

        if (entries.isEmpty()) {
            showEmptyState("No friends data available");
            return;
        }

        // Update adapter
        adapter.setEntries(entries);
        leaderboardRecyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }


    private void showQRCodeFriendDialog() {
        Toast.makeText(getContext(), "QR code friend feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showNFCFriendDialog() {
        Toast.makeText(getContext(), "NFC friend feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState(String message) {
        if (getActivity() == null) return;

        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
        progressBar.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (getActivity() == null) return;

        showEmptyState(message);
    }
}