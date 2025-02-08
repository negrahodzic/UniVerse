package com.universe.android.model;

public class Event {
    private String id;
    private String title;
    private String date;
    private String location;
    private int pointsPrice;
    private int imageResource;

    public Event(String id, String title, String date, String location, int pointsPrice, int imageResource) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.location = location;
        this.pointsPrice = pointsPrice;
        this.imageResource = imageResource;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getLocation() { return location; }
    public int getPointsPrice() { return pointsPrice; }
    public int getImageResource() { return imageResource; }
}
