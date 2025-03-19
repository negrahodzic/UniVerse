package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.universe.android.adapter.EventsPagerAdapter;
import com.universe.android.util.NetworkUtil;

public class EventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_events);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        EventsPagerAdapter pagerAdapter = new EventsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(position == 0 ? "Upcoming" : "My Tickets");
                }
        ).attach();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navigation_events);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_study) {
                Intent intent = new Intent(this, StudySessionActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_events) {
                return true;
            } else if (itemId == R.id.navigation_leaderboard) {
                Intent intent = new Intent(this, LeaderboardActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Check if Docker API is available
        checkDockerApiAvailability();
    }

    private void checkDockerApiAvailability() {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            showNetworkError("No network connection");
            return;
        }

        NetworkUtil.isDockerApiAvailable(this, isAvailable -> {
            if (!isAvailable) {
                runOnUiThread(() -> {
                    showNetworkError("Cannot connect to event API");
                });
            }
        });
    }

    private void showNetworkError(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setAction("Settings", v -> {
                    // Open network settings
                    startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                });
        snackbar.show();
    }
}