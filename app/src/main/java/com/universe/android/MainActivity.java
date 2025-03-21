package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.universe.android.adapter.OrganisationAdapter;
import com.universe.android.manager.OrganisationManager;
import com.universe.android.model.Organisation;
import com.universe.android.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OrganisationAdapter.OnOrganisationClickListener {

    private RecyclerView recyclerView;
    private OrganisationAdapter adapter;
    private ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.organisationList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        adapter = new OrganisationAdapter(this);
        recyclerView.setAdapter(adapter);

        loadingSpinner = findViewById(R.id.loadingSpinner);

        // Load organisations
        loadOrganisations();
    }

    @Override
    public void onOrganisationClick(Organisation organisation) {
        // Save the selected organisation ID for theme application
        getSharedPreferences("universe", MODE_PRIVATE)
                .edit()
                .putString("selected_org_id", organisation.getId())
                .apply();

        // Apply organisation theme
        ThemeManager.applyOrganisationTheme(this, organisation.getId());

        // Navigate to email verification
        Intent intent = new Intent(this, EmailVerificationActivity.class);
        intent.putExtra(EmailVerificationActivity.EXTRA_ORGANISATION_NAME, organisation.getName());
        intent.putExtra(EmailVerificationActivity.EXTRA_ORGANISATION_ID, organisation.getId());
        startActivity(intent);
    }

    private void loadOrganisations() {
        Log.d("MainActivity", "Loading organisations...");
        loadingSpinner.setVisibility(View.VISIBLE);

        OrganisationManager.getInstance()
                .getAllOrganisations()
                .addOnSuccessListener(querySnapshot -> {
                    loadingSpinner.setVisibility(View.GONE);
                    ArrayList<Organisation> organisations = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Organisation org = doc.toObject(Organisation.class);
                        Log.d("MainActivity", "Loaded org: " + org.getId() + ", domains: " + org.getDomains());
                        org.setLogoResource(R.drawable.ic_launcher_foreground);
                        organisations.add(org);
                    });
                    adapter.setOrganisations(organisations);
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Log.e("MainActivity", "Error loading organisations: " + e.getMessage());
                    Toast.makeText(this, "Error loading organizations: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}