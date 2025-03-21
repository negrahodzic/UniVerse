package com.universe.android.model;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.Serializable;

public class Ticket implements Serializable {
    private String id;
    private Event event;
    private String purchaseDate;
    private String verificationCode;
    private boolean isUsed;
    private int numberOfTickets;
    private int pointsPrice;
    private transient Bitmap qrCodeBitmap; // Not serialized

    public Ticket(String id, Event event, String purchaseDate, String verificationCode, boolean isUsed) {
        this.id = id;
        this.event = event;
        this.purchaseDate = purchaseDate;
        this.verificationCode = verificationCode;
        this.isUsed = isUsed;
        this.numberOfTickets = 1; // Default
    }

    // Getters
    public String getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public int getNumberOfTickets() {
        return numberOfTickets;
    }

    public int getPointsPrice() {
        return pointsPrice;
    }

    // Setters
    public void setUsed(boolean used) {
        isUsed = used;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setNumberOfTickets(int numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }

    public void setPointsPrice(int pointsPrice) {
        this.pointsPrice = pointsPrice;
    }

    public int getTotalPointsSpent() {
        return numberOfTickets * pointsPrice;
    }

    public Bitmap getQrCodeBitmap(int width, int height) {
        // Return cached bitmap if available
        if (qrCodeBitmap != null) {
            return qrCodeBitmap;
        }

        try {
            // Create JSON-like string with ticket info
            String qrContent = String.format(
                    "{\"id\":\"%s\",\"code\":\"%s\",\"event\":\"%s\",\"tickets\":%d}",
                    id, verificationCode, event.getId(), numberOfTickets
            );

            // Generate QR code bitmap
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    qrContent, BarcodeFormat.QR_CODE, width, height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Cache the bitmap
            qrCodeBitmap = bitmap;
            return bitmap;

        } catch (WriterException e) {
            Log.e("Ticket", "Error generating QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the status of the ticket as a string
     *
     * @return Status text
     */
    public String getStatusText() {
        return isUsed ? "Used" : "Valid";
    }

    /**
     * Get the status color for UI display
     *
     * @return Color resource ID
     */
    public int getStatusColor() {
        return isUsed ? android.R.color.darker_gray : android.R.color.holo_green_light;
    }
}