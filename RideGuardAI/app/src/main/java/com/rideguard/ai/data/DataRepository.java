package com.rideguard.ai.data;

import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.model.UserSession;

import java.util.List;

/**
 * Everything the app needs from the backend. Two implementations:
 * DemoRepository (sample data, simulates the vest) and FirebaseRepository (real backend).
 */
public interface DataRepository {
    boolean isDemo();

    // Accounts
    void login(String identifier, String password, String role, Callback<UserSession> cb);
    void register(Rider rider, String password, Callback<UserSession> cb);
    void sendPasswordReset(String identifier, Callback<Boolean> cb);
    void logout();

    // Riders
    void getRider(String riderId, Callback<Rider> cb);
    void saveRider(Rider rider, Callback<Boolean> cb);
    ListenerHandle observeRiders(String companyId, Callback<List<Rider>> cb);
    void addRider(Rider rider, Callback<Boolean> cb);
    void removeRider(String riderId, Callback<Boolean> cb);

    // Vests
    ListenerHandle observeDevice(String deviceId, Callback<DeviceStatus> cb);
    ListenerHandle observeDevices(String companyId, Callback<List<DeviceStatus>> cb);
    void setRideActive(String deviceId, boolean active, Callback<Boolean> cb);

    // Incidents
    /** If riderId is not null, only that rider's incidents; otherwise all of the company's. */
    ListenerHandle observeIncidents(String companyId, String riderId, Callback<List<Incident>> cb);
    ListenerHandle observeIncident(String incidentId, Callback<Incident> cb);
    void triggerManualSos(Rider rider, Callback<String> cb);
    void simulateAccident(Rider rider, Callback<String> cb);
    void cancelIncident(String incidentId, Callback<Boolean> cb);
    void reportFalseAlarm(String incidentId, Callback<Boolean> cb);
    void requestLocationUpdate(String incidentId, Callback<Boolean> cb);
    void acknowledgeIncident(String incidentId, String byName, Callback<Boolean> cb);
    void closeIncident(String incidentId, String finalResult, Callback<Boolean> cb);
    void requestVideoDeletion(String incidentId, Callback<Boolean> cb);
    void logIncidentView(String incidentId, String viewer);
    void resolveVideoUrl(String pathOrUrl, Callback<String> cb);
}
