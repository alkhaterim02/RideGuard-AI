package com.rideguard.ai.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rideguard.ai.R;

/** 13.8 Rider accident history and 14.6 Company emergency history. */
public class HistoryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_list);
        showBack();

        boolean company = session.isCompany();
        setTitle(company ? "Emergency history" : "Accident history");

        RecyclerView rv = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.tvEmpty);
        empty.setText("No accidents or alerts recorded yet.");
        rv.setLayoutManager(new LinearLayoutManager(this));
        IncidentAdapter adapter = new IncidentAdapter(company,
                i -> startActivity(IncidentDetailActivity.intent(this, i.id)));
        rv.setAdapter(adapter);

        track(repo.observeIncidents(session.companyId, company ? null : session.riderId, (list, err) -> {
            adapter.setItems(list);
            empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            if (err != null) toast(err);
        }));
    }
}
