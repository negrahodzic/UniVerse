package com.universe.android.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.R;
import com.universe.android.model.Participant;
import com.universe.android.model.User;

import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {
    private List<Participant> participants = new ArrayList<>();
    private FirebaseFirestore db;

    public ParticipantAdapter() {
        this.db = FirebaseFirestore.getInstance();
    }

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

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private final ImageView participantImage;
        private final TextView participantName;
        private final TextView participantCourse;
        private final TextView participantLevel;
        private final ImageView participantBadge;

        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            participantImage = itemView.findViewById(R.id.participantImage);
            participantName = itemView.findViewById(R.id.participantName);
            participantCourse = itemView.findViewById(R.id.participantCourse);
            participantLevel = itemView.findViewById(R.id.participantLevel);
            participantBadge = itemView.findViewById(R.id.participantBadge);
        }

        void bind(Participant participant) {
            // Set the participant name
            participantName.setText(participant.getName());

            // Default values
            participantCourse.setText("MSc Computer Science"); // Default course
            participantLevel.setText("LVL 1"); // Default level

            // If we have a userId, load additional user information from Firestore
            if (participant.getUserId() != null && !participant.getUserId().isEmpty()) {
                loadUserDetails(participant.getUserId());
            }
        }

        private void loadUserDetails(String userId) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // Calculate user level based on points
                                int level = calculateLevel(user.getPoints());
                                participantLevel.setText("LVL " + level);

                                // Set user profile image if available
                                if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
                                    try {
                                        // Convert Base64 to Bitmap
                                        byte[] decodedString = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        participantImage.setImageBitmap(decodedByte);
                                    } catch (Exception e) {
                                        // Fallback to default image
                                        participantImage.setImageResource(R.drawable.ic_launcher_foreground);
                                    }
                                }

                                // Show trophy badge if user has high points
                                if (user.getPoints() > 1000) {
                                    participantBadge.setVisibility(View.VISIBLE);
                                } else {
                                    participantBadge.setVisibility(View.GONE);
                                }

                                // For now, don't have course info in the User model
                            }
                        }
                    });
        }

        private int calculateLevel(int points) {
            // level calculation: 1 level per 200 points, minimum level 1
            return Math.max(1, (points / 200) + 1);
        }
    }

    public List<Participant> getParticipants() {
        return new ArrayList<>(participants); // Returning copy to prevent external modifications
    }
}