package com.rideguard.ai.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.util.Extras;

/** 13.6 Possible accident countdown. Opens over the lock screen. */
public class CountdownActivity extends BaseActivity {
    private String incidentId;
    private CountDownTimer timer;
    private Vibrator vibrator;
    private boolean done;

    private TextView tvCountdown, tvLocation, tvWaiting;
    private MaterialButton btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_countdown);
        incidentId = getIntent().getStringExtra(Extras.INCIDENT_ID);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvLocation = findViewById(R.id.tvLocation);
        tvWaiting = findViewById(R.id.tvWaiting);
        btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> cancelAlert());

        startVibration();
        track(repo.observeIncident(incidentId, (i, err) -> render(i)));
    }

    private void render(Incident i) {
        if (i == null || done) return;

        if (IncidentStatus.CANCELLED.equals(i.status)) {
            done = true;
            stopAlerts();
            toast("Alert cancelled. Stay safe!");
            finish();
            return;
        }
        if (IncidentStatus.isOpenEmergency(i.status) || IncidentStatus.CLOSED.equals(i.status)) {
            done = true;
            stopAlerts();
            startActivity(EmergencyActivity.intent(this, i.id));
            finish();
            return;
        }

        tvLocation.setText(i.hasLocation() ? "📍 Location found" : "📍 Finding your location…");
        if (timer == null) {
            long remaining = Math.max(0, i.countdownEndsAt - System.currentTimeMillis());
            timer = new CountDownTimer(remaining, 200) {
                @Override
                public void onTick(long ms) {
                    tvCountdown.setText(String.valueOf((ms + 999) / 1000));
                }

                @Override
                public void onFinish() {
                    tvCountdown.setText("0");
                    tvWaiting.setVisibility(View.VISIBLE);
                    btnCancel.setEnabled(false);
                }
            }.start();
        }
    }

    private void cancelAlert() {
        btnCancel.setEnabled(false);
        btnCancel.setText("Cancelling…");
        repo.cancelIncident(incidentId, (ok, err) -> {
            if (err != null) {
                toast(err);
                btnCancel.setText("CANCEL ALERT");
            }
            // On success the incident changes to "cancelled" and render() closes this screen.
        });
    }

    @SuppressWarnings("deprecation")
    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 700, 500};
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlerts() {
        if (timer != null) timer.cancel();
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    protected void onDestroy() {
        stopAlerts();
        super.onDestroy();
    }
}
