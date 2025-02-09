package com.universe.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        // Check current user with small delay for splash effect
        new Handler().postDelayed(this::checkUser, 1000);
    }

    private void checkUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        Intent intent;

        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is signed in and verified
            intent = new Intent(this, DashboardActivity.class);
        } else {
            // No user is signed in
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }
}