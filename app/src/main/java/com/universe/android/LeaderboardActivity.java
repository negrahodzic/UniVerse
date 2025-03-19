package com.universe.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.universe.android.adapter.LeaderboardPagerAdapter;

/**
 * Activity for displaying leaderboards
 */
public class LeaderboardActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialButtonToggleGroup filterToggleGroup;
    private MaterialButton btnFilterPoints;
    private MaterialButton btnFilterHours;
    private MaterialButton btnFilterStreak;

    private LeaderboardPagerAdapter pagerAdapter;
    private String currentDisplayMode = "points"; // Default display mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        filterToggleGroup = findViewById(R.id.filterToggleGroup);
        btnFilterPoints = findViewById(R.id.btnFilterPoints);
        btnFilterHours = findViewById(R.id.btnFilterHours);
        btnFilterStreak = findViewById(R.id.btnFilterStreak);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leaderboards");
        }

        // Setup ViewPager
        pagerAdapter = new LeaderboardPagerAdapter(this, currentDisplayMode);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Global" : "Friends");
        }).attach();

        // Set up filter toggle group
        setupFilterButtons();

        // Set up bottom navigation
        setupBottomNavigation();
    }

    /**
     * Set up filter toggle buttons
     */
    private void setupFilterButtons() {
        // Set initial selection
        btnFilterPoints.setChecked(true);

        // Set listeners
        filterToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFilterPoints) {
                    currentDisplayMode = "points";
                } else if (checkedId == R.id.btnFilterHours) {
                    currentDisplayMode = "hours";
                } else if (checkedId == R.id.btnFilterStreak) {
                    currentDisplayMode = "streak";
                }

                // Update adapter with new display mode
                pagerAdapter.updateDisplayMode(currentDisplayMode);
            }
        });
    }

    /**
     * Set up bottom navigation
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navigation_leaderboard);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;

            if (itemId == R.id.navigation_dashboard) {
                intent = new Intent(this, DashboardActivity.class);
            } else if (itemId == R.id.navigation_study) {
                intent = new Intent(this, StudySessionActivity.class);
            } else if (itemId == R.id.navigation_events) {
                intent = new Intent(this, EventsActivity.class);
            } else if (itemId == R.id.navigation_leaderboard) {
                return true; // Already on leaderboard screen
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, LeaderboardActivity.class);
        context.startActivity(intent);
    }
}