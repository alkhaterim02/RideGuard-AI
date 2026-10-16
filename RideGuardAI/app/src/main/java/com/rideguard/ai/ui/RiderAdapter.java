package com.rideguard.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rideguard.ai.R;
import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiderAdapter extends RecyclerView.Adapter<RiderAdapter.VH> {
    public interface OnClick {
        void onClick(Rider r);
    }

    private final List<Rider> riders = new ArrayList<>();
    private final Map<String, DeviceStatus> devices = new HashMap<>();
    private final OnClick click;

    public RiderAdapter(OnClick click) {
        this.click = click;
    }

    public void setRiders(List<Rider> list) {
        riders.clear();
        if (list != null) riders.addAll(list);
        notifyDataSetChanged();
    }

    public void setDevices(Map<String, DeviceStatus> map) {
        devices.clear();
        devices.putAll(map);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rider, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Rider r = riders.get(position);
        h.name.setText(r.fullName);
        h.info.setText("ID: " + (r.riderCode == null ? "—" : r.riderCode) + "   •   "
                + (r.phone == null ? "—" : r.phone));

        String deviceText;
        if (r.deviceId == null) {
            deviceText = "No vest assigned";
        } else {
            DeviceStatus d = devices.get(r.deviceId);
            if (d == null) deviceText = r.deviceId + ": not registered yet";
            else if (!d.connected()) deviceText = r.deviceId + ": ✖ offline";
            else deviceText = r.deviceId + ": ✔ connected, 🔋 " + d.battery + "%"
                        + (d.hasFault() ? "\n⚠ Needs attention (check sensors or GPS)" : "");
        }
        h.device.setText(deviceText);

        if ("invited".equals(r.status)) UiUtils.styleChip(h.status, "… Invited", R.color.rg_amber);
        else if ("inactive".equals(r.status)) UiUtils.styleChip(h.status, "— Inactive", R.color.rg_grey);
        else UiUtils.styleChip(h.status, "✔ Active", R.color.rg_green);

        h.itemView.setOnClickListener(v -> click.onClick(r));
    }

    @Override
    public int getItemCount() {
        return riders.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView status, name, info, device;

        VH(View v) {
            super(v);
            status = v.findViewById(R.id.riderStatus);
            name = v.findViewById(R.id.riderName);
            info = v.findViewById(R.id.riderInfo);
            device = v.findViewById(R.id.riderDevice);
        }
    }
}
