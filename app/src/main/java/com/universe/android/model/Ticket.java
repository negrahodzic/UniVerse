package com.universe.android.model;

public class Ticket {
    private String id;
    private Event event;
    private String purchaseDate;
    private String qrCode;
    private boolean isUsed;

    public Ticket(String id, Event event, String purchaseDate, String qrCode, boolean isUsed) {
        this.id = id;
        this.event = event;
        this.purchaseDate = purchaseDate;
        this.qrCode = qrCode;
        this.isUsed = isUsed;
    }

    // Getters
    public String getId() { return id; }
    public Event getEvent() { return event; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getQrCode() { return qrCode; }
    public boolean isUsed() { return isUsed; }
}