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

public class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.Holder> {

    // ===== REQUIRED INTERFACE =====
    public interface OnApprovalAction {
        void onOpen(Booking booking);
        void onApprove(Booking booking);
        void onReject(Booking booking);
    }

    private final OnApprovalAction listener;
    private final List<Booking> data = new ArrayList<>();

    // ===== REQUIRED CONSTRUCTOR =====
    public ApprovalAdapter(OnApprovalAction listener) {
        this.listener = listener;
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

        String custIdText = (b.CustID != null && b.CustID.CustID != null) 
                ? String.valueOf(b.CustID.CustID) : "-";
        String mainText = "Booking #" + (b.BookID != null ? b.BookID : "-") +
                " • Cust ID " + custIdText +
                " • " + (b.Capacity != null ? b.Capacity : "0") + " pax";
        h.tvMain.setText(mainText);

        String status = (b.LatestStatus == null || b.LatestStatus.trim().isEmpty())
                ? "PENDING" : b.LatestStatus.trim().toUpperCase();

        String subText = "Time: " + (b.TimeSlot != null ? b.TimeSlot : "-");
        h.tvSub.setText(subText);
        
        if (h.tvStatus != null) {
            h.tvStatus.setText("Status: " + status);
        }

        // Admin approval UI: show Approve/Reject, hide Cancel
        // Hide buttons and show message if booking is already CANCELLED
        boolean isCancelled = "CANCELLED".equalsIgnoreCase(status);
        
        if (h.btnCancel != null) h.btnCancel.setVisibility(View.GONE);
        
        if (isCancelled) {
            // Hide buttons and show message for cancelled bookings
            if (h.btnApprove != null) h.btnApprove.setVisibility(View.GONE);
            if (h.btnReject != null) h.btnReject.setVisibility(View.GONE);
            if (h.tvCancelledMessage != null) {
                h.tvCancelledMessage.setVisibility(View.VISIBLE);
                h.tvCancelledMessage.setText("Cannot approve or reject - customer already cancelled this booking");
            }
        } else {
            // Show buttons for non-cancelled bookings
            if (h.btnApprove != null) {
                h.btnApprove.setVisibility(View.VISIBLE);
                h.btnApprove.setEnabled(true);
                h.btnApprove.setText("Approve");
            }
            if (h.btnReject != null) {
                h.btnReject.setVisibility(View.VISIBLE);
                h.btnReject.setEnabled(true);
                h.btnReject.setText("Reject");
            }
            if (h.tvCancelledMessage != null) {
                h.tvCancelledMessage.setVisibility(View.GONE);
            }
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(b);
        });

        if (h.btnApprove != null) {
            h.btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(b);
            });
        }

        if (h.btnReject != null) {
            h.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(b);
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView tvMain, tvSub, tvStatus, tvCancelledMessage;
        MaterialButton btnCancel, btnApprove, btnReject;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvMain = itemView.findViewById(R.id.tvMain);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCancelledMessage = itemView.findViewById(R.id.tvCancelledMessage);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
