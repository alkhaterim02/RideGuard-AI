package com.rideguard.ai.model;

public class UserSession {
    public static final String ROLE_RIDER = "rider";
    public static final String ROLE_COMPANY = "company";

    public String uid;
    public String role;
    public String riderId;
    public String companyId;
    public String displayName;

    public UserSession() {
    }

    public UserSession(String uid, String role, String riderId, String companyId, String displayName) {
        this.uid = uid;
        this.role = role;
        this.riderId = riderId;
        this.companyId = companyId;
        this.displayName = displayName;
    }

    public boolean isCompany() {
        return ROLE_COMPANY.equals(role);
    }
}
