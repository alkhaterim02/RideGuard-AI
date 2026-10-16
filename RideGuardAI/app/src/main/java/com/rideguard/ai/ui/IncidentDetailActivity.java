package com.rideguard.ai.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.util.Extras;
import com.rideguard.ai.util.UiUtils;

import java.util.Locale;

/** 13.9 Accident details and video (rider) and 14.4 Emergency details (company). */
public class IncidentDetailActivity extends BaseActivity {
    private String incidentId;
    private Incident incident;
    private boolean isCompany;
    private boolean viewLogged;

    private TextView tvStatus, tvRider, tvNumber, tvTime, tvLocation, tvPeople, tvHealth, tvSensor;
    private TextView tvFrontStatus, tvRearStatus, tvResult, tvViewLog;
    private View cardPeople;
    private MaterialButton btnMap, btnPlayFront, btnDownloadFront, btnPlayRear, btnDownloadRear;
    private MaterialButton btnAck, btnClose, btnDeleteReq;

    public static Intent intent(Context c, String incidentId) {
        return new Intent(c, IncidentDetailActivity.class).putExtra(Extras.INCIDENT_ID, incidentId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_incident_detail);
        setTitle("Incident details");
        showBack();

        incidentId = getIntent().getStringExtra(Extras.INCIDENT_ID);
        isCompany = session.isCompany();

        tvStatus = findViewById(R.id.tvStatus);
        tvRider = findViewById(R.id.tvRider);
        tvNumber = findViewById(R.id.tvNumber);
        tvTime = findViewById(R.id.tvTime);
        tvLocation = findViewById(R.id.tvLocation);
        tvPeople = findViewById(R.id.tvPeople);
        tvHealth = findViewById(R.id.tvHealth);
        tvSensor = findViewById(R.id.tvSensor);
        tvFrontStatus = findViewById(R.id.tvFrontStatus);
        tvRearStatus = findViewById(R.id.tvRearStatus);
        tvResult = findViewById(R.id.tvResult);
        tvViewLog = findViewById(R.id.tvViewLog);
        cardPeople = findViewById(R.id.cardPeople);
        btnMap = findViewById(R.id.btnMap);
        btnPlayFront = findViewById(R.id.btnPlayFront);
        btnDownloadFront = findViewById(R.id.btnDownloadFront);
        btnPlayRear = findViewById(R.id.btnPlayRear);
        btnDownloadRear = findViewById(R.id.btnDownloadRear);
        btnAck = findViewById(R.id.btnAck);
        btnClose = findViewById(R.id.btnClose);
        btnDeleteReq = findViewById(R.id.btnDeleteReq);

        btnMap.setOnClickListener(v -> {
            if (incident != null && incident.hasLocation()) {
                UiUtils.openMap(this, incident.latitude, incident.longitude, incident.riderName);
            }
        });
        findViewById(R.id.btnCallRider).setOnClickListener(v -> {
            if (incident != null) UiUtils.dial(this, incident.riderPhone);
        });
        findViewById(R.id.btnCallContact).setOnClickListener(v -> {
            if (incident != null) UiUtils.dial(this, incident.contactPhone);
        });
        btnPlayFront.setOnClickListener(v -> {
            if (incident != null) play(incident.frontVideoUrl, "Front accident video");
        });
        btnPlayRear.setOnClickListener(v -> {
            if (incident != null) play(incident.rearVideoUrl, "Rear accident video");
        });
        btnDownloadFront.setOnClickListener(v -> {
            if (incident != null) download(incident.frontVideoUrl);
        });
        btnDownloadRear.setOnClickListener(v -> {
            if (incident != null) download(incident.rearVideoUrl);
        });
        btnAck.setOnClickListener(v -> repo.acknowledgeIncident(incidentId, displayName(), (ok, err) ->
                toast(err == null ? "Incident acknowledged." : err)));
        btnClose.setOnClickListener(v -> askClose());
        btnDeleteReq.setOnClickListener(v -> UiUtils.confirm(this, "Request video deletion?",
                "Your company will review the request according to the privacy policy. Videos may be kept "
                        + "if the law requires it.", "Send request",
                () -> repo.requestVideoDeletion(incidentId, (ok, err) ->
                        toast(err == null ? "Deletion request sent." : err))));

        track(repo.observeIncident(incidentId, (i, err) -> {
            if (i == null) {
                toast(err);
                return;
            }
            render(i);
        }));
    }

    private String displayName() {
        return session.displayName == null ? "Company user" : session.displayName;
    }

