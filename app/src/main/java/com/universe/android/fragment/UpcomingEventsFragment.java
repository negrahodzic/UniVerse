package com.universe.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.universe.android.EventDetailActivity;
import com.universe.android.R;
import com.universe.android.adapter.EventAdapter;
import com.universe.android.model.Event;
import com.universe.android.service.EventService;

import java.util.List;

public class UpcomingEventsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private EventService eventService;

    public UpcomingEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upcoming_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize EventService
        eventService = new EventService(requireContext());

        // Find views
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventAdapter(event -> {
            // Handle event click - open event detail
            Intent intent = new Intent(requireContext(), EventDetailActivity.class);
            intent.putExtra("event", event);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        // Load events from API
        loadEvents();
    }

    private void loadEvents() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // Call our EventService
        eventService.getUpcomingEvents(new EventService.EventCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (getActivity() == null || !isAdded()) return;

                // Hide progress bar
                progressBar.setVisibility(View.GONE);

                if (events.isEmpty()) {
                    // Show empty state
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Show events
                    adapter.setEvents(events);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null || !isAdded()) return;

                // Hide progress bar
                progressBar.setVisibility(View.GONE);

                // Show error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();

                // Show empty state with error message
                emptyStateText.setText("Couldn't load events.\nTap to retry.");
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setOnClickListener(v -> loadEvents());
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh events when returning to fragment
        if (adapter != null && adapter.getItemCount() == 0) {
            loadEvents();
        }
    }
}