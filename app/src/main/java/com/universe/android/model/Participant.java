package com.universe.android.model;

public class Participant {
    private String name;
    private boolean isActive;
    private String userId;
    private String nfcId;
    private String actualUsername; // Real username for points

    public Participant(String name, boolean isActive) {
        this.name = name;
        this.isActive = isActive;
    }

    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNfcId() { return nfcId; }
    public void setNfcId(String nfcId) { this.nfcId = nfcId; }

    public String getActualUsername() { return actualUsername; }
    public void setActualUsername(String actualUsername) { this.actualUsername = actualUsername; }
}