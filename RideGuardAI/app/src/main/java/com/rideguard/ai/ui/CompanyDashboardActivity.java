package com.rideguard.ai.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.rideguard.ai.R;
import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Incident;
import com.rideguard.ai.model.IncidentStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.NotificationHelper;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 14.2 Company dashboard + 14.3 active emergency list (active ones always on top). */
public class CompanyDashboardActivity extends BaseActivity {
    private final List<Rider> riders = new ArrayList<>();
    private final List<DeviceStatus> devices = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    private TextView statRiders, statVests, statActive, statErrors, tvEmpty;
    private EditText etSearch;
    private IncidentAdapter adapter;

    @Override
    protected boolean showLogoutMenu() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_company_dashboard);
        setTitle("Company dashboard");

        LinearLayout grid = findViewById(R.id.statsGrid);
        LinearLayout row1 = newRow(grid);
        LinearLayout row2 = newRow(grid);
        statRiders = addTile(row1, "Registered riders");
        statVests = addTile(row1, "Connected vests");
        statActive = addTile(row2, "Active emergencies");
        statErrors = addTile(row2, "Vests with errors");

        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);
        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidentAdapter(true, i -> startActivity(IncidentDetailActivity.intent(this, i.id)));
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderList();
            }
        });

        findViewById(R.id.btnRiders).setOnClickListener(v ->
                startActivity(new Intent(this, RiderManagementActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        track(repo.observeRiders(session.companyId, (list, err) -> {
            riders.clear();
            if (list != null) riders.addAll(list);
            renderStats();
        }));
        track(repo.observeDevices(session.companyId, (list, err) -> {
            devices.clear();
            if (list != null) devices.addAll(list);
            renderStats();
        }));
        track(repo.observeIncidents(session.companyId, null, (list, err) -> {
            incidents.clear();
            if (list != null) incidents.addAll(list);
            renderStats();
            renderList();
            if (err != null) toast(err);
        }));
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    private LinearLayout newRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView addTile(LinearLayout row, String label) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(UiUtils.dp(this, 14));
        card.setCardElevation(UiUtils.dp(this, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        int m = UiUtils.dp(this, 4);
        lp.setMargins(m, m, m, m);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        int p = UiUtils.dp(this, 14);
        inner.setPadding(p, p, p, p);

        TextView number = new TextView(this);
        number.setTextSize(30);
        number.setTypeface(Typeface.DEFAULT_BOLD);
        number.setTextColor(ContextCompat.getColor(this, R.color.rg_text));
        number.setText("–");
        TextView caption = new TextView(this);
        caption.setTextSize(14);
        caption.setTextColor(ContextCompat.getColor(this, R.color.rg_grey));
        caption.setText(label);

        inner.addView(number);
        inner.addView(caption);
        card.addView(inner);
        row.addView(card, lp);
        return number;
    }

    private void renderStats() {
        statRiders.setText(String.valueOf(riders.size()));
        int connected = 0;
        int errors = 0;
        for (DeviceStatus d : devices) {
            if (d.connected()) {
                connected++;
                if (d.hasFault()) errors++;
            }
        }
        int active = 0;
        for (Incident i : incidents) {
            if (IncidentStatus.isOpenEmergency(i.status) || IncidentStatus.isPending(i.status)) active++;
        }
        statVests.setText(String.valueOf(connected));
        statErrors.setText(String.valueOf(errors));
        statErrors.setTextColor(ContextCompat.getColor(this, errors > 0 ? R.color.rg_red : R.color.rg_text));
        statActive.setText(active > 0 ? "🚨 " + active : "0");
        statActive.setTextColor(ContextCompat.getColor(this, active > 0 ? R.color.rg_red : R.color.rg_text));
    }

    private static boolean urgent(Incident i) {
        return IncidentStatus.isOpenEmergency(i.status) || IncidentStatus.isPending(i.status);
    }

    private void renderList() {
        String q = FormText.lower(etSearch);
        List<Incident> list = new ArrayList<>();
        for (Incident i : incidents) {
            if (q.isEmpty()
                    || (i.riderName != null && i.riderName.toLowerCase(Locale.ROOT).contains(q))
                    || (i.incidentNumber != null && i.incidentNumber.toLowerCase(Locale.ROOT).contains(q))) {
                list.add(i);
            }
        }
        Collections.sort(list, (a, b) -> {
            boolean ua = urgent(a), ub = urgent(b);
            if (ua != ub) return ua ? -1 : 1;
            return Long.compare(b.createdAt, a.createdAt);
        });
        if (q.isEmpty() && list.size() > 20) list = new ArrayList<>(list.subList(0, 20));
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Small helper to read the search box. */
    private static final class FormText {
        static String lower(EditText e) {
            return e.getText() == null ? "" : e.getText().toString().trim().toLowerCase(Locale.ROOT);
        }
    }
}
