package com.universe.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.universe.android.manager.UserManager;
import com.universe.android.model.Event;
import com.universe.android.model.Ticket;
import com.universe.android.service.EventService;

public class EventDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "EventDetailActivity";

    // Views
    private ImageView eventImage;
    private TextView eventDate;
    private TextView eventLocation;
    private TextView eventDescription;
    private TextView eventPrice;
    private TextView availabilityText;
    private TextView quantityText;
    private MaterialButton decreaseButton;
    private MaterialButton increaseButton;
    private MaterialButton bookButton;
    private MaterialButton directionsButton;

    // Data
    private Event event;
    private int ticketQuantity = 1;
    private int userPoints = 0;
    private EventService eventService;

    // Map
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize service
        eventService = new EventService(this);

        // Get event from intent
        event = (Event) getIntent().getSerializableExtra("event");
        if (event == null) {
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Get user points
        loadUserPoints();

        // Populate UI
        populateEventDetails();
    }

    private void initializeViews() {
        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(event.getTitle());
        }

        // Find views
        eventImage = findViewById(R.id.eventImage);
        eventDate = findViewById(R.id.eventDate);
        eventLocation = findViewById(R.id.eventLocation);
        eventDescription = findViewById(R.id.eventDescription);
        eventPrice = findViewById(R.id.eventPrice);
        availabilityText = findViewById(R.id.availabilityText);
        quantityText = findViewById(R.id.quantityText);
        decreaseButton = findViewById(R.id.decreaseButton);
        increaseButton = findViewById(R.id.increaseButton);
        bookButton = findViewById(R.id.bookButton);
        directionsButton = findViewById(R.id.directionsButton);

        // Setup quantity buttons
        decreaseButton.setOnClickListener(v -> {
            if (ticketQuantity > 1) {
                ticketQuantity--;
                updateQuantityUI();
            }
        });

        increaseButton.setOnClickListener(v -> {
            if (ticketQuantity < event.getAvailableTickets()) {
                ticketQuantity++;
                updateQuantityUI();
            } else {
                Toast.makeText(this, "Maximum available tickets reached", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup book button
        bookButton.setOnClickListener(v -> {
            if (!event.isAvailable()) {
                Toast.makeText(this, "This event is sold out", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalCost = event.getPointsPrice() * ticketQuantity;
            if (totalCost > userPoints) {
                Toast.makeText(this, "You don't have enough points. Need " + totalCost + " points.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showBookingConfirmationDialog(totalCost);
        });

        // Setup directions button
        directionsButton.setOnClickListener(v -> {
            openDirections();
        });
    }

    private void populateEventDetails() {
        // Set image
        Glide.with(this)
                .load(event.getImageResource())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(eventImage);

        // Set text fields
        String dateTimeText = event.getDate();
        if (event.getTime() != null && !event.getTime().isEmpty()) {
            dateTimeText += " at " + event.getTime();
        }
        eventDate.setText(dateTimeText);
        eventLocation.setText(event.getLocation() + "\n" + event.getAddress());
        eventDescription.setText(event.getDescription());
        eventPrice.setText(event.getPointsPrice() + " points per ticket");

        // Set availability
        if (event.isAvailable()) {
            availabilityText.setText(event.getAvailableTickets() + " tickets available");
            availabilityText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            bookButton.setEnabled(true);
        } else {
            availabilityText.setText("SOLD OUT");
            availabilityText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            bookButton.setEnabled(false);
        }

        // Initial quantity
        updateQuantityUI();
    }

    private void updateQuantityUI() {
        quantityText.setText(String.valueOf(ticketQuantity));

        // Update book button text with total
        int totalCost = event.getPointsPrice() * ticketQuantity;
        bookButton.setText("Book Now (" + totalCost + " points)");

        // Disable decrease button if at minimum
        decreaseButton.setEnabled(ticketQuantity > 1);

        // Disable increase button if at maximum
        increaseButton.setEnabled(ticketQuantity < event.getAvailableTickets());
    }

    private void loadUserPoints() {
        UserManager.getInstance().getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                userPoints = user.getPoints();
                Log.d(TAG, "User has " + userPoints + " points");
            }
        });
    }

    private void showBookingConfirmationDialog(int totalCost) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Booking")
                .setMessage("Book " + ticketQuantity + " ticket" +
                        (ticketQuantity > 1 ? "s" : "") + " for " +
                        totalCost + " points?")
                .setPositiveButton("Book Now", (dialog, which) -> {
                    bookTickets();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void bookTickets() {
        // Show loading
        bookButton.setEnabled(false);
        bookButton.setText("Processing...");

        // Call event service to book tickets
        eventService.bookEvent(event.getId(), ticketQuantity, new EventService.BookingCallback() {
            @Override
            public void onSuccess(Ticket ticket) {
                // Deduct points from user
                int totalCost = event.getPointsPrice() * ticketQuantity;
                UserManager.getInstance().getCurrentUserData().addOnSuccessListener(user -> {
                    if (user != null) {
                        int newPoints = user.getPoints() - totalCost;
                        UserManager.getInstance().updatePoints(newPoints);

                        // Show success and go to ticket view
                        Intent intent = new Intent(EventDetailActivity.this, TicketDetailActivity.class);
                        intent.putExtra("ticket", ticket);
                        startActivity(intent);

                        // Finish this activity
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EventDetailActivity.this,
                        "Error booking: " + error, Toast.LENGTH_SHORT).show();
                // Reset button
                bookButton.setEnabled(true);
                bookButton.setText("Book Now");
            }
        });
    }

    private void openDirections() {
        if (event.getLatitude() != 0 && event.getLongitude() != 0) {
            // Open Google Maps directions
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                    event.getLatitude() + "," + event.getLongitude() +
                    "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback to browser if Google Maps not installed
                Uri fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                        event.getLatitude() + "," + event.getLongitude());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
                startActivity(browserIntent);
            }
        } else {
            // Use address if coordinates not available
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(event.getAddress()));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "No maps application found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Add marker for event location
        if (event.getLatitude() != 0 && event.getLongitude() != 0) {
            LatLng location = new LatLng(event.getLatitude(), event.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(event.getLocation()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        } else {
            // Hide map card if no location available
            findViewById(R.id.mapCard).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}