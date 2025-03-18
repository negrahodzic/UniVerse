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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.model.Participant;
import com.universe.android.model.StudySession;
import com.universe.android.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private int duration; // in seconds for testing
    private int points;   // points rewarded for this session

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
        duration = intent.getIntExtra("duration", 3); // Default to 3 seconds for testing
        points = intent.getIntExtra("points", 20);   // Default to 20 points

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

        // Get current user data to add host with actual username
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Add host as first participant with actual username
                            List<Participant> participants = new ArrayList<>();
                            Participant hostParticipant = new Participant(user.getUsername() + " (Host)", true);
                            hostParticipant.setUserId(currentUser.getUid());
                            hostParticipant.setActualUsername(user.getUsername());

                            participants.add(hostParticipant);
                            participantAdapter.setParticipants(participants);
                        }
                    });
        }
    }

    private void updateSessionInfo() {
        sessionIdText.setText("Session #" + sessionId);

        // Create a simplified settings text
        String durationText = (duration < 60) ?
                duration + " seconds" : // For testing use seconds
                (duration / 60) + " minutes"; // For production use minutes

        settingsText.setText("Duration: " + durationText + "\n" +
                "Points reward: " + points + " points");
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

        // Get current user ID (host)
        String currentUserId = auth.getCurrentUser().getUid();

        // Query Firestore for user with this NFC ID
        db.collection("users")
                .whereEqualTo("nfcId", serialNumber)
                .limit(1)  // Only need one result
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // If no user found with this NFC ID, show error
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Unregistered NFC tag. Please register your tag in your profile.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Get user from query result
                    User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                    String userId = user.getUid();
                    String participantName = user.getUsername();

                    // Check if this NFC tag belongs to the host
                    if (userId.equals(currentUserId)) {
                        Toast.makeText(this, "This is your own NFC tag", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if this participant already joined (by user ID)
                    List<Participant> currentParticipants = participantAdapter.getParticipants();
                    boolean alreadyJoined = false;

                    for (Participant p : currentParticipants) {
                        // Check by userId if available
                        if (p.getUserId() != null && p.getUserId().equals(userId)) {
                            alreadyJoined = true;
                            break;
                        }
                    }

                    if (alreadyJoined) {
                        Toast.makeText(this, "This user has already joined!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create new participant with userId stored
                        Participant newParticipant = new Participant(participantName, true);
                        newParticipant.setUserId(userId);
                        newParticipant.setNfcId(serialNumber);
                        newParticipant.setActualUsername(participantName);  // Store actual username

                        currentParticipants.add(newParticipant);
                        participantAdapter.setParticipants(currentParticipants);
                        Toast.makeText(this, participantName + " joined!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error reading NFC tag: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
        // Create study session record in Firestore
        createStudySessionRecord();

        // Start the active session activity
        Intent intent = new Intent(this, ActiveSessionActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("duration", duration);
        intent.putExtra("points", points);

        // Convert participants to a serializable format
        ArrayList<HashMap<String, String>> participantData = new ArrayList<>();
        for (Participant p : participantAdapter.getParticipants()) {
            HashMap<String, String> data = new HashMap<>();
            data.put("name", p.getName());
            data.put("userId", p.getUserId());
            data.put("actualUsername", p.getActualUsername());
            participantData.add(data);
        }

        intent.putExtra("participantData", participantData);
        startActivity(intent);
        finish();
    }

    private void createStudySessionRecord() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Create participants list
        List<Map<String, String>> participants = new ArrayList<>();
        for (Participant p : participantAdapter.getParticipants()) {
            Map<String, String> participantMap = new HashMap<>();
            participantMap.put("userId", p.getUserId());
            participantMap.put("username", p.getActualUsername());
            participantMap.put("nfcId", p.getNfcId());
            participants.add(participantMap);
        }

        // Create study session object
        StudySession studySession = new StudySession(
                sessionId,
                currentUser.getUid(),
                participants,
                new Date(),
                duration
        );

        // Set points to be awarded
        studySession.setPointsAwarded(points);

        // Save to Firestore - using "sessions" collection
        db.collection("sessions")
                .document(sessionId)
                .set(studySession)
                .addOnSuccessListener(aVoid -> {
                    // Session created successfully, no need to handle here
                })
                .addOnFailureListener(e -> {
                    // Session creation failed, but still continue
                    Toast.makeText(this, "Failed to record session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}