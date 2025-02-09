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
import com.universe.android.adapter.OrganisationAdapter;
import com.universe.android.manager.OrganisationManager;
import com.universe.android.model.Organisation;
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

        recyclerView = findViewById(R.id.organisationList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        adapter = new OrganisationAdapter(this);
        recyclerView.setAdapter(adapter);

        loadingSpinner = findViewById(R.id.loadingSpinner);

        // Load sample data
        // loadSampleOrganisations();
        loadOrganisations();
    }

    private void loadSampleOrganisations() {
        List<Organisation> organisations = new ArrayList<>();
        organisations.add(new Organisation("1", "Nottingham Trent University", R.drawable.ic_launcher_foreground));
        organisations.add(new Organisation("3", "Oxford University", R.drawable.ic_launcher_foreground));
        organisations.add(new Organisation("2", "Cambridge University", R.drawable.ic_launcher_foreground));
        organisations.add(new Organisation("4", "UCL", R.drawable.ic_launcher_foreground));
        adapter.setOrganisations(organisations);
    }


    @Override
    public void onOrganisationClick(Organisation organisation) {
        getSharedPreferences("universe", MODE_PRIVATE)
                .edit()
                .putString("selected_org_id", organisation.getId())
                .apply();

        Intent intent = new Intent(this, EmailVerificationActivity.class);
        intent.putExtra(EmailVerificationActivity.EXTRA_ORGANISATION_NAME, organisation.getName());
        intent.putExtra(EmailVerificationActivity.EXTRA_ORGANISATION_ID, organisation.getId());
        startActivity(intent);
    }


    private void loadOrganisations() {
        Log.d("MainActivity", "Loading organisations...");

        OrganisationManager.getInstance()
                .getAllOrganisations()
                .addOnSuccessListener(querySnapshot -> {
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
                    Log.e("MainActivity", "Error loading organisations: " + e.getMessage());
                    Toast.makeText(this, "Error loading organizations: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}