package com.rideguard.ai.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.rideguard.ai.R;
import com.rideguard.ai.data.ListenerHandle;
import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.Extras;
import com.rideguard.ai.util.NotificationHelper;
import com.rideguard.ai.util.UiUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 13.4 Rider home: vest status, start ride, manual SOS. Opens the countdown automatically. */
public class RiderHomeActivity extends BaseActivity {
    private static final long SOS_HOLD_MS = 2000;
    /** Countdowns already shown, so the same alert does not open twice. */
    private static final Set<String> handledCountdowns = new HashSet<>();

    private Rider rider;
    private DeviceStatus device;
    private String observedDeviceId;
    private ListenerHandle deviceHandle;
    private Incident activeEmergency;

    private TextView tvGreeting, tvState, tvActive;
    private LinearLayout statusRows;
    private MaterialButton btnStartRide, btnSos;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sosRunnable = this::sendSos;

    @Override
    protected boolean showLogoutMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_rider_home);
        setTitle("RideGuard AI");

        tvGreeting = findViewById(R.id.tvGreeting);
        tvState = findViewById(R.id.tvState);
        tvActive = findViewById(R.id.tvActiveEmergency);
        statusRows = findViewById(R.id.statusRows);
        btnStartRide = findViewById(R.id.btnStartRide);
        btnSos = findViewById(R.id.btnSos);

        View btnSimulate = findViewById(R.id.btnSimulate);
        btnSimulate.setVisibility(repo.isDemo() ? View.VISIBLE : View.GONE);
        btnSimulate.setOnClickListener(v -> repo.simulateAccident(rider, (id, err) -> toast(err)));

        btnStartRide.setOnClickListener(v -> toggleRide());
        setupSosButton();
        findViewById(R.id.btnHealth).setOnClickListener(v ->
                startActivity(new Intent(this, HealthProfileActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        tvActive.setOnClickListener(v -> {
            if (activeEmergency != null) startActivity(EmergencyActivity.intent(this, activeEmergency.id));
        });

        renderDevice();
        track(repo.observeIncidents(session.companyId, session.riderId, (list, err) -> handleIncidents(list)));
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRider(); // refresh after editing the health profile
    }

    private void loadRider() {
        repo.getRider(session.riderId, (r, err) -> {
            if (isFinishing() || isDestroyed()) return;
            if (r == null) {
                toast(err);
                if (repo.isDemo()) logout(); // demo data resets when the app restarts
                return;
            }
            rider = r;
            String first = r.fullName == null ? "" : r.fullName.split(" ")[0];
            tvGreeting.setText("Hello, " + first);
            observeVest(r.deviceId);
        });
    }

    private void observeVest(String deviceId) {
        boolean same = deviceId == null ? observedDeviceId == null : deviceId.equals(observedDeviceId);
        if (deviceHandle != null && same) return;
        if (deviceHandle != null) deviceHandle.remove();
        observedDeviceId = deviceId;
        deviceHandle = repo.observeDevice(deviceId, (d, e) -> {
            device = d;
            renderDevice();
        });
    }

    private void renderDevice() {
        statusRows.removeAllViews();
        if (device == null) {
            UiUtils.styleChip(tvState, "— No vest connected", R.color.rg_grey);
            UiUtils.addStatusRow(statusRows, "Vest", "✖ Not assigned", R.color.rg_grey);
            btnStartRide.setEnabled(false);
            return;
        }
        boolean connected = device.connected();
        if (!connected) {
            UiUtils.styleChip(tvState, "✖ Vest disconnected", R.color.rg_grey);
        } else if (IncidentStatus.READY.equals(device.state)) {
            UiUtils.styleChip(tvState, device.rideActive ? "✔ Riding: protection on" : "✔ Ready",
                    R.color.rg_green);
        } else {
            UiUtils.styleStatus(tvState, device.state);
        }

        UiUtils.addStatusRow(statusRows, "Vest", connected ? "✔ Connected" : "✖ Disconnected",
                connected ? R.color.rg_green : R.color.rg_grey);
        if (connected) {
            int color = device.battery > 30 ? R.color.rg_green
                    : device.battery > 15 ? R.color.rg_amber : R.color.rg_red;
            UiUtils.addStatusRow(statusRows, "Battery", "🔋 " + device.battery + "%", color);
        } else {
            UiUtils.addStatusRow(statusRows, "Battery", "— Unknown", R.color.rg_grey);
        }
        UiUtils.addCheckRow(statusRows, "Crash sensor", device.mpuOk, connected);
        UiUtils.addCheckRow(statusRows, "Front camera", device.frontCamOk, connected);
        UiUtils.addCheckRow(statusRows, "Rear camera", device.rearCamOk, connected);
        UiUtils.addCheckRow(statusRows, "Mobile network (4G)", device.networkOk, connected);
        UiUtils.addCheckRow(statusRows, "GPS location", device.gnssOk, connected);

        btnStartRide.setEnabled(connected);
        btnStartRide.setText(device.rideActive ? "End ride" : "Start ride");
    }

    private void toggleRide() {
        if (device == null) return;
        boolean start = !device.rideActive;
        repo.setRideActive(device.deviceId, start, (ok, err) -> {
            if (err != null) toast(err);
            else toast(start ? "Ride started. Stay safe!" : "Ride ended.");
        });
    }

    private void handleIncidents(List<Incident> list) {
        if (list == null) return;
        activeEmergency = null;
        long now = System.currentTimeMillis();
        for (Incident i : list) {
            if (IncidentStatus.isPending(i.status)) {
                if (i.countdownEndsAt > now && !handledCountdowns.contains(i.id)) {
                    handledCountdowns.add(i.id);
                    startActivity(new Intent(this, CountdownActivity.class)
                            .putExtra(Extras.INCIDENT_ID, i.id));
                }
            } else if (activeEmergency == null && IncidentStatus.isOpenEmergency(i.status)) {
                activeEmergency = i;
            }
        }
        tvActive.setVisibility(activeEmergency != null ? View.VISIBLE : View.GONE);
    }

    /** SOS needs a 2-second hold so it is not sent by an accidental tap. */
    @SuppressLint("ClickableViewAccessibility")
    private void setupSosButton() {
        btnSos.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    btnSos.setText("Keep holding…");
                    handler.postDelayed(sosRunnable, SOS_HOLD_MS);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(sosRunnable);
                    btnSos.setText(R.string.sos_label);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private void sendSos() {
        btnSos.setText(R.string.sos_label);
        if (rider == null) {
            toast("Your profile is still loading. Try again in a moment.");
            return;
        }
        toast("Sending SOS…");
        repo.triggerManualSos(rider, (id, err) -> {
            if (isFinishing()) return;
            if (err != null || id == null) {
                toast(err != null ? err : "Could not send SOS.");
                return;
            }
            startActivity(EmergencyActivity.intent(this, id));
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(sosRunnable);
        if (deviceHandle != null) deviceHandle.remove();
        super.onDestroy();
    }
}
