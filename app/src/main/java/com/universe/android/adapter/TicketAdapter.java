package com.universe.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.universe.android.R;
import com.universe.android.model.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {
    private List<Ticket> tickets = new ArrayList<>();
    private final OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    public TicketAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public TicketAdapter() {
        this.listener = null;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(tickets.get(position));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        notifyDataSetChanged();
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView eventTitle;
        private final TextView eventDate;
        private final TextView ticketId;
        private final TextView ticketQuantity;
        private final ImageView qrCode;
        private final Chip statusChip;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            ticketId = itemView.findViewById(R.id.ticketId);
            ticketQuantity = itemView.findViewById(R.id.ticketQuantity);
            qrCode = itemView.findViewById(R.id.qrCode);
            statusChip = itemView.findViewById(R.id.statusChip);

            // Set click listener if provided
            if (listener != null) {
                cardView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTicketClick(tickets.get(position));
                    }
                });
            }
        }

        void bind(Ticket ticket) {
            // Set text fields
            eventTitle.setText(ticket.getEvent().getTitle());
            eventDate.setText(ticket.getEvent().getFormattedDateTime());
            ticketId.setText("Ticket ID: " + ticket.getId());

            // Set quantity if view exists and ticket has more than one
            if (ticketQuantity != null && ticket.getNumberOfTickets() > 1) {
                ticketQuantity.setVisibility(View.VISIBLE);
                ticketQuantity.setText("Quantity: " + ticket.getNumberOfTickets());
            } else if (ticketQuantity != null) {
                ticketQuantity.setVisibility(View.GONE);
            }

            // Generate and set QR code
            if (qrCode != null) {
                qrCode.setImageBitmap(ticket.getQrCodeBitmap(200, 200));
            }

            // Set status chip
            statusChip.setText(ticket.getStatusText());
            statusChip.setChipBackgroundColorResource(ticket.getStatusColor());

            // Set card stroke color based on ticket status
            int strokeColor = ContextCompat.getColor(itemView.getContext(),
                    ticket.isUsed() ? android.R.color.darker_gray : android.R.color.holo_green_light);
            cardView.setStrokeColor(strokeColor);
        }
    }
}