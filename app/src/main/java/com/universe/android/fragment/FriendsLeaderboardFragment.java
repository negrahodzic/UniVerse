package com.universe.android.fragment;

import android.content.Intent;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.universe.android.FriendNfcActivity;
import com.universe.android.FriendQrActivity;
import com.universe.android.R;
import com.universe.android.adapter.LeaderboardAdapter;
import com.universe.android.model.LeaderboardEntry;
import com.universe.android.model.User;
import com.universe.android.repository.UserRepository;

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
    private MaterialCardView addFriendCard;

    private LeaderboardAdapter adapter;
    private UserRepository userRepository;
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

        userRepository = UserRepository.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        addFriendQrButton = view.findViewById(R.id.addFriendQrButton);
        addFriendNfcButton = view.findViewById(R.id.addFriendNfcButton);
        addFriendCard = view.findViewById(R.id.addFriendCard);

        adapter = new LeaderboardAdapter(this);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        // Add padding to recycler view to avoid overlap with add friend card
        leaderboardRecyclerView.setPadding(0, 0, 0,
                getResources().getDimensionPixelSize(R.dimen.user_rank_card_height));
        leaderboardRecyclerView.setClipToPadding(false);

        adapter.setDisplayMode(displayMode);

        addFriendQrButton.setOnClickListener(v -> showQRCodeFriendActivity());
        addFriendNfcButton.setOnClickListener(v -> showNFCFriendActivity());

        loadFriendsLeaderboardData();
    }

    @Override
    public void onEntryClick(LeaderboardEntry entry) {
        Toast.makeText(getContext(), "Viewing " + entry.getUsername() + "'s profile", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEntryLongClick(LeaderboardEntry entry) {
        // Only offer remove option for friends (not for self)
        if (!entry.isCurrentUser()) {
            showRemoveFriendDialog(entry);
        }
    }

    private void showRemoveFriendDialog(LeaderboardEntry entry) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Do you want to remove " + entry.getUsername() + " from your friends list?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeFriend(entry.getUserId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFriend(String friendId) {
        userRepository.removeFriend(friendId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Friend removed successfully", Toast.LENGTH_SHORT).show();
                    // Reload the friends list to reflect changes
                    loadFriendsLeaderboardData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to remove friend: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    public void updateDisplayMode(String mode) {
        this.displayMode = mode;
        if (adapter != null) {
            adapter.setDisplayMode(mode);
        }
        loadFriendsLeaderboardData();
    }

    private void loadFriendsLeaderboardData() {
        if (getActivity() == null) return;

        progressBar.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        userRepository.getFriendsLeaderboard(displayMode)
                .addOnSuccessListener(users -> {
                    if (getActivity() == null) return;

                    sortUsers(users, displayMode);

                    List<LeaderboardEntry> entries = new ArrayList<>();
                    for (int i = 0; i < users.size(); i++) {
                        User user = users.get(i);
                        LeaderboardEntry entry = new LeaderboardEntry(user, i + 1);

                        if (user.getUid().equals(userRepository.getCurrentUserId())) {
                            entry.setCurrentUser(true);
                        }

                        entries.add(entry);
                    }

                    updateLeaderboardUI(entries);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading friends data", e);
                    showError("Failed to load friends data");
                });
    }

    private void sortUsers(List<User> users, String sortField) {
        Comparator<User> comparator;
        switch (sortField) {
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
    }

    private void updateLeaderboardUI(List<LeaderboardEntry> entries) {
        if (getActivity() == null) return;

        progressBar.setVisibility(View.GONE);

        if (entries.isEmpty()) {
            showEmptyState("No friends data available");
            return;
        }

        adapter.setEntries(entries);
        leaderboardRecyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }

    private void showQRCodeFriendActivity() {
        Intent intent = new Intent(getActivity(), FriendQrActivity.class);
        startActivity(intent);
    }

    private void showNFCFriendActivity() {
        Intent intent = new Intent(getActivity(), FriendNfcActivity.class);
        startActivity(intent);
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

    @Override
    public void onResume() {
        super.onResume();
        loadFriendsLeaderboardData();
    }
}