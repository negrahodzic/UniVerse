package com.universe.android.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class StudySession {
    private String id;
    private String hostId;
    private List<Map<String, String>> participants; // Each map contains userId, username, and nfcId
    private Date startTime;
    private Date endTime;
    private int durationSeconds;
    private int pointsAwarded;
    private boolean completed;

    // Required for Firestore
    public StudySession() {
    }

    public StudySession(String id, String hostId, List<Map<String, String>> participants,
                        Date startTime, int durationSeconds) {
        this.id = id;
        this.hostId = hostId;
        this.participants = participants;
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
        this.completed = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public List<Map<String, String>> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Map<String, String>> participants) {
        this.participants = participants;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Get the actual session duration in seconds based on start and end time
     * Returns -1 if end time is not set
     */
    public int getActualDurationSeconds() {
        if (startTime == null || endTime == null) {
            return -1;
        }

        return (int) ((endTime.getTime() - startTime.getTime()) / 1000);
    }

    public int getParticipantCount() {
        return participants != null ? participants.size() : 0;
    }
}