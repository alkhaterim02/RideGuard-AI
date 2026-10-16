package com.rideguard.ai.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real backend using Firebase Auth, Firestore, Storage and Cloud Messaging.
 * Collections: users, riders, devices, incidents. See docs/FIRESTORE_CONTRACT.md.
 */
public class FirebaseRepository implements DataRepository {
    private static FirebaseRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public static synchronized FirebaseRepository get() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    private FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private CollectionReference users() {
        return db.collection("users");
    }

    private CollectionReference riders() {
        return db.collection("riders");
    }

    private CollectionReference devices() {
        return db.collection("devices");
    }

    private CollectionReference incidents() {
        return db.collection("incidents");
    }

    @Override
    public boolean isDemo() {
        return false;
    }

    // ------------------------------------------------------------ accounts

    @Override
    public void login(String identifier, String password, String role, Callback<UserSession> cb) {
        String email = identifier.trim();
        if (!email.contains("@")) {
            cb.onResult(null, "Please log in with your email address.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadSession(result.getUser(), role, cb))
                .addOnFailureListener(e -> cb.onResult(null, "Login failed: " + e.getMessage()));
    }

    private void loadSession(FirebaseUser user, String role, Callback<UserSession> cb) {
        if (user == null) {
            cb.onResult(null, "Login failed.");
            return;
        }
        users().document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String r = doc.getString("role");
                    if (r == null || !r.equals(role)) {
                        auth.signOut();
                        cb.onResult(null, "This account is not registered as a " + role + " account.");
                        return;
                    }
                    UserSession s = new UserSession(user.getUid(), r, doc.getString("riderId"),
                            doc.getString("companyId"), doc.getString("displayName"));
                    updateToken();
                    cb.onResult(s, null);
                })
                .addOnFailureListener(e -> cb.onResult(null, "Could not load account: " + e.getMessage()));
    }

