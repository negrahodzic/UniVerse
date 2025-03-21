package com.universe.android;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanOptions;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.QrCodeUtil;
import com.universe.android.util.ThemeManager;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.activity.result.ActivityResultLauncher;

public class FriendQrActivity extends AppCompatActivity implements QrCodeUtil.QrScanCallback {

    private ImageView qrCodeView;
    private TextView instructionsText;
    private MaterialButton scanQrButton;
    private MaterialButton closeButton;

    private ActivityResultLauncher<ScanOptions> qrScanLauncher;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Apply organisation theme
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.applyOrganisationTheme(this, orgId);
        }


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friend_qr);

        userRepository = UserRepository.getInstance();

        // Setup QR scanner
        qrScanLauncher = QrCodeUtil.setupScanner(this, this);

        // Initialize views
        initializeViews();

        // Generate QR code for current user
        generateQrCode();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        qrCodeView = findViewById(R.id.qrCodeView);
        instructionsText = findViewById(R.id.instructionsText);
        scanQrButton = findViewById(R.id.scanQrButton);
        closeButton = findViewById(R.id.closeButton);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Friend");
        }

        scanQrButton.setOnClickListener(v -> startQrScanner());
        closeButton.setOnClickListener(v -> finish());
    }

    private void generateQrCode() {
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                Bitmap qrCode = QrCodeUtil.generateFriendQrCode(
                        user.getUid(),
                        user.getUsername(),
                        300, 300);

                if (qrCode != null) {
                    qrCodeView.setImageBitmap(qrCode);
                    instructionsText.setText("Show this QR code to a friend to connect.\n\n" +
                            "Or tap the button below to scan a friend's code.");
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void startQrScanner() {
        QrCodeUtil.startScanner(qrScanLauncher, "Scan your friend's QR code");
    }

    @Override
    public void onScanSuccess(String result) {
        try {
            JSONObject data = QrCodeUtil.parseQrCodeJson(result);

            if (data != null && "friend".equals(data.optString("type"))) {
                String friendId = data.getString("userId");
                String friendUsername = data.getString("username");

                // Add friend in Firestore
                userRepository.addFriend(this, friendId)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this,
                                    "You are now friends with " + friendUsername,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this,
                                    "Error adding friend: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this,
                        "Invalid QR code. Please scan a friend's QR code.",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this,
                    "Error reading QR code: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onScanCancelled() {
        // User cancelled the scan
        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
    }
}