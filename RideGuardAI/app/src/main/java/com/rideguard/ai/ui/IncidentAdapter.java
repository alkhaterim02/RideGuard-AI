package com.rideguard.ai.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Incident cards. Active emergencies get a red border and background. */
public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.VH> {
    public interface OnClick {
        void onClick(Incident i);
    }

    private final List<Incident> items = new ArrayList<>();
    private final boolean showRider;
    private final OnClick click;

    public IncidentAdapter(boolean showRider, OnClick click) {
        this.showRider = showRider;
        this.click = click;
    }

    public void setItems(List<Incident> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Incident i = items.get(position);
        Context c = h.itemView.getContext();
        boolean manual = "manual_sos".equals(i.type);

        UiUtils.styleStatus(h.status, i.status);
        h.title.setText(showRider ? i.riderName + "  •  " + i.incidentNumber
                : i.incidentNumber + (manual ? "  •  Manual SOS" : ""));
        h.time.setText(UiUtils.formatTime(i.createdAt));
        h.location.setText("📍 " + UiUtils.locationText(i));
        boolean hasVideo = i.frontVideoUrl != null || i.rearVideoUrl != null;
        h.extras.setText("Alert: " + UiUtils.smsLabel(i.smsStatus) + "\nVideos: "
                + (hasVideo ? "✔ Available" : UiUtils.videoLabel(i.frontVideoStatus, i.frontVideoProgress)));

        boolean urgent = IncidentStatus.isOpenEmergency(i.status) || IncidentStatus.isPending(i.status);
        h.card.setStrokeColor(ContextCompat.getColor(c, R.color.rg_red));
        h.card.setStrokeWidth(urgent ? UiUtils.dp(c, 3) : 0);
        h.card.setCardBackgroundColor(ContextCompat.getColor(c, urgent ? R.color.rg_red_light : R.color.white));

        h.itemView.setOnClickListener(v -> click.onClick(i));
        h.details.setOnClickListener(v -> click.onClick(i));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView status, title, time, location, extras;
        final View details;

        VH(View v) {
            super(v);
            card = (MaterialCardView) v;
            status = v.findViewById(R.id.itemStatus);
            title = v.findViewById(R.id.itemTitle);
            time = v.findViewById(R.id.itemTime);
            location = v.findViewById(R.id.itemLocation);
            extras = v.findViewById(R.id.itemExtras);
            details = v.findViewById(R.id.itemDetails);
        }
    }
}
