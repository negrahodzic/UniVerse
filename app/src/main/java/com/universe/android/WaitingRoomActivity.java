package com.universe.android;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.model.Participant;
import com.universe.android.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaitingRoomActivity extends AppCompatActivity {
    private TextView sessionIdText;
    private TextView settingsText;
    private RecyclerView participantsList;
    private MaterialButton startButton;
    private ParticipantAdapter participantAdapter;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String sessionId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Session settings
    private int duration;
    private boolean usesBluetooth;
    private boolean usesWifi;
    private boolean usesLocation;
    private boolean usesBreaks;
    private int breakInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_waiting_room);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device",
                    Toast.LENGTH_LONG).show();
        }
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // Get session settings from intent
        Intent intent = getIntent();
        duration = intent.getIntExtra("duration", 60);
        usesBluetooth = intent.getBooleanExtra("bluetooth", false);
        usesWifi = intent.getBooleanExtra("wifi", false);
        usesLocation = intent.getBooleanExtra("location", false);
        usesBreaks = intent.getBooleanExtra("breaks", false);
        breakInterval = intent.getIntExtra("breakInterval", 45);

        // Generate session ID
        sessionId = generateSessionId();

        // Initialize views
        initializeViews();
        setupParticipantsList();
        updateSessionInfo();

        startButton.setOnClickListener(v -> startSession());
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        sessionIdText = findViewById(R.id.sessionIdText);
        settingsText = findViewById(R.id.settingsText);
        participantsList = findViewById(R.id.participantsList);
        startButton = findViewById(R.id.startButton);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupParticipantsList() {
        participantAdapter = new ParticipantAdapter();
        participantsList.setLayoutManager(new LinearLayoutManager(this));
        participantsList.setAdapter(participantAdapter);

        // Add host as first participant
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant("You (Host)", true));
        participantAdapter.setParticipants(participants);
    }

    private void updateSessionInfo() {
        sessionIdText.setText("Session #" + sessionId);

        StringBuilder settings = new StringBuilder();
        settings.append("Duration: ").append(duration).append(" minutes\n");
        settings.append("Features: ");
        if (usesBluetooth) settings.append("Bluetooth, ");
        if (usesWifi) settings.append("WiFi, ");
        if (usesLocation) settings.append("Location, ");
        if (usesBreaks) settings.append("Breaks (").append(breakInterval).append("min), ");

        if (settings.toString().endsWith(", ")) {
            settings.setLength(settings.length() - 2);
        }

        settingsText.setText(settings.toString());
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent,
                    new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)}, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            handleNfcTag(tag);
        }
    }

    private void handleNfcTag(Tag tag) {
        // Get NFC serial number
        byte[] tagId = tag.getId();
        String serialNumber = bytesToHex(tagId);


        // Query Firestore for user with this NFC ID
        db.collection("users")
                .whereEqualTo("nfcId", serialNumber)
                .limit(1)  // Only need one result
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Participant> currentParticipants = participantAdapter.getParticipants();

                    // Default name for anonymous user
                    String participantName = "Anonymous (NFC: " + serialNumber.substring(0, 4) + ")";

                    // If user is found, use their username instead
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        participantName = user.getUsername();
                    }

                    // Check if this participant (by NFC ID) already joined
                    boolean alreadyJoined = false;
                    for (Participant p : currentParticipants) {
                        // TODO: change this to actually check for users nfcId and not name substring
                        if (p.getName().contains(serialNumber.substring(0, 4))) {
                            alreadyJoined = true;
                            break;
                        }
                    }

                    if (alreadyJoined) {
                        Toast.makeText(this, "This device already joined!", Toast.LENGTH_SHORT).show();
                    } else {
                        currentParticipants.add(new Participant(participantName, true));
                        participantAdapter.setParticipants(currentParticipants);
                        Toast.makeText(this, participantName + " joined!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error reading NFC tag", Toast.LENGTH_SHORT).show();
                });
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void startSession() {
        Intent intent = new Intent(this, ActiveSessionActivity.class);
        intent.putExtra("duration", duration);
        intent.putExtra("bluetooth", usesBluetooth);
        intent.putExtra("wifi", usesWifi);
        intent.putExtra("location", usesLocation);
        intent.putExtra("breaks", usesBreaks);
        intent.putExtra("breakInterval", breakInterval);

        // Pass participants
        ArrayList<String> participantNames = new ArrayList<>();
        for (Participant p : participantAdapter.getParticipants()) {
            participantNames.add(p.getName());
        }
        intent.putStringArrayListExtra("participants", participantNames);

        startActivity(intent);
        finish();
    }
}