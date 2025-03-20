package com.universe.android;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.adapter.AchievementAdapter;
import com.universe.android.model.Achievement;
import com.universe.android.repository.AchievementRepository;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.NavigationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AchievementsActivity extends AppCompatActivity implements AchievementAdapter.OnAchievementClickListener {

    private RecyclerView achievementsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private TextView completionText;

    private AchievementAdapter adapter;
    private UserRepository userRepository;
    private AchievementRepository achievementRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        achievementRepository = AchievementRepository.getInstance();

        // Initialize views
        initializeViews();

        // Setup toolbar
        NavigationHelper.setupToolbarWithBack(this, "Achievements");

        // Setup recycler view
        setupRecyclerView();

        // Load achievements
        loadAchievements();
    }

    private void initializeViews() {
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        completionText = findViewById(R.id.completionText);
    }

    private void setupRecyclerView() {
        adapter = new AchievementAdapter(this);
        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        achievementsRecyclerView.setAdapter(adapter);
    }

    private void loadAchievements() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        achievementsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // Get current user data
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user == null) {
                showError("Error loading user data");
                return;
            }

            // Get all achievements from repository
            Map<String, Achievement> allAchievements = achievementRepository.getAllAchievements();
            List<Achievement> achievements = new ArrayList<>(allAchievements.values());

            // Mark earned achievements
            for (Achievement achievement : achievements) {
                if (user.hasAchievement(achievement.getId())) {
                    achievement.setEarned(true);
                }
            }

            // Calculate completion rate
            int earnedCount = 0;
            for (Achievement achievement : achievements) {
                if (achievement.isEarned()) {
                    earnedCount++;
                }
            }

            int completionPercentage = achievements.isEmpty() ? 0 :
                    (earnedCount * 100) / achievements.size();

            completionText.setText(String.format("Completion: %d/%d (%d%%)", earnedCount, achievements.size(), completionPercentage));

            // Update UI
            if (achievements.isEmpty()) {
                showEmptyState("No achievements available");
            } else {
                adapter.setAchievements(achievements);
                progressBar.setVisibility(View.GONE);
                achievementsRecyclerView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            showError("Failed to load achievements: " + e.getMessage());
        });
    }

    private void showEmptyState(String message) {
        progressBar.setVisibility(View.GONE);
        achievementsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void showError(String message) {
        showEmptyState(message);
    }

    @Override
    public void onAchievementClick(Achievement achievement) {
        // Show achievement details dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(achievement.getTitle())
                .setMessage(achievement.getDescription())
                .setIcon(achievement.getIconResource())
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}