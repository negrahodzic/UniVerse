package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.universe.android.adapter.EventPreviewAdapter;
import com.universe.android.manager.UserManager;
import com.universe.android.model.Event;

import java.util.ArrayList;
import java.util.List;

import android.widget.TextView;

public class DashboardActivity extends AppCompatActivity {
    private TextView pointsText;
    private TextView levelText;
    private TextView rankText;
    private TextView sessionsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize views
        pointsText = findViewById(R.id.pointsText);
        levelText = findViewById(R.id.levelText);
        rankText = findViewById(R.id.rankText);
        sessionsText = findViewById(R.id.sessionsText);
        MaterialButton startSessionButton = findViewById(R.id.startSessionButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        RecyclerView eventsPreviewList = findViewById(R.id.eventsPreviewList);

        eventsPreviewList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        EventPreviewAdapter eventAdapter = new EventPreviewAdapter(event -> {
            Intent intent = new Intent(this, EventsActivity.class);
            startActivity(intent);
        });

        eventsPreviewList.setAdapter(eventAdapter);


        // Load user data
        loadUserData();

        // Load events
        loadSampleEvents(eventAdapter);

        startSessionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudySessionActivity.class);
            startActivity(intent);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                return true;
            } else if (itemId == R.id.navigation_study) {
                Intent intent = new Intent(this, StudySessionActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_events) {
                Intent intent = new Intent(this, EventsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void loadSampleEvents(EventPreviewAdapter adapter) {
        List<Event> events = new ArrayList<>();
        events.add(new Event(
                "1",
                "Campus Music Festival",
                "Join us for a night of amazing performances by student bands and professional artists.",
                "March 15, 2025",
                "7:30 PM",
                "University Main Square",
                "123 University Avenue, Nottingham NG1 1AA",
                500,
                R.drawable.ic_launcher_background
        ));
        events.add(new Event(
                "2",
                "Career Fair 2025",
                "Connect with top employers and explore internship and job opportunities.",
                "March 20, 2025",
                "10:00 AM",
                "Student Union Building",
                "456 University Boulevard, Nottingham NG7 2RD",
                200,
                R.drawable.ic_launcher_background
        ));
        adapter.setEvents(events);
    }

    private void loadUserData() {
        UserManager.getInstance().getCurrentUserData()
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        pointsText.setText("Points: " + user.getPoints());
                        levelText.setText("Level: " + calculateLevel(user.getPoints()));
                        sessionsText.setText("Sessions: " + (user.getTotalStudyTime() / 60));

                        // You might want to implement rank calculation later
                        rankText.setText("Rank: #-");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private int calculateLevel(int points) {
        // TODO
        return Math.max(1, points / 1000 + 1);
    }
}