    private void render(Incident i) {
        incident = i;
        if (isCompany && !viewLogged) {
            viewLogged = true;
            repo.logIncidentView(i.id, displayName()); // privacy: record who opened the incident
        }
        boolean manual = "manual_sos".equals(i.type);

        UiUtils.styleStatus(tvStatus, i.status);
        tvRider.setText(i.riderName);
        tvNumber.setText(i.incidentNumber + (manual ? "  •  Manual SOS" : "  •  Automatic detection"));
        tvTime.setText("Date and time: " + UiUtils.formatTime(i.createdAt));
        tvLocation.setText("Location: " + UiUtils.locationText(i));
        btnMap.setEnabled(i.hasLocation());

        cardPeople.setVisibility(isCompany ? View.VISIBLE : View.GONE);
        tvPeople.setText("Rider phone: " + nz(i.riderPhone)
                + "\nEmergency contact: " + nz(i.contactName) + " (" + nz(i.contactPhone) + ")"
                + "\nSMS to contact: " + UiUtils.smsLabel(i.smsStatus));
        tvHealth.setText(i.healthSnapshot == null ? "No health information provided" : i.healthSnapshot);

        tvSensor.setText(manual ? "Manual SOS sent by the rider. No crash sensor data."
                : String.format(Locale.US, "Impact strength: %.1f g\nRotation: %.0f°/s\nTilt: %.0f°\n"
                        + "AI verification score: %.0f%%",
                i.maxAcceleration, i.maxRotation, i.tiltDeg, i.aiScore * 100));

        renderVideo(tvFrontStatus, btnPlayFront, btnDownloadFront, i.frontVideoStatus,
                i.frontVideoProgress, i.frontVideoUrl);
        renderVideo(tvRearStatus, btnPlayRear, btnDownloadRear, i.rearVideoStatus,
                i.rearVideoProgress, i.rearVideoUrl);

        StringBuilder result = new StringBuilder();
        if (i.acknowledged) result.append("Acknowledged by ").append(nz(i.acknowledgedBy));
        if (i.finalResult != null) {
            if (result.length() > 0) result.append("\n");
            result.append("Result: ").append(i.finalResult);
        }
        if (i.deleteRequested) {
            if (result.length() > 0) result.append("\n");
            result.append("Video deletion requested");
        }
        tvResult.setText(result.toString());
        tvResult.setVisibility(result.length() > 0 ? View.VISIBLE : View.GONE);

        boolean hasLog = isCompany && i.viewLog != null && !i.viewLog.isEmpty();
        tvViewLog.setVisibility(hasLog ? View.VISIBLE : View.GONE);
        if (hasLog) tvViewLog.setText("Viewed by:\n" + TextUtils.join("\n", i.viewLog));

        btnAck.setVisibility(isCompany && IncidentStatus.isOpenEmergency(i.status) && !i.acknowledged
                ? View.VISIBLE : View.GONE);
        boolean closable = !IncidentStatus.CLOSED.equals(i.status) && !IncidentStatus.CANCELLED.equals(i.status)
                && !IncidentStatus.isPending(i.status);
        btnClose.setVisibility(isCompany && closable ? View.VISIBLE : View.GONE);
        boolean hasVideo = i.frontVideoUrl != null || i.rearVideoUrl != null;
        btnDeleteReq.setVisibility(!isCompany && hasVideo && !i.deleteRequested ? View.VISIBLE : View.GONE);
    }

    private static String nz(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }

    private void renderVideo(TextView status, MaterialButton play, MaterialButton download,
                             String videoStatus, int progress, String url) {
        UiUtils.styleChip(status, UiUtils.videoLabel(videoStatus, progress), UiUtils.videoColor(videoStatus));
        boolean ready = url != null && "uploaded".equals(videoStatus);
        play.setEnabled(ready);
        download.setEnabled(ready);
    }

    private void play(String url, String title) {
        repo.resolveVideoUrl(url, (u, err) -> {
            if (err != null || u == null) toast(err != null ? err : "Video not available.");
            else startActivity(VideoPlayerActivity.intent(this, u, title));
        });
    }

    private void download(String url) {
        repo.resolveVideoUrl(url, (u, err) -> {
            if (err != null || u == null) toast(err != null ? err : "Video not available.");
            else UiUtils.openUrl(this, u);
        });
    }

    private void askClose() {
        EditText input = new EditText(this);
        input.setHint("Final result (e.g. rider taken to hospital)");
        FrameLayout box = new FrameLayout(this);
        int p = UiUtils.dp(this, 20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(input);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Close this incident?")
                .setMessage("Only close the incident when the rider is safe or with emergency services.")
                .setView(box)
                .setPositiveButton("Close incident", (d, w) -> {
                    String r = input.getText().toString().trim();
                    repo.closeIncident(incidentId, r.isEmpty() ? "Closed by company" : r, (ok, err) ->
                            toast(err == null ? "Incident closed and saved." : err));
                })
                .setNegativeButton("Go back", null)
                .show();
    }
}
