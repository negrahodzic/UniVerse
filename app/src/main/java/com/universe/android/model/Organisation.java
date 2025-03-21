package com.universe.android.model;

import java.util.List;

public class Organisation {
    private String id;
    private String name;

    private List<String> domains;
    private int logoResource;

    public Organisation(String id, String name, int logoResource) {
        this.id = id;
        this.name = name;
        this.logoResource = logoResource;
    }

    // Required for Firestore
    public Organisation() {
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public int getLogoResource() {
        return logoResource;
    }

    public void setLogoResource(int logoResource) {
        this.logoResource = logoResource;
    }

    public boolean isValidDomain(String email) {
        String emailDomain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return domains.contains(emailDomain);
    }
}