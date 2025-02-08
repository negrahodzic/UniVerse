package com.universe.android.model;

public class Organisation {
    private String id;
    private String name;
    private int logoResource;

    public Organisation(String id, String name, int logoResource) {
        this.id = id;
        this.name = name;
        this.logoResource = logoResource;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getLogoResource() { return logoResource; }
}