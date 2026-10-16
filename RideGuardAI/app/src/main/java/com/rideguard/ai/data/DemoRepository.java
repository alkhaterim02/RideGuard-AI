package com.rideguard.ai.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.util.NotificationHelper;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory backend with sample data. It also pretends to be the vest:
 * after a simulated accident it runs the countdown, "sends" the SMS,
 * and "uploads" the two videos, so every screen can be tested without hardware.
 * Data resets when the app is closed.
 */
public class DemoRepository implements DataRepository {
    // Demo map location. Change to your city if you like.
    public static final double DEMO_LAT = 24.7136;
    public static final double DEMO_LNG = 46.6753;
    public static final long COUNTDOWN_MS = 20_000L;

    private static final String DEMO_PASSWORD = "demo123";
    private static final String COMPANY_ID = "c1";
    private static final String COMPANY_NAME = "Swift Deliveries (Demo)";
    private static final String COMPANY_EMAIL = "company@demo.com";
    private static final String VIDEO_FRONT =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";
    private static final String VIDEO_REAR =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static DemoRepository instance;

    private final Context app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Rider> riders = new LinkedHashMap<>();
    private final Map<String, String> passwords = new HashMap<>();
    private final Map<String, DeviceStatus> devices = new LinkedHashMap<>();
    private final Map<String, Incident> incidents = new LinkedHashMap<>();
    private final List<Observer> observers = new ArrayList<>();
    private int counter = 100;

    private static class Observer {
        final Runnable action;
        boolean active = true;

        Observer(Runnable action) {
            this.action = action;
        }
    }

    public static synchronized DemoRepository get(Context c) {
        if (instance == null) instance = new DemoRepository(c.getApplicationContext());
        return instance;
    }

    private DemoRepository(Context app) {
        this.app = app;
        seed();
        heartbeat();
    }

    @Override
    public boolean isDemo() {
        return true;
    }

    // ------------------------------------------------------------ helpers

    private <T> void reply(Callback<T> cb, T value, String error) {
        if (cb != null) handler.postDelayed(() -> cb.onResult(value, error), 300);
    }

    private ListenerHandle observe(Runnable action) {
        Observer o = new Observer(action);
        observers.add(o);
        handler.post(() -> {
            if (o.active) o.action.run();
        });
        return () -> {
            o.active = false;
            observers.remove(o);
        };
    }

    private void changed() {
        for (Observer o : new ArrayList<>(observers)) {
            if (o.active) o.action.run();
        }
    }

    private String newId(String prefix) {
        counter++;
        return prefix + "-" + counter;
    }

    private void setDeviceState(String deviceId, String state) {
        DeviceStatus d = deviceId == null ? null : devices.get(deviceId);
        if (d != null) d.state = state;
    }

