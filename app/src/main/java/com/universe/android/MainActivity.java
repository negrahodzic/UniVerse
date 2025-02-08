package com.universe.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.adapter.OrganisationAdapter;
import com.universe.android.model.Organisation;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OrganisationAdapter.OnOrganisationClickListener {

    private RecyclerView recyclerView;
    private OrganisationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.organisationList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        adapter = new OrganisationAdapter(this);
        recyclerView.setAdapter(adapter);

        // Load sample data
        loadSampleOrganisations();
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
        Intent intent = new Intent(this, EmailVerificationActivity.class);
        intent.putExtra(EmailVerificationActivity.EXTRA_ORGANISATION_NAME, organisation.getName());
        startActivity(intent);
    }
}