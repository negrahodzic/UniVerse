package com.universe.android;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.universe.android.repository.UserRepository;
import com.universe.android.util.NavigationHelper;
import com.universe.android.util.NfcUtil;
import com.universe.android.util.StatsHelper;

import com.bumptech.glide.Glide;
import com.universe.android.util.ThemeManager;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameText;
    private TextView organisationText;
    private TextView totalPointsText;
    private TextView currentLevelText;
    private TextView totalSessionsText;
    private TextView totalHoursText;
    private TextView avgSessionLengthText;
    private TextView eventsAttendedText;
    private TextView pointsSpentText;

    private TextView streakDaysText;
    private TextView consistencyScoreText;
    private TextView achievementsEarnedText;
    private TextView friendsCountText;
    private TextView lastActivityText;

    private MaterialButton logoutButton;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private ImageView profileImage;
    private MaterialButton editUsernameButton;
    private MaterialButton changePasswordButton;

    private MaterialButton registerNfcIdButton;
    private MaterialButton viewAchievementsButton;

    private UserRepository userRepository;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private NfcUtil.NfcTagCallback nfcCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply organisation theme
        String orgId = ThemeManager.getCurrentOrg(this);
        if (!orgId.isEmpty()) {
            ThemeManager.applyOrganisationTheme(this, orgId);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views
        initializeViews();

        nfcAdapter = NfcUtil.initializeNfcAdapter(this);
        pendingIntent = NfcUtil.createNfcPendingIntent(this);

        // Load user data
        loadUserData();

        // Setup bottom navigation using helper
        NavigationHelper.setupBottomNavigation(
                this,
                findViewById(R.id.bottomNav),
                R.id.navigation_profile
        );
    }

    private void initializeViews() {
        // Basic profile info
        usernameText = findViewById(R.id.usernameText);
        organisationText = findViewById(R.id.organisationText);
        profileImage = findViewById(R.id.profileImage);

        // Stats fields
        totalPointsText = findViewById(R.id.totalPointsText);
        currentLevelText = findViewById(R.id.currentLevelText);
        totalSessionsText = findViewById(R.id.totalSessionsText);
        totalHoursText = findViewById(R.id.totalHoursText);
        avgSessionLengthText = findViewById(R.id.avgSessionLengthText);
        eventsAttendedText = findViewById(R.id.eventsAttendedText);
        pointsSpentText = findViewById(R.id.pointsSpentText);

        // Try to find new stats fields if they exist in the layout
        try {
            streakDaysText = findViewById(R.id.streakDaysText);
            consistencyScoreText = findViewById(R.id.consistencyScoreText);
            achievementsEarnedText = findViewById(R.id.achievementsEarnedText);
            friendsCountText = findViewById(R.id.friendsCountText);
            lastActivityText = findViewById(R.id.lastActivityText);
        } catch (Exception e) {
            Log.d("ProfileActivity", "Some new stats fields not found in layout");
        }

        // Buttons
        logoutButton = findViewById(R.id.logoutButton);
        editUsernameButton = findViewById(R.id.editUsernameButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        registerNfcIdButton = findViewById(R.id.registerNfcIdButton);

        // Try to find view achievements button if it exists
        try {
            viewAchievementsButton = findViewById(R.id.viewAchievementsButton);
            if (viewAchievementsButton != null) {
                viewAchievementsButton.setOnClickListener(v -> showAchievementsScreen());
            }
        } catch (Exception e) {
            Log.d("ProfileActivity", "View achievements button not found in layout");
        }

        logoutButton.setOnClickListener(v -> logout());
        setupProfileImageClick();
        setupEditButtons();
    }

    private void loadUserData() {
        // Initialize user stats
        userRepository.initializeUserStats()
                .addOnSuccessListener(aVoid -> {
                    Log.d("ProfileActivity", "User stats initialized");
                    // Now load user data
                    loadUserDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error initializing user stats", e);
                    // Still try to load user details
                    loadUserDetails();
                });
    }

    private void loadUserDetails() {
        userRepository.getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                // Set basic info
                usernameText.setText(user.getUsername());
                organisationText.setText(user.getOrganisationId());

                // Set standard stats
                totalPointsText.setText(String.valueOf(user.getPoints()));
                currentLevelText.setText(String.valueOf(user.calculateLevel()));

                // Study stats
                long totalHours = user.getTotalStudyTime() / 60; // Convert minutes to hours
                totalHoursText.setText(String.valueOf(totalHours));
                totalSessionsText.setText(String.valueOf(user.getSessionsCompleted()));

                // Calculate average session length
                int avgMinutes = StatsHelper.calculateAvgSessionLength(
                        user.getTotalStudyTime(), user.getSessionsCompleted());
                avgSessionLengthText.setText(StatsHelper.formatStudyTime(avgMinutes));

                // Event stats
                eventsAttendedText.setText(String.valueOf(user.getEventsAttended()));
                pointsSpentText.setText("0"); // Placeholder

                // Set new enhanced stats if available in the layout
                if (streakDaysText != null) {
                    streakDaysText.setText(user.getStreakDays() + " days");
                }

                if (consistencyScoreText != null) {
                    consistencyScoreText.setText(user.getConsistencyScore() + "%");
                }

                if (achievementsEarnedText != null) {
                    achievementsEarnedText.setText(String.valueOf(user.getAchievementCount()));
                }

                if (friendsCountText != null) {
                    friendsCountText.setText(String.valueOf(user.getFriendCount()));
                }

                if (lastActivityText != null) {
                    String lastActivity = "Never";
                    if (user.getLastStudyDate() > 0) {
                        lastActivity = StatsHelper.formatDate(
                                user.getLastStudyDate(), "MMM d, yyyy");
                    }
                    lastActivityText.setText(lastActivity);
                }

                // Load profile image if available
                if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
                    // Convert Base64 to Bitmap
                    byte[] decodedString = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    Glide.with(this)
                            .load(decodedByte)
                            .circleCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(profileImage);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
        });
    }

    private void logout() {
        userRepository.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupProfileImageClick() {
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
        });
    }

    private void setupEditButtons() {
        registerNfcIdButton.setOnClickListener(v -> showRegisterNfcIdDialog());
        editUsernameButton.setOnClickListener(v -> showUsernameDialog());
        changePasswordButton.setOnClickListener(v -> showPasswordDialog());
    }

    /**
     * Show achievements screen
     */
    private void showAchievementsScreen() {
        Intent intent = new Intent(this, AchievementsActivity.class);
        startActivity(intent);
    }

    private void showUsernameDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_username, null);
        TextInputEditText usernameInput = dialogView.findViewById(R.id.usernameInput);

        builder.setView(dialogView)
                .setTitle("Edit Username")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = usernameInput.getText().toString();
                    if (!newUsername.isEmpty()) {
                        updateUsername(newUsername);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRegisterNfcIdDialog() {
        // Create a custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_nfc, null);

        // Get references to views in the dialog layout
        TextInputEditText nfcIdInput = dialogView.findViewById(R.id.nfcIdInput);
        Button tapNfcButton = dialogView.findViewById(R.id.tapNfcButton);
        TextView statusText = dialogView.findViewById(R.id.statusText);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        // Create the dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Register NFC Card")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Will be set up later
                .setNegativeButton("Cancel", null)
                .create();

        // Check if NFC is available
        if (nfcAdapter == null) {
            tapNfcButton.setEnabled(false);
            statusText.setText("NFC is not available on this device");
        } else if (!nfcAdapter.isEnabled()) {
            tapNfcButton.setEnabled(false);
            statusText.setText("NFC is disabled. Please enable it in settings");
        }

        // Create an NFC tag callback to handle NFC scan results
        nfcCallback = new NfcUtil.NfcTagCallback() {
            @Override
            public void onNfcTagDiscovered(Tag tag, String serialNumber) {
                // Update UI with the scanned NFC ID
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("NFC Card detected: " + serialNumber);
                    nfcIdInput.setText(serialNumber);
                });
            }
        };

        // Set up tap button
        tapNfcButton.setOnClickListener(v -> {
            statusText.setText("Tap your NFC card to the back of the device...");
            progressBar.setVisibility(View.VISIBLE);

            // Enable NFC foreground dispatch to capture NFC events
            NfcUtil.enableForegroundDispatch(this, nfcAdapter, pendingIntent);
        });

        // Show the dialog
        dialog.show();

        // Override the positive button to validate input before saving
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nfcId = nfcIdInput.getText().toString().trim();
            if (nfcId.isEmpty()) {
                nfcIdInput.setError("Please enter an NFC ID or tap a card");
                return;
            }

            // Save the NFC ID
            updateNfcId(nfcId);
            dialog.dismiss();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            uploadProfileImage(selectedImageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        userRepository.uploadProfileImage(imageUri, this).addOnSuccessListener(aVoid -> {
            // Reload the user data to get the new Base64 image
            loadUserData();
            Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error uploading image", e);
        });
    }

    private void updateUsername(String newUsername) {
        userRepository.updateUsername(newUsername).addOnSuccessListener(aVoid -> {
            usernameText.setText(newUsername);
            Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error updating username", e);
        });
    }

    private void updateNfcId(String newNfcId) {
        userRepository.updateNfcId(newNfcId).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "NFC ID updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update NFC ID", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error updating NFC ID", e);
        });
    }

    private void showPasswordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPassword = dialogView.findViewById(R.id.currentPassword);
        TextInputEditText newPassword = dialogView.findViewById(R.id.newPassword);
        TextInputEditText confirmPassword = dialogView.findViewById(R.id.confirmPassword);

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Save", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String currentPass = currentPassword.getText().toString();
                String newPass = newPassword.getText().toString();
                String confirmPass = confirmPassword.getText().toString();

                if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(ProfileActivity.this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // First reauthenticate
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

                    user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
                        // Now update password
                        updatePassword(newPass);
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(ProfileActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            Log.e("ProfileActivity", "Reauthentication failed", e);
                        }
                    });
                }
            });
        });

        dialog.show();
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userRepository.updatePassword(newPassword, user.getEmail()).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                Log.e("ProfileActivity", "Error updating password", e);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to profile screen
        loadUserData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Process the NFC intent if there is a callback registered
        if (nfcCallback != null) {
            NfcUtil.processNfcIntent(intent, nfcCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            NfcUtil.disableForegroundDispatch(nfcAdapter, this);
        }
    }
}