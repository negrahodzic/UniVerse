package com.universe.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.R;
import com.universe.android.model.Organisation;
import java.util.ArrayList;
import java.util.List;

public class OrganisationAdapter extends RecyclerView.Adapter<OrganisationAdapter.OrganisationViewHolder> {
    private List<Organisation> organisations = new ArrayList<>();
    private OnOrganisationClickListener listener;

    public interface OnOrganisationClickListener {
        void onOrganisationClick(Organisation organisation);
    }

    public OrganisationAdapter(OnOrganisationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrganisationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organisation, parent, false);
        return new OrganisationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrganisationViewHolder holder, int position) {
        holder.bind(organisations.get(position));
    }

    @Override
    public int getItemCount() {
        return organisations.size();
    }

    public void setOrganisations(List<Organisation> organisations) {
        this.organisations = organisations;
        notifyDataSetChanged();
    }

    class OrganisationViewHolder extends RecyclerView.ViewHolder {
        private ImageView logoImage;
        private TextView nameText;

        OrganisationViewHolder(@NonNull View itemView) {
            super(itemView);
            logoImage = itemView.findViewById(R.id.logoImage);
            nameText = itemView.findViewById(R.id.nameText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onOrganisationClick(organisations.get(position));
                }
            });
        }

        void bind(Organisation organisation) {
            nameText.setText(organisation.getName());
            logoImage.setImageResource(organisation.getLogoResource());
        }
    }
}