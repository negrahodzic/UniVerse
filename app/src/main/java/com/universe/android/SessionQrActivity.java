package com.universe.android;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.universe.android.util.QrCodeUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionQrActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private TextView sessionIdText;
    private MaterialButton closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_session_qr);

        // Get session information from intent
        String sessionId = getIntent().getStringExtra("sessionId");
        String hostId = getIntent().getStringExtra("hostId");
        String hostUsername = getIntent().getStringExtra("hostUsername");

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        sessionIdText = findViewById(R.id.sessionIdText);
        closeButton = findViewById(R.id.closeButton);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Session QR Code");
        }

        // Set session ID text
        sessionIdText.setText("Session #" + sessionId);

        // Generate QR code
        generateQrCode(sessionId, hostId, hostUsername);

        // Set close button listener
        closeButton.setOnClickListener(v -> finish());
    }

    private void generateQrCode(String sessionId, String hostId, String hostUsername) {
        try {
            Log.d("SessionQrActivity", "Generating QR code for session: " + sessionId);

            // Create JSON object with session information
            JSONObject sessionData = new JSONObject();
            sessionData.put("type", "session");
            sessionData.put("sessionId", sessionId);
            sessionData.put("hostId", hostId);
            sessionData.put("hostUsername", hostUsername);

            Log.d("SessionQrActivity", "QR code data: " + sessionData.toString());

            // Generate QR code
            Bitmap qrCode = QrCodeUtil.generateQrCodeFromJson(sessionData, 250, 250);

            // Display QR code
            if (qrCode != null) {
                qrCodeImageView.setImageBitmap(qrCode);
            }
        } catch (JSONException e) {
            Log.e("SessionQrActivity", "Error generating QR code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}