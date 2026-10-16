package com.rideguard.ai.model;

/** Health report of one vest, written by the Raspberry Pi every ~30 seconds. */
public class DeviceStatus {
    public String deviceId;
    public String riderId;
    public String companyId;
    public boolean online;
    public long lastSeen;
    public int battery;
    public boolean mpuOk;
    public boolean frontCamOk;
    public boolean rearCamOk;
    public boolean networkOk;
    public boolean gnssOk;
    public String state = IncidentStatus.READY;
    public boolean rideActive;

    public DeviceStatus() {
    }

    /** The vest counts as connected if it reported in the last 2 minutes. */
    public boolean connected() {
        return online && System.currentTimeMillis() - lastSeen < 120_000L;
    }

    public boolean hasFault() {
        return !mpuOk || !frontCamOk || !rearCamOk || !networkOk || !gnssOk || battery < 15;
    }
}
