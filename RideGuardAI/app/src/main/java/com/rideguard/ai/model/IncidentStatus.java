package com.rideguard.ai.model;

/** System states shared by the vest (Raspberry Pi), backend and app. */
public final class IncidentStatus {
    public static final String READY = "ready";
    public static final String POSSIBLE = "possible_accident";
    public static final String COUNTDOWN = "countdown";
    public static final String CANCELLED = "cancelled";
    public static final String CONFIRMED = "emergency_confirmed";
    public static final String ALERT_SENT = "alert_sent";
    public static final String VIDEO_UPLOADING = "video_uploading";
    public static final String ACTIVE = "emergency_active";
    public static final String CLOSED = "emergency_closed";

    private IncidentStatus() {
    }

    /** Possible accident detected, rider can still cancel. */
    public static boolean isPending(String s) {
        return POSSIBLE.equals(s) || COUNTDOWN.equals(s);
    }

    /** Emergency confirmed and not yet closed. */
    public static boolean isOpenEmergency(String s) {
        return CONFIRMED.equals(s) || ALERT_SENT.equals(s)
                || VIDEO_UPLOADING.equals(s) || ACTIVE.equals(s);
    }
}
