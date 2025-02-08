package com.universe.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.universe.android.R;
import com.universe.android.model.Participant;
import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {
    private List<Participant> participants = new ArrayList<>();

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        holder.bind(participants.get(position));
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
        notifyDataSetChanged();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView statusText;

        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.participantName);
            statusText = itemView.findViewById(R.id.participantStatus);
        }

        void bind(Participant participant) {
            nameText.setText(participant.getName());
            statusText.setText(participant.isActive() ? "Active" : "Away");
            statusText.setTextColor(itemView.getContext().getColor(
                    participant.isActive() ? android.R.color.holo_green_dark :
                            android.R.color.holo_red_dark));
        }
    }
}