    private void updateToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(FirebaseRepository::saveFcmToken);
    }

    /** Saves this phone's push-notification token so the backend can notify it. */
    public static void saveFcmToken(String token) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || token == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("fcmToken", token);
        FirebaseFirestore.getInstance().collection("users").document(u.getUid()).set(m, SetOptions.merge());
    }

    @Override
    public void register(Rider rider, String password, Callback<UserSession> cb) {
        auth.createUserWithEmailAndPassword(rider.email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser u = result.getUser();
                    if (u == null) {
                        cb.onResult(null, "Registration failed.");
                        return;
                    }
                    Rider r = rider.copy();
                    r.id = u.getUid();
                    r.status = "active";
                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("role", UserSession.ROLE_RIDER);
                    userDoc.put("riderId", r.id);
                    userDoc.put("companyId", r.companyId);
                    userDoc.put("displayName", r.fullName);
                    WriteBatch b = db.batch();
                    b.set(riders().document(r.id), r);
                    b.set(users().document(r.id), userDoc);
                    b.commit()
                            .addOnSuccessListener(v -> {
                                updateToken();
                                cb.onResult(new UserSession(r.id, UserSession.ROLE_RIDER, r.id,
                                        r.companyId, r.fullName), null);
                            })
                            .addOnFailureListener(e -> cb.onResult(null, "Could not save profile: " + e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onResult(null, "Registration failed: " + e.getMessage()));
    }

    @Override
    public void sendPasswordReset(String identifier, Callback<Boolean> cb) {
        if (identifier == null || !identifier.contains("@")) {
            cb.onResult(false, "Enter your email address first.");
            return;
        }
        auth.sendPasswordResetEmail(identifier.trim())
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    @Override
    public void logout() {
        auth.signOut();
    }

    // ------------------------------------------------------------ riders

    @Override
    public void getRider(String riderId, Callback<Rider> cb) {
        if (riderId == null) {
            cb.onResult(null, "Rider profile not found.");
            return;
        }
        riders().document(riderId).get()
                .addOnSuccessListener(doc -> {
                    Rider r = doc.exists() ? doc.toObject(Rider.class) : null;
                    if (r != null) r.id = doc.getId();
                    cb.onResult(r, r == null ? "Rider profile not found." : null);
                })
                .addOnFailureListener(e -> cb.onResult(null, e.getMessage()));
    }

    @Override
    public void saveRider(Rider rider, Callback<Boolean> cb) {
        riders().document(rider.id).set(rider, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    @Override
    public ListenerHandle observeRiders(String companyId, Callback<List<Rider>> cb) {
        ListenerRegistration reg = riders().whereEqualTo("companyId", companyId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        cb.onResult(new ArrayList<>(), e == null ? "No data" : e.getMessage());
                        return;
                    }
                    List<Rider> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Rider r = d.toObject(Rider.class);
                        if (r != null && !"removed".equals(r.status)) {
                            r.id = d.getId();
                            list.add(r);
                        }
                    }
                    cb.onResult(list, null);
                });
        return reg::remove;
    }

    /**
     * Firebase cannot create another person's login from a phone, so the company
     * creates an "invited" record; the rider then registers with the company code.
     */
    @Override
    public void addRider(Rider rider, Callback<Boolean> cb) {
        DocumentReference ref = riders().document();
        Rider r = rider.copy();
        r.id = ref.getId();
        r.status = "invited";
        ref.set(r)
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    @Override
    public void removeRider(String riderId, Callback<Boolean> cb) {
        riders().document(riderId).update("status", "removed")
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    // ------------------------------------------------------------ vests

    @Override
    public ListenerHandle observeDevice(String deviceId, Callback<DeviceStatus> cb) {
        if (deviceId == null) {
            cb.onResult(null, "No vest assigned.");
            return () -> {
            };
        }
        ListenerRegistration reg = devices().document(deviceId).addSnapshotListener((doc, e) -> {
            DeviceStatus d = doc != null && doc.exists() ? doc.toObject(DeviceStatus.class) : null;
            if (d != null) d.deviceId = doc.getId();
            cb.onResult(d, d == null ? "Vest not found." : null);
        });
        return reg::remove;
    }

    @Override
    public ListenerHandle observeDevices(String companyId, Callback<List<DeviceStatus>> cb) {
        ListenerRegistration reg = devices().whereEqualTo("companyId", companyId)
                .addSnapshotListener((snap, e) -> {
                    List<DeviceStatus> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            DeviceStatus s = d.toObject(DeviceStatus.class);
                            if (s != null) {
                                s.deviceId = d.getId();
                                list.add(s);
                            }
                        }
                    }
                    cb.onResult(list, e == null ? null : e.getMessage());
                });
        return reg::remove;
    }

    @Override
    public void setRideActive(String deviceId, boolean active, Callback<Boolean> cb) {
        devices().document(deviceId).update("rideActive", active)
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    // ------------------------------------------------------------ incidents

    private static Incident toIncident(DocumentSnapshot d) {
        Incident i = d.toObject(Incident.class);
        if (i == null) return null;
        i.id = d.getId();
        if (i.incidentNumber == null) i.incidentNumber = Incident.numberFromId(i.id);
        return i;
    }

    @Override
    public ListenerHandle observeIncidents(String companyId, String riderId, Callback<List<Incident>> cb) {
        Query q = riderId != null
                ? incidents().whereEqualTo("riderId", riderId)
                : incidents().whereEqualTo("companyId", companyId);
        ListenerRegistration reg = q.addSnapshotListener((snap, e) -> {
            List<Incident> list = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Incident i = toIncident(d);
                    if (i != null) list.add(i);
                }
            }
            Collections.sort(list, (x, y) -> Long.compare(y.createdAt, x.createdAt));
            cb.onResult(list, e == null ? null : e.getMessage());
        });
        return reg::remove;
    }

    @Override
    public ListenerHandle observeIncident(String incidentId, Callback<Incident> cb) {
        ListenerRegistration reg = incidents().document(incidentId).addSnapshotListener((doc, e) -> {
            Incident i = doc != null && doc.exists() ? toIncident(doc) : null;
            cb.onResult(i, i == null ? "Incident not found." : null);
        });
        return reg::remove;
    }

    /** The vest listens for incidents with source "app" and sends the SMS itself. */
    @Override
    public void triggerManualSos(Rider rider, Callback<String> cb) {
        if (rider == null) {
            cb.onResult(null, "Rider profile not loaded yet.");
            return;
        }
        DocumentReference ref = incidents().document();
        Incident i = Incident.newFor(rider, "manual_sos", IncidentStatus.CONFIRMED);
        i.id = ref.getId();
        i.incidentNumber = Incident.numberFromId(i.id);
        i.source = "app";
        ref.set(i)
                .addOnSuccessListener(v -> cb.onResult(i.id, null))
                .addOnFailureListener(e -> cb.onResult(null, e.getMessage()));
    }

    @Override
    public void simulateAccident(Rider rider, Callback<String> cb) {
        cb.onResult(null, "Simulation is only available in demo mode. Use the vest to test real detection.");
    }

    @Override
    public void cancelIncident(String incidentId, Callback<Boolean> cb) {
        DocumentReference ref = incidents().document(incidentId);
        db.runTransaction((Transaction.Function<Boolean>) tr -> {
                    DocumentSnapshot s = tr.get(ref);
                    if (!IncidentStatus.isPending(s.getString("status"))) return false;
                    Map<String, Object> m = new HashMap<>();
                    m.put("status", IncidentStatus.CANCELLED);
                    m.put("cancelledFrom", "app");
                    m.put("finalResult", "False alarm cancelled by rider");
                    tr.update(ref, m);
                    return true;
                })
                .addOnSuccessListener(ok -> cb.onResult(ok, Boolean.TRUE.equals(ok) ? null
                        : "The alert was already sent. Use \"This was a false alarm\" instead."))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    private void update(String incidentId, Map<String, Object> fields, Callback<Boolean> cb) {
        incidents().document(incidentId).update(fields)
                .addOnSuccessListener(v -> {
                    if (cb != null) cb.onResult(true, null);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onResult(false, e.getMessage());
                });
    }

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new HashMap<>();
        for (int k = 0; k + 1 < keyValues.length; k += 2) m.put((String) keyValues[k], keyValues[k + 1]);
        return m;
    }

    @Override
    public void reportFalseAlarm(String incidentId, Callback<Boolean> cb) {
        update(incidentId, map("falseAlarmReported", true,
                "falseAlarmReportedAt", System.currentTimeMillis()), cb);
    }

    @Override
    public void requestLocationUpdate(String incidentId, Callback<Boolean> cb) {
        update(incidentId, map("locationRequestAt", System.currentTimeMillis()), cb);
    }

    @Override
    public void acknowledgeIncident(String incidentId, String byName, Callback<Boolean> cb) {
        update(incidentId, map("acknowledged", true, "acknowledgedBy", byName), cb);
    }

    @Override
    public void closeIncident(String incidentId, String finalResult, Callback<Boolean> cb) {
        update(incidentId, map("status", IncidentStatus.CLOSED,
                "closedAt", System.currentTimeMillis(), "finalResult", finalResult), cb);
    }

    @Override
    public void requestVideoDeletion(String incidentId, Callback<Boolean> cb) {
        update(incidentId, map("deleteRequested", true), cb);
    }

    @Override
    public void logIncidentView(String incidentId, String viewer) {
        String entry = viewer + " – " + UiUtils.formatTime(System.currentTimeMillis());
        update(incidentId, map("viewLog", FieldValue.arrayUnion(entry)), null);
    }

    /** Accepts an https URL, a gs:// URL, or a Storage path such as "videos/inc123/front.mp4". */
    @Override
    public void resolveVideoUrl(String pathOrUrl, Callback<String> cb) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            cb.onResult(null, "Video not uploaded yet.");
            return;
        }
        if (pathOrUrl.startsWith("http")) {
            cb.onResult(pathOrUrl, null);
            return;
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref = pathOrUrl.startsWith("gs://")
                ? storage.getReferenceFromUrl(pathOrUrl)
                : storage.getReference().child(pathOrUrl);
        ref.getDownloadUrl()
                .addOnSuccessListener(uri -> cb.onResult(uri.toString(), null))
                .addOnFailureListener(e -> cb.onResult(null, e.getMessage()));
    }
}
