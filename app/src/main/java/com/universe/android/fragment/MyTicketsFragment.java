package com.universe.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.R;
import com.universe.android.adapter.TicketAdapter;
import com.universe.android.model.Event;
import com.universe.android.model.Ticket;
import java.util.ArrayList;
import java.util.List;

public class MyTicketsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TicketAdapter adapter;

    public MyTicketsFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.ticketsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TicketAdapter();
        recyclerView.setAdapter(adapter);

        loadSampleTickets();
    }

    private void loadSampleTickets() {
        List<Ticket> tickets = new ArrayList<>();

        // Create a sample event
        Event event = new Event(
                "1",
                "Campus Music Festival",
                "March 15, 2024",
                "University Main Square",
                500,
                R.drawable.ic_launcher_background
        );

        // Create a ticket for this event
        tickets.add(new Ticket(
                "TICKET-001",
                event,
                "February 1, 2024",
                "qr_placeholder",
                false
        ));

        adapter.setTickets(tickets);
    }
}