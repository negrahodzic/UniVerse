package com.universe.android.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.universe.android.R;
import com.universe.android.model.StudySession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {
    private List<StudySession> sessions = new ArrayList<>();
    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(StudySession session);
        void onDetailsClick(StudySession session);
    }

    public SessionHistoryAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        StudySession session = sessions.get(position);
        Log.d("SessionHistoryAdapter", "Binding session " + session.getId() + " at position " + position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public void setSessions(List<StudySession> sessions) {
        this.sessions = sessions;
        notifyDataSetChanged();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView sessionDate;
        private final TextView sessionPoints;
        private final TextView sessionDuration;
        private final TextView sessionParticipants;
        private final TextView sessionStatus;
        private final MaterialButton sessionDetailsButton;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionDate = itemView.findViewById(R.id.sessionDate);
            sessionPoints = itemView.findViewById(R.id.sessionPoints);
            sessionDuration = itemView.findViewById(R.id.sessionDuration);
            sessionParticipants = itemView.findViewById(R.id.sessionParticipants);
            sessionStatus = itemView.findViewById(R.id.sessionStatus);
            sessionDetailsButton = itemView.findViewById(R.id.sessionDetailsButton);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessions.get(position));
                }
            });

            sessionDetailsButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDetailsClick(sessions.get(position));
                }
            });
        }

        void bind(StudySession session) {
            // Format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US);
            String formattedDate = session.getStartTime() != null ?
                    dateFormat.format(session.getStartTime()) : "Unknown date";

            // Format the duration
            String durationText;
            if (session.getDurationSeconds() < 60) {
                durationText = session.getDurationSeconds() + " seconds"; // For testing
            } else {
                int minutes = session.getDurationSeconds() / 60;
                if (minutes < 60) {
                    durationText = minutes + " minutes";
                } else {
                    int hours = minutes / 60;
                    int remainingMinutes = minutes % 60;
                    durationText = hours + "h " + (remainingMinutes > 0 ? remainingMinutes + "m" : "");
                }
            }

            // Set the views
            sessionDate.setText(formattedDate);
            sessionPoints.setText("+" + session.getPointsAwarded() + " points");
            sessionDuration.setText(durationText);

            // Set participants count
            int participantCount = session.getParticipantCount();
            sessionParticipants.setText(participantCount + " participant" +
                    (participantCount != 1 ? "s" : ""));

            // Set status
            if (session.isCompleted()) {
                sessionStatus.setText("Completed");
                sessionStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                sessionStatus.setText("Incomplete");
                sessionStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
            }
        }
    }
}