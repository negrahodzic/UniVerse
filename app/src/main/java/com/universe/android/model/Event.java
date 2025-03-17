package com.universe.android.model;

import java.io.Serializable;

public class Event implements Serializable {
    private String id;
    private String title;
    private String description;
    private String date;
    private String time;
    private String location;
    private String address;
    private double latitude;
    private double longitude;
    private int pointsPrice;
    private int imageResource;
    private int availableTickets;
    private String status; // SCHEDULED, SOLD_OUT, CANCELLED
    private String organizer;

    // Default constructor for Firebase
    public Event() {
    }

    public Event(String id, String title, String description, String date,
                 String time, String location, String address, int pointsPrice,
                 int imageResource) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.address = address;
        this.pointsPrice = pointsPrice;
        this.imageResource = imageResource;
        this.availableTickets = 50; // Default value
        this.status = "SCHEDULED";
    }

    // Constructor for API response
    public Event(String id, String title, String description, String date,
                 String time, String location, String address, double latitude,
                 double longitude, int pointsPrice, int availableTickets) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pointsPrice = pointsPrice;
        this.availableTickets = availableTickets;
        this.imageResource = android.R.drawable.ic_menu_agenda; // Default resource
        this.status = availableTickets > 0 ? "SCHEDULED" : "SOLD_OUT";
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getPointsPrice() {
        return pointsPrice;
    }

    public void setPointsPrice(int pointsPrice) {
        this.pointsPrice = pointsPrice;
    }

    public int getImageResource() {
        return imageResource;
    }

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
    }

    public int getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(int availableTickets) {
        this.availableTickets = availableTickets;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    // Helper methods
    public boolean isAvailable() {
        return "SCHEDULED".equals(status) && availableTickets > 0;
    }

    public String getFormattedDateTime() {
        return date + (time != null ? " at " + time : "");
    }
}