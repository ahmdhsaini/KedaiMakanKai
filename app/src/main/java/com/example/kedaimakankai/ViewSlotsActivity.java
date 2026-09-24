package com.example.kedaimakankai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.kedaimakankai.adapter.SlotAdapter;
import com.example.kedaimakankai.model.Slot;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.SlotService;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewSlotsActivity extends AppCompatActivity {

    private SlotAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_slots);

        RecyclerView rv = findViewById(R.id.rvSlots);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SlotAdapter(slot -> {
            Intent i = new Intent(ViewSlotsActivity.this, MakeBookingActivity.class);
            i.putExtra("slot_id", slot.slot_id);
            i.putExtra("table_name", slot.table_name);
            i.putExtra("date", slot.date);
            i.putExtra("time", slot.time);
            i.putExtra("max_pax", slot.max_pax);
            startActivity(i);
        });

        rv.setAdapter(adapter);

        loadSlots();
    }

    private void loadSlots() {
        SlotService api = ApiClient.getClient(this).create(SlotService.class);
        api.listAvailable().enqueue(new Callback<List<Slot>>() {
            @Override
            public void onResponse(Call<List<Slot>> call, Response<List<Slot>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    adapter.setData(res.body());
                } else {
                    // fallback dummy
                    adapter.setData(dummySlots());
                    Toast.makeText(ViewSlotsActivity.this, "Slots API failed (HTTP " + res.code() + "), showing dummy data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Slot>> call, Throwable t) {
                adapter.setData(dummySlots());
                Toast.makeText(ViewSlotsActivity.this, "Network error, showing dummy data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Slot> dummySlots() {
        List<Slot> list = new ArrayList<>();
        Slot a = new Slot();
        a.slot_id = 1; a.table_name = "Table 1"; a.max_pax = 2; a.date = "2026-01-26"; a.time = "20:00";
        Slot b = new Slot();
        b.slot_id = 2; b.table_name = "Table 2"; b.max_pax = 4; b.date = "2026-01-26"; b.time = "21:00";
        list.add(a); list.add(b);
        return list;
    }
}
