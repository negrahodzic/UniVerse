package com.universe.android;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.NfcUtil;

public class FriendNfcActivity extends AppCompatActivity implements NfcUtil.NfcTagCallback {

    private TextView statusText;
    private TextView instructionsText;
    private MaterialButton closeButton;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friend_nfc);

        userRepository = UserRepository.getInstance();

        // Initialize NFC
        nfcAdapter = NfcUtil.initializeNfcAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device",
                    Toast.LENGTH_LONG).show();
        }
        pendingIntent = NfcUtil.createNfcPendingIntent(this);

        // Initialize views
        initializeViews();

        // Check if NFC is enabled
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            statusText.setText("NFC is disabled. Please enable it in your device settings.");
        } else if (nfcAdapter != null) {
            statusText.setText("Ready to scan");
        } else {
            statusText.setText("NFC is not supported on this device");
        }
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        statusText = findViewById(R.id.statusText);
        instructionsText = findViewById(R.id.instructionsText);
        closeButton = findViewById(R.id.closeButton);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Friend with NFC");
        }

        closeButton.setOnClickListener(v -> finish());
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
        statusText.setText("NFC Tag detected: " + serialNumber);

        // Get user with this NFC ID
        userRepository.getUserByNfcId(serialNumber).addOnSuccessListener(user -> {
            if (user == null) {
                statusText.setText("No user registered with this NFC tag");
                return;
            }

            String friendId = user.getUid();
            String friendUsername = user.getUsername();

            // Don't allow adding yourself
            if (friendId.equals(userRepository.getCurrentUserId())) {
                statusText.setText("This is your own NFC tag");
                return;
            }

            // Add friend in Firestore
            userRepository.addFriend(this, friendId)
                    .addOnSuccessListener(aVoid -> {
                        statusText.setText("Success! You are now friends with " + friendUsername);
                        Toast.makeText(this,
                                "You are now friends with " + friendUsername,
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("Already friends")) {
                            statusText.setText("You are already friends with " + friendUsername);
                        } else {
                            statusText.setText("Error adding friend: " + errorMessage);
                            Toast.makeText(this,
                                    "Error adding friend: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e -> {
            statusText.setText("Error reading NFC tag: " + e.getMessage());
        });
    }
}