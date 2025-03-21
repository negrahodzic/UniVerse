package com.universe.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.model.Event;
import com.universe.android.model.Ticket;
import com.universe.android.util.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TicketDetailActivity extends AppCompatActivity {
    private static final String TAG = "TicketDetailActivity";

    // Views
    private TextView eventTitle;
    private TextView eventDate;
    private TextView eventLocation;
    private TextView ticketId;
    private TextView purchaseDate;
    private TextView ticketQuantity;
    private TextView pointsSpent;
    private Chip statusChip;
    private ImageView qrCodeImage;
    private TextView verificationCode;
    private MaterialButton shareButton;
    private MaterialButton addToCalendarButton;
    private MaterialButton directionsButton;

    // Data
    private Ticket ticket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply organisation theme
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.applyOrganisationTheme(this, orgId);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        // Get ticket from intent
        ticket = (Ticket) getIntent().getSerializableExtra("ticket");
        if (ticket == null) {
            Toast.makeText(this, "Error loading ticket details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Populate UI
        populateTicketDetails();

        // Load additional details from Firestore if needed
        loadAdditionalDetails();
    }

    private void initializeViews() {
        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ticket: " + ticket.getEvent().getTitle());
        }

        // Add organisation logo to toolbar
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.addLogoToToolbar(this, toolbar, orgId);
        }

        // Find views
        eventTitle = findViewById(R.id.eventTitle);
        eventDate = findViewById(R.id.eventDate);
        eventLocation = findViewById(R.id.eventLocation);
        ticketId = findViewById(R.id.ticketId);
        purchaseDate = findViewById(R.id.purchaseDate);
        statusChip = findViewById(R.id.statusChip);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        verificationCode = findViewById(R.id.verificationCode);
        shareButton = findViewById(R.id.shareButton);
        addToCalendarButton = findViewById(R.id.addToCalendarButton);
        directionsButton = findViewById(R.id.directionsButton);

        // Check if the optional views exist in the layout
        try {
            ticketQuantity = findViewById(R.id.ticketQuantity);
            pointsSpent = findViewById(R.id.pointsSpent);
        } catch (Exception e) {
            Log.d(TAG, "Optional views not found in layout");
        }

        // Setup button click listeners
        shareButton.setOnClickListener(v -> shareTicket());
        addToCalendarButton.setOnClickListener(v -> addToCalendar());
        directionsButton.setOnClickListener(v -> openDirections());
    }

    private void loadAdditionalDetails() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load additional ticket details from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("tickets")
                .document(ticket.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Update ticket with additional data
                        Integer numTickets = documentSnapshot.getLong("numberOfTickets") != null ?
                                documentSnapshot.getLong("numberOfTickets").intValue() : 1;
                        Integer ticketPrice = documentSnapshot.getLong("pointsPrice") != null ?
                                documentSnapshot.getLong("pointsPrice").intValue() : 0;

                        ticket.setNumberOfTickets(numTickets);
                        ticket.setPointsPrice(ticketPrice);

                        // Update UI with the new information
                        updateTicketQuantityAndPrice();
                    }
                });
    }

    private void updateTicketQuantityAndPrice() {
        if (ticketQuantity != null) {
            if (ticket.getNumberOfTickets() > 1) {
                ticketQuantity.setText("Quantity: " + ticket.getNumberOfTickets() + " tickets");
            } else {
                ticketQuantity.setText("Quantity: 1 ticket");
            }
        }

        if (pointsSpent != null && ticket.getPointsPrice() > 0) {
            int total = ticket.getNumberOfTickets() * ticket.getPointsPrice();
            pointsSpent.setText("Points spent: " + total);
        }
    }

    private void populateTicketDetails() {
        Event event = ticket.getEvent();

        // Set ticket info
        eventTitle.setText(event.getTitle());
        eventDate.setText(event.getFormattedDateTime());
        eventLocation.setText(event.getLocation() + "\n" + event.getAddress());
        ticketId.setText(ticket.getId());
        purchaseDate.setText(ticket.getPurchaseDate());

        // Set quantity info
        updateTicketQuantityAndPrice();

        // Set status chip
        statusChip.setText(ticket.getStatusText());
        statusChip.setChipBackgroundColorResource(ticket.getStatusColor());

        // Generate and display QR code
        Bitmap qrCode = ticket.getQrCodeBitmap(250, 250);
        if (qrCode != null) {
            qrCodeImage.setImageBitmap(qrCode);
        } else {
            qrCodeImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Set verification code
        verificationCode.setText(ticket.getVerificationCode());
    }

    private void shareTicket() {
        try {
            // First save the QR code to a temporary file
            Bitmap qrCode = ticket.getQrCodeBitmap(500, 500);
            if (qrCode == null) {
                Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
                return;
            }

            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "ticket_qr_" + ticket.getId() + ".png");

            FileOutputStream stream = new FileOutputStream(imageFile);
            qrCode.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Create file URI using FileProvider
            Uri imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    imageFile);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    "Ticket for " + ticket.getEvent().getTitle());

            String ticketMessage = "Event: " + ticket.getEvent().getTitle() + "\n" +
                    "Date: " + ticket.getEvent().getFormattedDateTime() + "\n" +
                    "Location: " + ticket.getEvent().getLocation() + "\n" +
                    "Ticket ID: " + ticket.getId() + "\n" +
                    "Verification Code: " + ticket.getVerificationCode();

            if (ticket.getNumberOfTickets() > 1) {
                ticketMessage += "\nQuantity: " + ticket.getNumberOfTickets() + " tickets";
            }

            shareIntent.putExtra(Intent.EXTRA_TEXT, ticketMessage);
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Launch share dialog
            startActivity(Intent.createChooser(shareIntent, "Share Ticket"));

        } catch (IOException e) {
            Toast.makeText(this, "Error sharing ticket: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCalendar() {
        Event event = ticket.getEvent();

        // Parse event date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();

        try {
            // Parse date
            Date eventDate = dateFormat.parse(event.getDate());
            startTime.setTime(eventDate);
            endTime.setTime(eventDate);

            // Parse time if available
            if (event.getTime() != null && !event.getTime().isEmpty()) {
                Date eventTime = timeFormat.parse(event.getTime());
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTime(eventTime);

                startTime.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                startTime.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));

                // Set end time to 2 hours after start by default
                endTime.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY) + 2);
                endTime.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            } else {
                // If no time specified, default to all-day event
                endTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Create calendar intent
            Intent calendarIntent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                    .putExtra(CalendarContract.Events.TITLE, event.getTitle());

            // Add ticket details to description
            String description = "Ticket ID: " + ticket.getId() + "\n" +
                    "Verification Code: " + ticket.getVerificationCode() + "\n";

            if (ticket.getNumberOfTickets() > 1) {
                description += "Number of Tickets: " + ticket.getNumberOfTickets() + "\n";
            }

            description += "\n" + (event.getDescription() != null ? event.getDescription() : "");

            calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, description)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION,
                            event.getLocation() + ", " + event.getAddress())
                    .putExtra(CalendarContract.Events.AVAILABILITY,
                            CalendarContract.Events.AVAILABILITY_BUSY);

            startActivity(calendarIntent);

        } catch (ParseException e) {
            Toast.makeText(this, "Error parsing event date: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openDirections() {
        Event event = ticket.getEvent();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}