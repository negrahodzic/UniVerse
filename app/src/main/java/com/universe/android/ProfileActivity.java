package com.universe.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.universe.android.manager.UserManager;

import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameText;
    private TextView organisationText;
    private TextView totalPointsText;
    private TextView currentLevelText;
    private TextView globalRankText;
    private TextView totalSessionsText;
    private TextView totalHoursText;
    private TextView avgSessionLengthText;
    private TextView eventsAttendedText;
    private TextView pointsSpentText;

    private MaterialButton logoutButton;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private ImageView profileImage;
    private MaterialButton editUsernameButton;
    private MaterialButton changePasswordButton;
    private MaterialButton changeOrgButton;

    private MaterialButton registerNfcIdButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        organisationText = findViewById(R.id.organisationText);
        totalPointsText = findViewById(R.id.totalPointsText);
        currentLevelText = findViewById(R.id.currentLevelText);
        globalRankText = findViewById(R.id.globalRankText);
        totalSessionsText = findViewById(R.id.totalSessionsText);
        totalHoursText = findViewById(R.id.totalHoursText);
        avgSessionLengthText = findViewById(R.id.avgSessionLengthText);
        eventsAttendedText = findViewById(R.id.eventsAttendedText);
        pointsSpentText = findViewById(R.id.pointsSpentText);
        logoutButton = findViewById(R.id.logoutButton);
        profileImage = findViewById(R.id.profileImage);
        editUsernameButton = findViewById(R.id.editUsernameButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        changeOrgButton = findViewById(R.id.changeOrgButton);
        registerNfcIdButton = findViewById(R.id.registerNfcIdButton);

        logoutButton.setOnClickListener(v -> logout());
        setupProfileImageClick();
        setupEditButtons();
    }

    private void loadUserData() {
        UserManager.getInstance().getCurrentUserData().addOnSuccessListener(user -> {
            if (user != null) {
                // Set basic info
                usernameText.setText(user.getUsername());
                organisationText.setText(user.getOrganisationId());

                // Set stats
                totalPointsText.setText(String.valueOf(user.getPoints()));
                currentLevelText.setText(String.valueOf(calculateLevel(user.getPoints())));
                globalRankText.setText("#-"); // TODO: Implement rank calculation

                // Study stats
                long totalHours = user.getTotalStudyTime() / 60; // Convert minutes to hours
                totalHoursText.setText(String.valueOf(totalHours));

                // Set other stats to 0 for now
                totalSessionsText.setText("0");
                avgSessionLengthText.setText("0h");
                eventsAttendedText.setText("0");
                pointsSpentText.setText("0");

                if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
                    // Convert Base64 to Bitmap
                    byte[] decodedString = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    Glide.with(this).load(decodedByte).circleCrop().placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_launcher_foreground).into(profileImage);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
        });
    }

    private int calculateLevel(int points) {
        return Math.max(1, points / 1000 + 1);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (itemId == R.id.navigation_study) {
                startActivity(new Intent(this, StudySessionActivity.class));
                return true;
            } else if (itemId == R.id.navigation_events) {
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }

    private void logout() {
        UserManager.getInstance().signOut();
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
        changeOrgButton.setOnClickListener(v -> showOrganisationDialog());
    }

    private void showUsernameDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_username, null);
        TextInputEditText usernameInput = dialogView.findViewById(R.id.usernameInput);

        builder.setView(dialogView).setTitle("Edit Username").setPositiveButton("Save", (dialog, which) -> {
            String newUsername = usernameInput.getText().toString();
            if (!newUsername.isEmpty()) {
                updateUsername(newUsername);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showRegisterNfcIdDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_nfc_id, null);
        TextInputEditText nfcIdInput = dialogView.findViewById(R.id.nfcIdInput);

        builder.setView(dialogView).setTitle("Register NFC id").setPositiveButton("Save", (dialog, which) -> {
            String newNfcId = nfcIdInput.getText().toString();
            if (!newNfcId.isEmpty()) {
                updateNfcId(newNfcId);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showOrganisationDialog() {
        Log.e("TODO", "TODO showOrganisationDialog");
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
        UserManager.getInstance().uploadProfileImage(imageUri, this).addOnSuccessListener(aVoid -> {
            // Reload the user data to get the new Base64 image
            loadUserData();
            Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error uploading image", e);
        });
    }

    private void updateUsername(String newUsername) {
        UserManager.getInstance().updateUsername(newUsername).addOnSuccessListener(aVoid -> {
            usernameText.setText(newUsername);
            Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error updating username", e);
        });
    }



    private void updateNfcId(String newNfcId) {
        UserManager.getInstance().updateNfcId(newNfcId).addOnSuccessListener(aVoid -> {
//            usernameText.setText(newUsername);
            Toast.makeText(this, "NFC id updated successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update NFC id", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error updating NFC id", e);
        });
    }

    private void showPasswordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPassword = dialogView.findViewById(R.id.currentPassword);
        TextInputEditText newPassword = dialogView.findViewById(R.id.newPassword);
        TextInputEditText confirmPassword = dialogView.findViewById(R.id.confirmPassword);

        builder.setView(dialogView).setTitle("Change Password").setPositiveButton("Save", null) // Set to null initially
                .setNegativeButton("Cancel", null).create();

        AlertDialog dialog = builder.create();

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
            user.updatePassword(newPassword).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                Log.e("ProfileActivity", "Error updating password", e);
            });
        }
    }

}