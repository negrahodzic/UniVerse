package com.universe.android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.universe.android.R;
import com.android.volley.toolbox.Volley;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.adapter.EventAdapter;
import com.universe.android.model.Event;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class UpcomingEventsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EventAdapter adapter;

    private static final String API_URL = "https://java-war-test.onrender.com/webresources/events";


    public UpcomingEventsFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upcoming_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventAdapter(event -> {
            // TODO: Handle event booking
        });
        recyclerView.setAdapter(adapter);

        loadSampleEvents();

        loadEventsFromApi();
    }

    private void loadSampleEvents() {
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


    private void loadEventsFromApi() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, API_URL, null,
                response -> {
                    try {
                        List<Event> events = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject eventJson = response.getJSONObject(i);

                            // Parse venue
                            JSONObject venueJson = eventJson.getJSONObject("venue");

                            // Parse datetime
                            String dateTimeStr = eventJson.getString("eventDateTime");
                            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                            Date date = apiFormat.parse(dateTimeStr);
                            String displayDate = displayFormat.format(date);

                            events.add(new Event(
                                    eventJson.getString("eventId"),
                                    eventJson.getString("eventName"),
                                    displayDate,
                                    venueJson.getString("address"),
                                    eventJson.getInt("availableTickets"),

                                    R.drawable.ic_launcher_background  // Default image
                            ));
                        }
                        adapter.setEvents(events);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Error parsing events: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(), "Error loading events: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loadSampleEvents(); // Fallback to sample events if API fails
                });

        queue.add(jsonArrayRequest);
    }
}