    private void heartbeat() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (DeviceStatus d : devices.values()) {
                    if (d.online) d.lastSeen = now;
                }
                changed();
                handler.postDelayed(this, 30_000L);
            }
        }, 30_000L);
    }

    private DeviceStatus device(String id, Rider r, int battery) {
        DeviceStatus d = new DeviceStatus();
        d.deviceId = id;
        d.riderId = r.id;
        d.companyId = r.companyId;
        d.online = true;
        d.lastSeen = System.currentTimeMillis();
        d.battery = battery;
        d.mpuOk = true;
        d.frontCamOk = true;
        d.rearCamOk = true;
        d.networkOk = true;
        d.gnssOk = true;
        d.state = IncidentStatus.READY;
        return d;
    }

    private void seed() {
        long now = System.currentTimeMillis();

        Rider a = new Rider();
        a.id = "r1";
        a.fullName = "Ali Hassan";
        a.phone = "0500000001";
        a.email = "rider@demo.com";
        a.companyId = COMPANY_ID;
        a.companyName = COMPANY_NAME;
        a.riderCode = "R-1001";
        a.age = "29";
        a.bloodType = "O+";
        a.allergies = "Penicillin";
        a.conditions = "None";
        a.medication = "None";
        a.contactName = "Mona Hassan";
        a.contactRelationship = "Sister";
        a.contactPhone = "0500000099";
        a.consent = true;
        a.deviceId = "VEST-001";

        Rider b = new Rider();
        b.id = "r2";
        b.fullName = "Omar Saleh";
        b.phone = "0500000002";
        b.email = "rider2@demo.com";
        b.companyId = COMPANY_ID;
        b.companyName = COMPANY_NAME;
        b.riderCode = "R-1002";
        b.age = "34";
        b.bloodType = "A-";
        b.allergies = "None";
        b.conditions = "Asthma";
        b.medication = "Salbutamol inhaler";
        b.contactName = "Layla Saleh";
        b.contactRelationship = "Wife";
        b.contactPhone = "0500000088";
        b.consent = true;
        b.deviceId = "VEST-002";

        riders.put(a.id, a);
        riders.put(b.id, b);
        passwords.put(a.id, DEMO_PASSWORD);
        passwords.put(b.id, DEMO_PASSWORD);

        devices.put("VEST-001", device("VEST-001", a, 82));
        DeviceStatus d2 = device("VEST-002", b, 45);
        d2.gnssOk = false; // shows a "device with errors" on the company dashboard
        devices.put("VEST-002", d2);

        Incident old1 = Incident.newFor(a, "auto", IncidentStatus.CANCELLED);
        old1.id = "demo-inc-1";
        old1.incidentNumber = "INC-1001";
        old1.createdAt = now - 3 * DAY;
        old1.latitude = DEMO_LAT + 0.004;
        old1.longitude = DEMO_LNG - 0.003;
        old1.locationStatus = "live";
        old1.maxAcceleration = 3.9;
        old1.maxRotation = 180;
        old1.tiltDeg = 35;
        old1.aiScore = 0.18;
        old1.frontVideoStatus = "not_uploaded";
        old1.rearVideoStatus = "not_uploaded";
        old1.finalResult = "False alarm cancelled by rider (road bump)";

        Incident old2 = Incident.newFor(b, "auto", IncidentStatus.CLOSED);
        old2.id = "demo-inc-2";
        old2.incidentNumber = "INC-1002";
        old2.createdAt = now - 9 * DAY;
        old2.latitude = DEMO_LAT - 0.006;
        old2.longitude = DEMO_LNG + 0.005;
        old2.locationStatus = "live";
        old2.maxAcceleration = 7.4;
        old2.maxRotation = 520;
        old2.tiltDeg = 88;
        old2.aiScore = 0.91;
        old2.smsStatus = "sent";
        old2.companyNotified = true;
        old2.frontVideoStatus = "uploaded";
        old2.rearVideoStatus = "uploaded";
        old2.frontVideoProgress = 100;
        old2.rearVideoProgress = 100;
        old2.frontVideoUrl = VIDEO_FRONT;
        old2.rearVideoUrl = VIDEO_REAR;
        old2.acknowledged = true;
        old2.acknowledgedBy = "Demo Company Admin";
        old2.closedAt = old2.createdAt + 40 * 60 * 1000;
        old2.finalResult = "Rider taken to hospital with minor injuries";

        incidents.put(old1.id, old1);
        incidents.put(old2.id, old2);
    }

    // ------------------------------------------------------------ accounts

    @Override
    public void login(String identifier, String password, String role, Callback<UserSession> cb) {
        String id = identifier.trim();
        if (UserSession.ROLE_COMPANY.equals(role)) {
            if (!COMPANY_EMAIL.equalsIgnoreCase(id)) {
                reply(cb, null, "No company account found with that email.");
                return;
            }
            if (!DEMO_PASSWORD.equals(password)) {
                reply(cb, null, "Wrong password. Please try again.");
                return;
            }
            reply(cb, new UserSession("company-" + COMPANY_ID, UserSession.ROLE_COMPANY, null,
                    COMPANY_ID, "Demo Company Admin"), null);
            return;
        }
        for (Rider r : riders.values()) {
            if ("removed".equals(r.status)) continue;
            if (id.equalsIgnoreCase(r.email) || id.equals(r.phone) || id.equalsIgnoreCase(r.riderCode)) {
                if (!password.equals(passwords.get(r.id))) {
                    reply(cb, null, "Wrong password. Please try again.");
                    return;
                }
                reply(cb, new UserSession(r.id, UserSession.ROLE_RIDER, r.id, r.companyId, r.fullName), null);
                return;
            }
        }
        reply(cb, null, "No rider account found. Check your email, phone number or rider ID.");
    }

    @Override
    public void register(Rider rider, String password, Callback<UserSession> cb) {
        for (Rider r : riders.values()) {
            if (rider.email != null && rider.email.equalsIgnoreCase(r.email) && !"removed".equals(r.status)) {
                reply(cb, null, "An account with this email already exists.");
                return;
            }
        }
        Rider r = rider.copy();
        r.id = newId("r");
        r.companyId = COMPANY_ID; // in demo mode every new rider joins the demo company
        r.companyName = COMPANY_NAME;
        r.status = "active";
        r.deviceId = "VEST-" + counter; // give the new rider a demo vest so they can test
        riders.put(r.id, r);
        passwords.put(r.id, password);
        devices.put(r.deviceId, device(r.deviceId, r, 100));
        changed();
        reply(cb, new UserSession(r.id, UserSession.ROLE_RIDER, r.id, r.companyId, r.fullName), null);
    }

    @Override
    public void sendPasswordReset(String identifier, Callback<Boolean> cb) {
        reply(cb, true, null);
    }

    @Override
    public void logout() {
    }

    // ------------------------------------------------------------ riders

    @Override
    public void getRider(String riderId, Callback<Rider> cb) {
        Rider r = riderId == null ? null : riders.get(riderId);
        reply(cb, r == null ? null : r.copy(), r == null ? "Rider profile not found." : null);
    }

    @Override
    public void saveRider(Rider rider, Callback<Boolean> cb) {
        riders.put(rider.id, rider.copy());
        if (rider.deviceId != null) {
            DeviceStatus d = devices.get(rider.deviceId);
            if (d != null) d.riderId = rider.id;
        }
        changed();
        reply(cb, true, null);
    }

    @Override
    public ListenerHandle observeRiders(String companyId, Callback<List<Rider>> cb) {
        return observe(() -> {
            List<Rider> list = new ArrayList<>();
            for (Rider r : riders.values()) {
                if (companyId != null && companyId.equals(r.companyId) && !"removed".equals(r.status)) {
                    list.add(r.copy());
                }
            }
            cb.onResult(list, null);
        });
    }

    @Override
    public void addRider(Rider rider, Callback<Boolean> cb) {
        Rider r = rider.copy();
        r.id = newId("r");
        r.status = "active";
        riders.put(r.id, r);
        passwords.put(r.id, DEMO_PASSWORD);
        changed();
        reply(cb, true, null);
    }

    @Override
    public void removeRider(String riderId, Callback<Boolean> cb) {
        Rider r = riders.get(riderId);
        if (r != null) r.status = "removed";
        changed();
        reply(cb, true, null);
    }

    // ------------------------------------------------------------ vests

    @Override
    public ListenerHandle observeDevice(String deviceId, Callback<DeviceStatus> cb) {
        return observe(() -> {
            DeviceStatus d = deviceId == null ? null : devices.get(deviceId);
            cb.onResult(d, d == null ? "No vest assigned." : null);
        });
    }

    @Override
    public ListenerHandle observeDevices(String companyId, Callback<List<DeviceStatus>> cb) {
        return observe(() -> {
            List<DeviceStatus> list = new ArrayList<>();
            for (DeviceStatus d : devices.values()) {
                if (companyId != null && companyId.equals(d.companyId)) list.add(d);
            }
            cb.onResult(list, null);
        });
    }

    @Override
    public void setRideActive(String deviceId, boolean active, Callback<Boolean> cb) {
        DeviceStatus d = devices.get(deviceId);
        if (d != null) d.rideActive = active;
        changed();
        reply(cb, d != null, d == null ? "Vest not found." : null);
    }

    // ------------------------------------------------------------ incidents

    @Override
    public ListenerHandle observeIncidents(String companyId, String riderId, Callback<List<Incident>> cb) {
        return observe(() -> {
            List<Incident> list = new ArrayList<>();
            for (Incident i : incidents.values()) {
                boolean match = riderId != null
                        ? riderId.equals(i.riderId)
                        : companyId != null && companyId.equals(i.companyId);
                if (match) list.add(i);
            }
            Collections.sort(list, (x, y) -> Long.compare(y.createdAt, x.createdAt));
            cb.onResult(list, null);
        });
    }

    @Override
    public ListenerHandle observeIncident(String incidentId, Callback<Incident> cb) {
        return observe(() -> {
            Incident i = incidentId == null ? null : incidents.get(incidentId);
            cb.onResult(i, i == null ? "Incident not found." : null);
        });
    }

    private Incident openIncidentFor(String riderId) {
        for (Incident x : incidents.values()) {
            if (riderId.equals(x.riderId)
                    && (IncidentStatus.isPending(x.status) || IncidentStatus.isOpenEmergency(x.status))) {
                return x;
            }
        }
        return null;
    }

    @Override
    public void simulateAccident(Rider rider, Callback<String> cb) {
        DeviceStatus d = rider == null || rider.deviceId == null ? null : devices.get(rider.deviceId);
        if (d == null || !d.connected()) {
            reply(cb, null, "No vest connected. A vest must be assigned first.");
            return;
        }
        if (openIncidentFor(rider.id) != null) {
            reply(cb, null, "There is already an active alert.");
            return;
        }
        Incident i = Incident.newFor(rider, "auto", IncidentStatus.COUNTDOWN);
        i.id = newId("inc");
        i.incidentNumber = "INC-" + (1000 + counter);
        i.countdownEndsAt = i.createdAt + COUNTDOWN_MS;
        i.maxAcceleration = 6.8;
        i.maxRotation = 410;
        i.tiltDeg = 78;
        i.aiScore = 0.82;
        incidents.put(i.id, i);
        d.state = IncidentStatus.COUNTDOWN;
        changed();

        String id = i.id;
        handler.postDelayed(() -> {
            Incident x = incidents.get(id);
            if (x != null && IncidentStatus.isPending(x.status)) confirm(x);
        }, COUNTDOWN_MS);
        reply(cb, id, null);
    }

    @Override
    public void triggerManualSos(Rider rider, Callback<String> cb) {
        if (rider == null) {
            reply(cb, null, "Rider profile not loaded yet.");
            return;
        }
        Incident existing = openIncidentFor(rider.id);
        if (existing != null && IncidentStatus.isOpenEmergency(existing.status)) {
            reply(cb, existing.id, null);
            return;
        }
        Incident i = Incident.newFor(rider, "manual_sos", IncidentStatus.CONFIRMED);
        i.id = newId("inc");
        i.incidentNumber = "INC-" + (1000 + counter);
        i.source = "app";
        incidents.put(i.id, i);
        confirm(i);
        reply(cb, i.id, null);
    }

    /** Simulates what the vest does after confirmation: location, SMS, then videos. */
    private void confirm(Incident i) {
        i.status = IncidentStatus.CONFIRMED;
        i.latitude = DEMO_LAT + (Math.random() - 0.5) * 0.01;
        i.longitude = DEMO_LNG + (Math.random() - 0.5) * 0.01;
        i.locationStatus = "live";
        i.locationUpdatedAt = System.currentTimeMillis();
        i.frontVideoStatus = "recording";
        i.rearVideoStatus = "recording";
        setDeviceState(i.deviceId, IncidentStatus.CONFIRMED);
        changed();

        String id = i.id;
        handler.postDelayed(() -> {
            Incident x = incidents.get(id);
            if (x == null || !IncidentStatus.isOpenEmergency(x.status)) return;
            x.smsStatus = "sent";
            x.companyNotified = true;
            x.status = IncidentStatus.ALERT_SENT;
            setDeviceState(x.deviceId, IncidentStatus.ALERT_SENT);
            changed();
            NotificationHelper.show(app, "Emergency: " + x.riderName,
                    "Possible motorcycle accident. Tap to view the location and details.", x.id);
        }, 1500);

        handler.postDelayed(() -> {
            Incident x = incidents.get(id);
            if (x == null || !IncidentStatus.isOpenEmergency(x.status)) return;
            x.status = IncidentStatus.VIDEO_UPLOADING;
            x.frontVideoStatus = "uploading";
            x.rearVideoStatus = "pending";
            setDeviceState(x.deviceId, IncidentStatus.VIDEO_UPLOADING);
            changed();
            uploadStep(id, true);
        }, 5000);
    }

    private void uploadStep(String id, boolean front) {
        handler.postDelayed(() -> {
            Incident x = incidents.get(id);
            if (x == null) return;
            if (front) {
                x.frontVideoProgress = Math.min(100, x.frontVideoProgress + 20);
                if (x.frontVideoProgress >= 100) {
                    x.frontVideoStatus = "uploaded";
                    x.frontVideoUrl = VIDEO_FRONT;
                    x.rearVideoStatus = "uploading";
                    changed();
                    uploadStep(id, false);
                    return;
                }
            } else {
                x.rearVideoProgress = Math.min(100, x.rearVideoProgress + 20);
                if (x.rearVideoProgress >= 100) {
                    x.rearVideoStatus = "uploaded";
                    x.rearVideoUrl = VIDEO_REAR;
                    if (IncidentStatus.isOpenEmergency(x.status)) {
                        x.status = IncidentStatus.ACTIVE;
                        setDeviceState(x.deviceId, IncidentStatus.ACTIVE);
                    }
                    changed();
                    return;
                }
            }
            changed();
            uploadStep(id, front);
        }, 700);
    }

    @Override
    public void cancelIncident(String incidentId, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i == null) {
            reply(cb, false, "Incident not found.");
            return;
        }
        if (!IncidentStatus.isPending(i.status)) {
            reply(cb, false, "The alert was already sent. Use \"This was a false alarm\" instead.");
            return;
        }
        i.status = IncidentStatus.CANCELLED;
        i.finalResult = "False alarm cancelled by rider";
        i.frontVideoStatus = "not_uploaded";
        i.rearVideoStatus = "not_uploaded";
        setDeviceState(i.deviceId, IncidentStatus.READY);
        changed();
        reply(cb, true, null);
    }

    @Override
    public void reportFalseAlarm(String incidentId, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i != null) {
            i.falseAlarmReported = true;
            i.finalResult = "Rider reported a false alarm. Follow-up message sent to the emergency contact.";
        }
        changed();
        reply(cb, i != null, i == null ? "Incident not found." : null);
    }

    @Override
    public void requestLocationUpdate(String incidentId, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i != null && i.hasLocation()) {
            i.latitude = i.latitude + (Math.random() - 0.5) * 0.0005;
            i.longitude = i.longitude + (Math.random() - 0.5) * 0.0005;
            i.locationStatus = "live";
            i.locationUpdatedAt = System.currentTimeMillis();
        }
        changed();
        reply(cb, true, null);
    }

    @Override
    public void acknowledgeIncident(String incidentId, String byName, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i != null) {
            i.acknowledged = true;
            i.acknowledgedBy = byName;
        }
        changed();
        reply(cb, true, null);
    }

    @Override
    public void closeIncident(String incidentId, String finalResult, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i != null) {
            i.status = IncidentStatus.CLOSED;
            i.closedAt = System.currentTimeMillis();
            i.finalResult = finalResult;
            setDeviceState(i.deviceId, IncidentStatus.READY);
        }
        changed();
        reply(cb, true, null);
    }

    @Override
    public void requestVideoDeletion(String incidentId, Callback<Boolean> cb) {
        Incident i = incidents.get(incidentId);
        if (i != null) i.deleteRequested = true;
        changed();
        reply(cb, true, null);
    }

    @Override
    public void logIncidentView(String incidentId, String viewer) {
        Incident i = incidents.get(incidentId);
        if (i == null) return;
        if (i.viewLog == null) i.viewLog = new ArrayList<>();
        i.viewLog.add(viewer + " – " + UiUtils.formatTime(System.currentTimeMillis()));
        changed();
    }

    @Override
    public void resolveVideoUrl(String pathOrUrl, Callback<String> cb) {
        reply(cb, pathOrUrl, pathOrUrl == null ? "Video not available yet." : null);
    }
}
