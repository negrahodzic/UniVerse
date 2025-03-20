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
import com.universe.android.R;
import com.universe.android.adapter.LeaderboardAdapter;
import com.universe.android.model.LeaderboardEntry;
import com.universe.android.model.User;
import com.universe.android.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class GlobalLeaderboardFragment extends Fragment implements LeaderboardAdapter.OnLeaderboardEntryClickListener {
    private static final String TAG = "GlobalLeaderboard";
    private static final int LEADERBOARD_LIMIT = 100;

    private RecyclerView leaderboardRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private MaterialCardView userRankCard;
    private TextView userRankText;
    private TextView userScoreText;
    private MaterialButton scrollToUserButton;

    private LeaderboardAdapter adapter;
    private UserRepository userRepository;
    private String displayMode = "points";

    public GlobalLeaderboardFragment() {
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

        userRepository = UserRepository.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_global_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        userRankCard = view.findViewById(R.id.userRankCard);
        userRankText = view.findViewById(R.id.userRankText);
        userScoreText = view.findViewById(R.id.userScoreText);
        scrollToUserButton = view.findViewById(R.id.scrollToUserButton);

        adapter = new LeaderboardAdapter(this);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        adapter.setDisplayMode(displayMode);

        userRankCard.setVisibility(View.GONE);

        // Add padding to the bottom of the recycler view to avoid overlap with user rank card
        leaderboardRecyclerView.setPadding(0, 0, 0,
                getResources().getDimensionPixelSize(R.dimen.user_rank_card_height));
        leaderboardRecyclerView.setClipToPadding(false);

        scrollToUserButton.setOnClickListener(v -> scrollToCurrentUser());

        loadLeaderboardData();
    }

    @Override
    public void onEntryClick(LeaderboardEntry entry) {
        // Could show user profile or achievement details
    }

    @Override
    public void onEntryLongClick(LeaderboardEntry entry) {
        // Not used in global leaderboard
    }

    public void updateDisplayMode(String mode) {
        this.displayMode = mode;
        if (adapter != null) {
            adapter.setDisplayMode(mode);
        }
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        if (getActivity() == null) return;

        progressBar.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        userRankCard.setVisibility(View.GONE);

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

        userRepository.getGlobalLeaderboard(sortField, LEADERBOARD_LIMIT)
                .addOnSuccessListener(users -> {
                    if (getActivity() == null) return;

                    List<LeaderboardEntry> entries = new ArrayList<>();
                    int userRank = -1;
                    LeaderboardEntry currentUserEntry = null;

                    int rank = 1;
                    for (User user : users) {
                        LeaderboardEntry entry = new LeaderboardEntry(user, rank);

                        if (userRepository.getCurrentUserId().equals(user.getUid())) {
                            userRank = rank;
                            entry.setCurrentUser(true);
                            currentUserEntry = entry;
                        }

                        entries.add(entry);
                        rank++;
                    }

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

        adapter.setEntries(entries);
        leaderboardRecyclerView.setVisibility(View.VISIBLE);

        if (userRank > 0 && currentUserEntry != null) {
            userRankCard.setVisibility(View.VISIBLE);
            userRankText.setText("Your rank: #" + userRank);

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