package com.universe.android;

import static android.content.ContentValues.TAG;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.universe.android.adapter.ParticipantAdapter;
import com.universe.android.model.Participant;
import com.universe.android.model.StudySession;
import com.universe.android.repository.SessionRepository;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.NfcUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaitingRoomActivity extends AppCompatActivity implements NfcUtil.NfcTagCallback {
    private TextView sessionIdText;
    private TextView settingsText;
    private RecyclerView participantsList;
    private MaterialButton startButton;
    private MaterialButton showQrCodeButton;
    private ParticipantAdapter participantAdapter;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String sessionId;

    private UserRepository userRepository;
    private SessionRepository sessionRepository;

    // Session settings
    private int duration;
    private int points;

    private boolean isSessionCreated = false;

    private ListenerRegistration sessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_waiting_room);

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        sessionRepository = SessionRepository.getInstance();

        // Initialize NFC
        nfcAdapter = NfcUtil.initializeNfcAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device",
                    Toast.LENGTH_LONG).show();
        }
        pendingIntent = NfcUtil.createNfcPendingIntent(this);

        // Get session settings from intent
        Intent intent = getIntent();
        duration = intent.getIntExtra("duration", 3);
        points = intent.getIntExtra("points", 20);

        // Generate session ID
        sessionId = generateSessionId();

        // Initialize views
        initializeViews();
        setupParticipantsList();
        updateSessionInfo();

        // Pre-create session document AFTER participants are set up
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                createStudySessionRecord();
            }
        });

        startButton.setOnClickListener(v -> startSession());
        showQrCodeButton.setOnClickListener(v -> showSessionQrCode());
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        sessionIdText = findViewById(R.id.sessionIdText);
        settingsText = findViewById(R.id.settingsText);
        participantsList = findViewById(R.id.participantsList);
        startButton = findViewById(R.id.startButton);
        showQrCodeButton = findViewById(R.id.showQrCodeButton);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupParticipantsList() {
        participantAdapter = new ParticipantAdapter();
        participantsList.setLayoutManager(new LinearLayoutManager(this));
        participantsList.setAdapter(participantAdapter);

        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                // Add host as first participant with actual username
                List<Participant> participants = new ArrayList<>();
                Participant hostParticipant = new Participant(user.getUsername() + " (Host)", true);
                hostParticipant.setUserId(user.getUid());
                hostParticipant.setActualUsername(user.getUsername());

                participants.add(hostParticipant);
                participantAdapter.setParticipants(participants);

                // Ensure session is created before setting up listener
                createStudySessionRecord();

                // Set up real-time listener for session updates
                setupSessionUpdateListener();
            }
        });
    }

    private void setupSessionUpdateListener() {
        sessionListener = sessionRepository.listenToSessionUpdates(sessionId, session -> {
            // Ensure the session is not null and has participants
            if (session != null && session.getParticipants() != null) {
                List<Participant> updatedParticipants = new ArrayList<>();
                for (Map<String, String> participantMap : session.getParticipants()) {
                    // Add (Host) tag for the host
                    String displayName = participantMap.get("username") +
                            (participantMap.get("userId").equals(session.getHostId()) ? " (Host)" : "");

                    Participant participant = new Participant(displayName, true);
                    participant.setUserId(participantMap.get("userId"));
                    participant.setActualUsername(participantMap.get("username"));
                    participant.setNfcId(participantMap.get("nfcId"));
                    updatedParticipants.add(participant);
                }

                runOnUiThread(() -> {
                    participantAdapter.setParticipants(updatedParticipants);
                });
            }
        });
    }

    private void createStudySessionRecord() {
        if (isSessionCreated) return;

        String currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == null) return;

        // Ensure participants adapter is not null and has participants
        if (participantAdapter == null || participantAdapter.getParticipants().isEmpty()) {
            Log.e(TAG, "Participants not initialized");
            return;
        }

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
                currentUserId,
                participants,
                new Date(),
                duration
        );
        studySession.setCompleted(false);

        // Set points to be awarded
        studySession.setPointsAwarded(points);

        // Save to Firestore using repository
        sessionRepository.createSession(studySession)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session pre-created: " + sessionId);
                    isSessionCreated = true;
                    showQrCodeButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to pre-create session: " + e.getMessage());
                    Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show();
                    showQrCodeButton.setEnabled(false);
                    isSessionCreated = false;
                });
    }

    private void updateSessionInfo() {
        sessionIdText.setText("Session #" + sessionId);

        // Create a simplified settings text
        String durationText = (duration < 60) ?
                duration + " seconds" :
                (duration / 60) + " minutes";

        settingsText.setText("Duration: " + durationText + "\n" +
                "Points reward: " + points + " points");
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void showSessionQrCode() {
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                Intent intent = new Intent(this, SessionQrActivity.class);
                intent.putExtra("sessionId", sessionId);
                intent.putExtra("hostId", user.getUid());
                intent.putExtra("hostUsername", user.getUsername());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcUtil.enableForegroundDispatch(this, nfcAdapter, pendingIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NfcUtil.disableForegroundDispatch(nfcAdapter, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NfcUtil.processNfcIntent(intent, this);
    }

    @Override
    public void onNfcTagDiscovered(Tag tag, String serialNumber) {
        // Get user with this NFC ID
        userRepository.getUserByNfcId(serialNumber).addOnSuccessListener(user -> {
            if (user == null) {
                Toast.makeText(this, "Unregistered NFC tag. Please register your tag in your profile.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String userId = user.getUid();
            String participantName = user.getUsername();

            // Check if this NFC tag belongs to the host
            if (userId.equals(userRepository.getCurrentUserId())) {
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
                newParticipant.setActualUsername(participantName);

                // Create participant data for Firestore
                Map<String, String> participantMap = new HashMap<>();
                participantMap.put("userId", userId);
                participantMap.put("username", participantName);
                participantMap.put("nfcId", serialNumber);

                // Add to Firestore first, then update local UI
                sessionRepository.addParticipantToSession(sessionId, participantMap)
                        .addOnSuccessListener(aVoid -> {
                            // Update local list after successful Firebase update
                            currentParticipants.add(newParticipant);
                            participantAdapter.setParticipants(currentParticipants);
                            Toast.makeText(WaitingRoomActivity.this, participantName + " joined!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(WaitingRoomActivity.this, "Error adding participant: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error reading NFC tag: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void startSession() {
        // Update the session in Firestore to mark it as started
        sessionRepository.startSession(sessionId)
                .addOnSuccessListener(aVoid -> {
                    // Get the latest session data from Firestore to ensure all participants are included
                    sessionRepository.getSessionById(sessionId)
                            .addOnSuccessListener(session -> {
                                // Start the active session activity for the host
                                Intent intent = new Intent(this, ActiveSessionActivity.class);
                                intent.putExtra("sessionId", sessionId);
                                intent.putExtra("duration", duration);
                                intent.putExtra("points", points);

                                // Convert participants from Firestore to a serializable format
                                ArrayList<HashMap<String, String>> participantData = new ArrayList<>();
                                if (session != null && session.getParticipants() != null) {
                                    for (Map<String, String> participantMap : session.getParticipants()) {
                                        HashMap<String, String> data = new HashMap<>();
                                        data.put("name", participantMap.get("username"));
                                        data.put("userId", participantMap.get("userId"));
                                        data.put("actualUsername", participantMap.get("username"));
                                        participantData.add(data);
                                    }
                                }

                                intent.putExtra("participantData", participantData);
                                intent.putExtra("isParticipant", false); // Host is starting the session
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to get updated session data", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to start session", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) {
            sessionListener.remove();
        }
    }
}