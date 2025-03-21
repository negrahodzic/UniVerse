package com.universe.android.util;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.universe.android.DashboardActivity;
import com.universe.android.EventsActivity;
import com.universe.android.LeaderboardActivity;
import com.universe.android.ProfileActivity;
import com.universe.android.R;
import com.universe.android.StudySessionActivity;

public class NavigationHelper {
    public static void setupBottomNavigation(Activity activity, BottomNavigationView bottomNav, int selectedItemId) {
        // Set the selected item
        bottomNav.setSelectedItemId(selectedItemId);

        // Set up navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == selectedItemId) {
                return true; // Already on this screen
            }

            Intent intent = getActivityForMenuItem(activity, item);
            if (intent != null) {
                activity.startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private static Intent getActivityForMenuItem(Activity activity, MenuItem item) {
        int itemId = item.getItemId();
        Class<?> targetActivity = null;

        if (itemId == R.id.navigation_dashboard) {
            targetActivity = DashboardActivity.class;
        } else if (itemId == R.id.navigation_study) {
            targetActivity = StudySessionActivity.class;
        } else if (itemId == R.id.navigation_events) {
            targetActivity = EventsActivity.class;
        } else if (itemId == R.id.navigation_leaderboard) {
            targetActivity = LeaderboardActivity.class;
        } else if (itemId == R.id.navigation_profile) {
            targetActivity = ProfileActivity.class;
        }

        if (targetActivity != null && !targetActivity.equals(activity.getClass())) {
            return new Intent(activity, targetActivity);
        }

        return null;
    }

    public static void setupToolbarWithBack(Activity activity, String title) {
        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ((AppCompatActivity) activity).setSupportActionBar(toolbar);
            androidx.appcompat.app.ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(title);
            }

            // Apply organisation logo to toolbar if applicable
            String orgId = ThemeManager.getCurrentOrg(activity);
            if (!orgId.isEmpty()) {
                ThemeManager.addLogoToToolbar(activity, toolbar, orgId);
            }

            toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        }
    }
}