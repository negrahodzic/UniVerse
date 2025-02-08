package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.universe.android.adapter.EventPreviewAdapter;
import com.universe.android.model.Event;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

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
                "March 15, 2024",
                "University Main Square",
                500,
                R.drawable.ic_launcher_background
        ));
        events.add(new Event(
                "2",
                "Career Fair 2024",
                "March 20, 2024",
                "Student Union Building",
                200,
                R.drawable.ic_launcher_background
        ));
        adapter.setEvents(events);
    }
}