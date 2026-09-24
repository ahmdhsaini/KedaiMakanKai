package com.example.kedaimakankai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kedaimakankai.R;
import com.example.kedaimakankai.model.Booking;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.Holder> {

    public interface OnBookingAction {
        void onCancel(Booking booking);
    }

    private List<Booking> data = new ArrayList<>();
    private final OnBookingAction listener;
    private final boolean allowCancel;

    public BookingAdapter(List<Booking> list) {
        this(list, null, false);
    }

    public BookingAdapter(List<Booking> list, OnBookingAction listener, boolean allowCancel) {
        if (list != null) data = list;
        this.listener = listener;
        this.allowCancel = allowCancel;
    }

    public void setData(List<Booking> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_booking, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Booking b = data.get(position);
        if (b == null) return;

        // MAIN LINE
        String mainText = "Booking #" + (b.BookID != null ? b.BookID : "-") +
                " • " + (b.Capacity != null ? b.Capacity : "0") + " pax";
        h.tvMain.setText(mainText);

        // SUB LINE
        String subText = "Time: " + (b.TimeSlot != null ? b.TimeSlot : "-");
        h.tvSub.setText(subText);

        String status = (b.LatestStatus == null || b.LatestStatus.trim().isEmpty())
                ? "PENDING" : b.LatestStatus.trim().toUpperCase();
        if (h.tvStatus != null) {
            h.tvStatus.setText("Status: " + status);
        }

        // Show AdminNote if status is REJECTED
        if (h.tvAdminNote != null) {
            if ("REJECTED".equalsIgnoreCase(status) && b.LatestAdminNote != null && !b.LatestAdminNote.trim().isEmpty()) {
                h.tvAdminNote.setText("Reason: " + b.LatestAdminNote);
                h.tvAdminNote.setVisibility(View.VISIBLE);
            } else {
                h.tvAdminNote.setVisibility(View.GONE);
            }
        }

        // Customer can cancel only if: PENDING status (not CANCELLED, REJECTED, or APPROVED)
        boolean canCancel = allowCancel && "PENDING".equalsIgnoreCase(status);
        if (h.btnCancel != null) {
            h.btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            h.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(b);
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvMain, tvSub, tvStatus, tvAdminNote;
        MaterialButton btnCancel;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvMain = itemView.findViewById(R.id.tvMain);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdminNote = itemView.findViewById(R.id.tvAdminNote);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
