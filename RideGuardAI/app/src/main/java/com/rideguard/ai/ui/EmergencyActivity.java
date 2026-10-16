package com.rideguard.ai.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.util.Extras;
import com.rideguard.ai.util.UiUtils;

/** 13.7 Emergency confirmed page (rider side). */
public class EmergencyActivity extends BaseActivity {
    private String incidentId;
    private Incident incident;

    private TextView tvHeader, tvTime, tvLocation, tvFalseNote;
    private LinearLayout statusRows;
    private MaterialButton btnMap, btnCall, btnUpdate, btnFalse;

    public static Intent intent(Context c, String incidentId) {
        return new Intent(c, EmergencyActivity.class).putExtra(Extras.INCIDENT_ID, incidentId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_emergency);
        setTitle("Emergency");
        showBack();

        incidentId = getIntent().getStringExtra(Extras.INCIDENT_ID);
        tvHeader = findViewById(R.id.tvHeader);
        tvTime = findViewById(R.id.tvTime);
        tvLocation = findViewById(R.id.tvLocation);
        tvFalseNote = findViewById(R.id.tvFalseNote);
        statusRows = findViewById(R.id.statusRows);
        btnMap = findViewById(R.id.btnMap);
        btnCall = findViewById(R.id.btnCallContact);
        btnUpdate = findViewById(R.id.btnUpdateLocation);
        btnFalse = findViewById(R.id.btnFalseAlarm);

        btnMap.setOnClickListener(v -> {
            if (incident != null && incident.hasLocation()) {
                UiUtils.openMap(this, incident.latitude, incident.longitude, "My location");
            }
        });
        btnCall.setOnClickListener(v -> {
            if (incident != null) UiUtils.dial(this, incident.contactPhone);
        });
        btnUpdate.setOnClickListener(v -> repo.requestLocationUpdate(incidentId, (ok, err) ->
                toast(err == null ? "Location update requested." : err)));
        findViewById(R.id.btnDetails).setOnClickListener(v ->
                startActivity(IncidentDetailActivity.intent(this, incidentId)));
        btnFalse.setOnClickListener(v -> UiUtils.confirm(this, "Report a false alarm?",
                "The emergency SMS was already sent and cannot be removed. A follow-up message will "
                        + "tell your emergency contact that you are safe.",
                "Yes, I am safe",
                () -> repo.reportFalseAlarm(incidentId, (ok, err) ->
                        toast(err == null ? "Thank you. Your contact will be told you are safe." : err))));

        track(repo.observeIncident(incidentId, (i, err) -> render(i)));
    }

    private void render(Incident i) {
        if (i == null) return;
        incident = i;
        boolean closed = IncidentStatus.CLOSED.equals(i.status);

        tvHeader.setText(closed ? "✔ Emergency closed"
                : IncidentStatus.CONFIRMED.equals(i.status) ? "🚨 Sending emergency alert…"
                : "🚨 Emergency alert sent");
        tvHeader.setBackgroundColor(ContextCompat.getColor(this, closed ? R.color.rg_grey : R.color.rg_red));

        tvTime.setText("Accident time: " + UiUtils.formatTime(i.createdAt) + "\nIncident: " + i.incidentNumber
                + ("manual_sos".equals(i.type) ? " (manual SOS)" : ""));
        tvLocation.setText("Location: " + UiUtils.locationText(i));
        btnMap.setEnabled(i.hasLocation());

        statusRows.removeAllViews();
        UiUtils.addStatusRow(statusRows, "Emergency contact (SMS)", UiUtils.smsLabel(i.smsStatus),
                UiUtils.smsColor(i.smsStatus));
        UiUtils.addStatusRow(statusRows, "Company", i.companyNotified ? "✔ Notified" : "… Waiting",
                i.companyNotified ? R.color.rg_green : R.color.rg_amber);
        UiUtils.addStatusRow(statusRows, "Front video",
                UiUtils.videoLabel(i.frontVideoStatus, i.frontVideoProgress), UiUtils.videoColor(i.frontVideoStatus));
        UiUtils.addStatusRow(statusRows, "Rear video",
                UiUtils.videoLabel(i.rearVideoStatus, i.rearVideoProgress), UiUtils.videoColor(i.rearVideoStatus));

        btnCall.setText("Call " + (i.contactName != null ? i.contactName : "emergency contact"));
        btnUpdate.setVisibility(closed ? View.GONE : View.VISIBLE);
        btnFalse.setVisibility(!i.falseAlarmReported && !closed ? View.VISIBLE : View.GONE);
        tvFalseNote.setVisibility(i.falseAlarmReported ? View.VISIBLE : View.GONE);
    }
}
