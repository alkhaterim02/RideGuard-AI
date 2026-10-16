package com.rideguard.ai.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.ui.CompanyDashboardActivity;
import com.rideguard.ai.ui.RiderHomeActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared helpers for labels, colours, maps, phone calls and dialogs. Status is never shown by colour alone. */
public final class UiUtils {
    private UiUtils() {
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "—";
        return new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(new Date(millis));
    }

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------ status labels and colours

    public static String statusLabel(String s) {
        if (s == null) return "— Unknown";
        switch (s) {
            case IncidentStatus.READY:
                return "✔ Ready";
            case IncidentStatus.POSSIBLE:
                return "⚠ Checking possible accident";
            case IncidentStatus.COUNTDOWN:
                return "⚠ Countdown: cancel if you are OK";
            case IncidentStatus.CANCELLED:
                return "✖ Cancelled (false alarm)";
            case IncidentStatus.CONFIRMED:
                return "🚨 Emergency confirmed";
            case IncidentStatus.ALERT_SENT:
                return "🚨 Alert sent";
            case IncidentStatus.VIDEO_UPLOADING:
                return "🚨 Emergency: uploading videos";
            case IncidentStatus.ACTIVE:
                return "🚨 Emergency active";
            case IncidentStatus.CLOSED:
                return "✔ Closed";
            default:
                return s;
        }
    }

    public static int statusColorRes(String s) {
        if (IncidentStatus.isOpenEmergency(s)) return R.color.rg_red;
        if (IncidentStatus.isPending(s)) return R.color.rg_amber;
        if (IncidentStatus.READY.equals(s)) return R.color.rg_green;
        return R.color.rg_grey;
    }

    public static void styleChip(TextView tv, String text, int colorRes) {
        Context c = tv.getContext();
        tv.setText(text);
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, colorRes)));
        // Dark text on amber for contrast, white text on all other colours.
        tv.setTextColor(ContextCompat.getColor(c, colorRes == R.color.rg_amber ? R.color.rg_text : R.color.white));
    }

    public static void styleStatus(TextView tv, String status) {
        styleChip(tv, statusLabel(status), statusColorRes(status));
    }

    public static void addStatusRow(LinearLayout parent, String label, String value, int colorRes) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_status_row, parent, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        styleChip(row.findViewById(R.id.rowValue), value, colorRes);
        parent.addView(row);
    }

    /** known=false shows a grey "Unknown" (e.g. vest disconnected). */
    public static void addCheckRow(LinearLayout parent, String label, boolean ok, boolean known) {
        if (!known) addStatusRow(parent, label, "— Unknown", R.color.rg_grey);
        else if (ok) addStatusRow(parent, label, "✔ OK", R.color.rg_green);
        else addStatusRow(parent, label, "✖ Problem", R.color.rg_red);
    }

    public static String smsLabel(String s) {
        if ("sent".equals(s)) return "✔ Sent";
        if ("sending".equals(s)) return "… Sending";
        if ("failed".equals(s)) return "✖ Failed, retrying";
        return "— Not sent";
    }

    public static int smsColor(String s) {
        if ("sent".equals(s)) return R.color.rg_green;
        if ("failed".equals(s)) return R.color.rg_red;
        if ("sending".equals(s)) return R.color.rg_amber;
        return R.color.rg_grey;
    }

    public static String videoLabel(String status, int progress) {
        if (status == null) return "—";
        switch (status) {
            case "uploaded":
                return "✔ Uploaded";
            case "uploading":
                return "⬆ Uploading " + progress + "%";
            case "pending":
                return "… Waiting (saved on vest)";
            case "recording":
                return "● Recording";
            case "failed":
                return "✖ Upload failed, will retry";
            case "not_uploaded":
                return "— Not uploaded";
            default:
                return status;
        }
    }

    public static int videoColor(String status) {
        if ("uploaded".equals(status)) return R.color.rg_green;
        if ("failed".equals(status)) return R.color.rg_red;
        if ("uploading".equals(status) || "pending".equals(status) || "recording".equals(status)) {
            return R.color.rg_amber;
        }
        return R.color.rg_grey;
    }

    public static String locationText(Incident i) {
        if (!i.hasLocation()) return "Location pending…";
        String label = "last_known".equals(i.locationStatus) ? "last known"
                : "live".equals(i.locationStatus) ? "live" : "pending";
        return String.format(Locale.US, "%.5f, %.5f (%s)", i.latitude, i.longitude, label);
    }

    // ------------------------------------------------------------ actions

    public static String mapsLink(double lat, double lng) {
        return "https://maps.google.com/?q=" + lat + "," + lng;
    }

    public static void openMap(Context c, double lat, double lng, String label) {
        Uri geo = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng
                + "(" + Uri.encode(label == null ? "Accident" : label) + ")");
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException e) {
            openUrl(c, mapsLink(lat, lng));
        }
    }

    public static void openUrl(Context c, String url) {
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(c, "No app found to open this link.", Toast.LENGTH_LONG).show();
        }
    }

    public static void dial(Context c, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(c, "No phone number saved.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            c.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(c, "This device cannot make phone calls.", Toast.LENGTH_LONG).show();
        }
    }

    public static void confirm(Context c, String title, String message, String positive, Runnable onYes) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .setNegativeButton("Go back", null)
                .show();
    }

    public static void goHome(Activity a, UserSession s) {
        Class<?> target = s.isCompany() ? CompanyDashboardActivity.class : RiderHomeActivity.class;
        Intent i = new Intent(a, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
    }
}
