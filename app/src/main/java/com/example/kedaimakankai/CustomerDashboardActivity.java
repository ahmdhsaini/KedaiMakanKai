package com.example.kedaimakankai;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

public class CustomerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        MaterialButton btnMakeBooking = findViewById(R.id.btnMakeBooking);
        MaterialButton btnViewMyBookings = findViewById(R.id.btnViewMyBookings);
        MaterialButton btnLogout = findViewById(R.id.btnLogoutCustomer);

        btnMakeBooking.setOnClickListener(v -> startActivity(new Intent(this, MakeBookingActivity.class)));

        btnViewMyBookings.setOnClickListener(v -> {
            Intent i = new Intent(this, ViewBookingsActivity.class);
            i.putExtra("mode", "customer");
            startActivity(i);
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
