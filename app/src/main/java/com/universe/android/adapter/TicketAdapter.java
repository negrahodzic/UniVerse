package com.universe.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.universe.android.R;
import com.universe.android.model.Ticket;
import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {
    private List<Ticket> tickets = new ArrayList<>();

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

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventTitle;
        private final TextView eventDate;
        private final TextView ticketId;
        private final ImageView qrCode;
        private final Chip statusChip;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            ticketId = itemView.findViewById(R.id.ticketId);
            qrCode = itemView.findViewById(R.id.qrCode);
            statusChip = itemView.findViewById(R.id.statusChip);
        }

        void bind(Ticket ticket) {
            eventTitle.setText(ticket.getEvent().getTitle());
            eventDate.setText(ticket.getEvent().getDate());
            ticketId.setText("Ticket ID: " + ticket.getId());
            qrCode.setImageResource(R.drawable.ic_launcher_foreground); // Placeholder

            statusChip.setText(ticket.isUsed() ? "Used" : "Valid");
            statusChip.setChipBackgroundColorResource(
                    ticket.isUsed() ? android.R.color.darker_gray : android.R.color.holo_green_light
            );
        }
    }
}
