package com.universe.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.universe.android.R;
import com.universe.android.TicketDetailActivity;
import com.universe.android.adapter.TicketAdapter;
import com.universe.android.model.Event;
import com.universe.android.model.Ticket;
import com.universe.android.service.EventService;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsFragment extends Fragment {
    private static final String TAG = "MyTicketsFragment";

    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private FirebaseFirestore db;
    private EventService eventService;

    public MyTicketsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore and EventService
        db = FirebaseFirestore.getInstance();
        eventService = new EventService(requireContext());

        // Find views
        recyclerView = view.findViewById(R.id.ticketsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TicketAdapter(ticket -> {
            // Handle ticket click - open ticket detail
            Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
            intent.putExtra("ticket", ticket);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        // Load user's tickets
        loadTickets();
    }

    private void loadTickets() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            showEmptyState("Please sign in to view your tickets");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Query Firestore for user's tickets
        CollectionReference ticketsRef = db.collection("users")
                .document(userId)
                .collection("tickets");

        // Order by purchase timestamp, most recent first
        ticketsRef.orderBy("purchaseTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() == null || !isAdded()) return;

                    if (task.isSuccessful()) {
                        List<Ticket> tickets = new ArrayList<>();

                        if (task.getResult().isEmpty()) {
                            // No tickets found, show empty state
                            showEmptyState("You don't have any tickets yet");
                            return;
                        }

                        // Process each ticket document
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String ticketId = document.getId();
                            String eventId = document.getString("eventId");
                            String purchaseDate = document.getString("purchaseDate");
                            String verificationCode = document.getString("verificationCode");
                            boolean isUsed = document.getBoolean("used") != null ?
                                    document.getBoolean("used") : false;

                            // Get stored event details
                            String eventTitle = document.getString("eventTitle");
                            String eventDate = document.getString("eventDate");
                            String eventTime = document.getString("eventTime");
                            String eventLocation = document.getString("eventLocation");
                            String eventAddress = document.getString("eventAddress");

                            // Create a basic event with the stored details
                            Event basicEvent = new Event(
                                    eventId,
                                    eventTitle != null ? eventTitle : "Unknown Event",
                                    "Loading event details...",
                                    eventDate != null ? eventDate : "Unknown date",
                                    eventTime != null ? eventTime : "",
                                    eventLocation != null ? eventLocation : "Unknown location",
                                    eventAddress != null ? eventAddress : "Unknown address",
                                    0,
                                    R.drawable.ic_launcher_foreground
                            );

                            // Create ticket with basic event details
                            Ticket ticket = new Ticket(
                                    ticketId, basicEvent, purchaseDate, verificationCode, isUsed);

                            // Add additional ticket details if available
                            if (document.getLong("numberOfTickets") != null) {
                                ticket.setNumberOfTickets(document.getLong("numberOfTickets").intValue());
                            }

                            if (document.getLong("pointsPrice") != null) {
                                ticket.setPointsPrice(document.getLong("pointsPrice").intValue());
                            }

                            tickets.add(ticket);
                        }

                        // Display tickets immediately with basic info
                        displayTickets(tickets);

                        // Then fetch complete event details for each ticket
                        for (Ticket ticket : tickets) {
                            fetchEventDetails(ticket);
                        }

                    } else {
                        // Error loading tickets
                        Log.e(TAG, "Error getting tickets: ", task.getException());
                        showEmptyState("Couldn't load tickets.\nTap to retry.");
                        emptyStateText.setOnClickListener(v -> loadTickets());
                    }
                });
    }

    private void fetchEventDetails(Ticket ticket) {
        eventService.getEventById(ticket.getEvent().getId(), new EventService.EventCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (getActivity() == null || !isAdded()) return;

                if (!events.isEmpty()) {
                    // Update ticket with full event details
                    Event event = events.get(0);
                    ticket.setEvent(event);

                    // Refresh adapter to show updated event details
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching event details: " + error);
                // The ticket will still show with basic event info
            }
        });
    }

    private void displayTickets(List<Ticket> tickets) {
        progressBar.setVisibility(View.GONE);

        if (tickets.isEmpty()) {
            showEmptyState("You don't have any tickets yet");
        } else {
            adapter.setTickets(tickets);
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        progressBar.setVisibility(View.GONE);
        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh tickets when returning to fragment
        loadTickets();
    }
}