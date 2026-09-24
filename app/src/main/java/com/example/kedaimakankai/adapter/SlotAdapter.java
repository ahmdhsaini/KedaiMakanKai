package com.example.kedaimakankai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.kedaimakankai.R;
import com.example.kedaimakankai.model.Slot;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.VH> {

    public interface OnSelectClick {
        void onSelect(Slot slot);
    }

    private final List<Slot> data = new ArrayList<>();
    private final OnSelectClick listener;

    public SlotAdapter(OnSelectClick listener) {
        this.listener = listener;
    }

    public void setData(List<Slot> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Slot s = data.get(position);

        h.tvMain.setText(s.table_name + " • " + s.max_pax + " pax");
        h.tvSub.setText(s.date + " • " + s.time);

        h.btnSelect.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(s);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMain, tvSub;
        MaterialButton btnSelect;

        VH(@NonNull View itemView) {
            super(itemView);
            tvMain = itemView.findViewById(R.id.tvSlotMain);
            tvSub = itemView.findViewById(R.id.tvSlotSub);
            btnSelect = itemView.findViewById(R.id.btnSelectSlot);
        }
    }
}
