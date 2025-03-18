package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.universe.android.fragment.SessionHistoryFragment;

public class SessionHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_container);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Study History");
        }

        // Initialize the SessionHistoryFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SessionHistoryFragment())
                    .commit();
        }

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navigation_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_study) {
                Intent intent = new Intent(this, StudySessionActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_events) {
                Intent intent = new Intent(this, EventsActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
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


}