package com.example.kedaimakankai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        MaterialButton btnAdminViewBookings = findViewById(R.id.btnAdminViewBookings);
        MaterialButton btnManageBooking = findViewById(R.id.btnManageBooking);
        MaterialButton btnLogout = findViewById(R.id.btnLogoutAdmin);

        btnAdminViewBookings.setOnClickListener(v -> {
            // View Bookings: View all bookings (read-only view)
            Intent i = new Intent(this, ViewBookingsActivity.class);
            i.putExtra("mode", "admin");
            startActivity(i);
        });

        btnManageBooking.setOnClickListener(v -> {
            // Manage Booking: Opens Booking Approval page where admin can approve/reject bookings
            startActivity(new Intent(this, BookingApprovalActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            new SessionManager(this).logout();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
