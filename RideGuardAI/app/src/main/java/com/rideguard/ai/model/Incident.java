package com.rideguard.ai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One possible or confirmed accident. Mirrors the "incidents" collection in Firestore. */
public class Incident {
    public String id;
    public String incidentNumber;
    public String riderId;
    public String deviceId;
    public String companyId;
    public String riderName;
    public String riderPhone;
    public String contactName;
    public String contactPhone;
    public String healthSnapshot;

    /** auto | manual_sos */
    public String type = "auto";
    /** vest | app */
    public String source = "vest";
    public String status = IncidentStatus.COUNTDOWN;
    public long createdAt;
    public long countdownEndsAt;

    public Double latitude;
    public Double longitude;
    /** pending | live | last_known */
    public String locationStatus = "pending";
    public long locationUpdatedAt;
    public long locationRequestAt;

    public double maxAcceleration;
    public double maxRotation;
    public double tiltDeg;
    public double aiScore;

    /** not_sent | sending | sent | failed */
    public String smsStatus = "not_sent";
    public boolean companyNotified;

    public String frontVideoUrl;
    public String rearVideoUrl;
    /** recording | pending | uploading | uploaded | failed | not_uploaded */
    public String frontVideoStatus = "recording";
    public String rearVideoStatus = "recording";
    public int frontVideoProgress;
    public int rearVideoProgress;

    public boolean acknowledged;
    public String acknowledgedBy;
    public long closedAt;
    public String finalResult;
    public boolean falseAlarmReported;
    public boolean deleteRequested;
    public List<String> viewLog = new ArrayList<>();

    public Incident() {
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public static Incident newFor(Rider r, String type, String status) {
        Incident i = new Incident();
        i.riderId = r.id;
        i.deviceId = r.deviceId;
        i.companyId = r.companyId;
        i.riderName = r.fullName;
        i.riderPhone = r.phone;
        i.contactName = r.contactName;
        i.contactPhone = r.contactPhone;
        i.healthSnapshot = r.healthSnapshot();
        i.type = type;
        i.status = status;
        i.createdAt = System.currentTimeMillis();
        return i;
    }

    public static String numberFromId(String id) {
        if (id == null) return "INC-?";
        String s = id.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
        return "INC-" + (s.length() > 6 ? s.substring(0, 6) : s);
    }
}
