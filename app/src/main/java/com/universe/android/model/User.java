package com.universe.android.model;

public class User {
    private String uid;
    private String email;
    private String username;
    private String organisationId;
    private int points;
    private long totalStudyTime;

    private String nfcId;

    // Required for Firestore
    public User() {}

    public User(String uid, String email, String username, String organisationId) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.organisationId = organisationId;
        this.points = 0;
        this.totalStudyTime = 0;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOrganisationId() { return organisationId; }
    public void setOrganisationId(String organisationId) { this.organisationId = organisationId; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public long getTotalStudyTime() { return totalStudyTime; }
    public void setTotalStudyTime(long totalStudyTime) { this.totalStudyTime = totalStudyTime; }

    public String getNfcId() { return nfcId; }
    public void setNfcId(String nfcId) { this.nfcId = nfcId; }
}