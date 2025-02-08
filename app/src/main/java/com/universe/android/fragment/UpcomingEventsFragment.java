package com.universe.android.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.universe.android.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.adapter.EventAdapter;
import com.universe.android.model.Event;
import java.util.ArrayList;
import java.util.List;

public class UpcomingEventsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EventAdapter adapter;

    public UpcomingEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
}