package com.universe.android;

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
import com.universe.android.util.NavigationHelper;

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

        // Set up bottom navigation using helper
        NavigationHelper.setupBottomNavigation(
                this,
                findViewById(R.id.bottomNav),
                R.id.navigation_leaderboard
        );
    }

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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}