package com.universe.android.model;

public class Participant {
    private String name;
    private boolean isActive;

    public Participant(String name, boolean isActive) {
        this.name = name;
        this.isActive = isActive;
    }

    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
