package com.example.kedaimakankai;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kedaimakankai.model.Customer;
import com.example.kedaimakankai.model.SimpleResponse;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.BookingService;
import com.example.kedaimakankai.remote.CustomerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeBookingActivity extends AppCompatActivity {

    private TextInputEditText etDate, etTime, etPax, etNotes;
    private MaterialButton btnSubmit;

    private String selectedDate = ""; // yyyy-MM-dd
    private String selectedTime = ""; // HH:mm:ss

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_booking);

        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPax = findViewById(R.id.etPax);
        etNotes = findViewById(R.id.etNotes);
        btnSubmit = findViewById(R.id.btnSubmitBooking);

        etDate.setOnClickListener(v -> openDatePicker());
        etTime.setOnClickListener(v -> openTimePicker());

        btnSubmit.setOnClickListener(v -> submitBooking());
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    etDate.setText(selectedDate);
                },
                y, m, d
        );

        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private void openTimePicker() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog dlg = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    selectedTime = String.format(Locale.US, "%02d:%02d:00", hourOfDay, minute1);
                    etTime.setText(selectedTime);
                },
                hour, minute, true
        );

        dlg.show();
    }

    private void submitBooking() {

        String paxStr = getText(etPax);

        if (selectedDate.isEmpty() || selectedTime.isEmpty() || paxStr.isEmpty()) {
            Toast.makeText(this, "Please fill date, time and pax", Toast.LENGTH_SHORT).show();
            return;
        }

        int pax;
        try {
            pax = Integer.parseInt(paxStr);
        } catch (Exception e) {
            Toast.makeText(this, "Pax must be number", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeSlot = selectedDate + " " + selectedTime;

        SessionManager session = new SessionManager(this);

        // ✅ We must use CUSTOMER.CustID (NOT users.id)
        int savedCustId = session.getCustomerId();

        if (savedCustId > 0) {
            createBooking(timeSlot, pax, savedCustId);
        } else {
            fetchCustomerIdAndCreate(timeSlot, pax);
        }
    }

    private void fetchCustomerIdAndCreate(String timeSlot, int pax) {

        SessionManager session = new SessionManager(this);
        String email = session.getEmail();
        int userId = session.getUserId();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);

        customerApi.listAll().enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(Call<List<Customer>> call, Response<List<Customer>> res) {

                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(MakeBookingActivity.this,
                            "Load customer failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Customer> list = res.body();
                int custIdFound = -1;

                // Try multiple ways to find the customer
                for (Customer c : list) {
                    if (c == null || c.CustID == null) continue;
                    
                    // Method 1: Match by UserID.id (most reliable - direct foreign key)
                    if (userId > 0 && c.UserID != null && c.UserID.id == userId) {
                        custIdFound = c.CustID;
                        break;
                    }
                    
                    // Method 2: Match by AccID.AccEmail
                    if (custIdFound <= 0 && c.AccID != null && c.AccID.AccEmail != null) {
                        if (email.equalsIgnoreCase(c.AccID.AccEmail)) {
                            custIdFound = c.CustID;
                            break;
                        }
                    }
                    
                    // Method 3: Match by UserID.email (fallback)
                    if (custIdFound <= 0 && c.UserID != null && c.UserID.email != null) {
                        if (email.equalsIgnoreCase(c.UserID.email)) {
                            custIdFound = c.CustID;
                            break;
                        }
                    }
                }

                if (custIdFound <= 0) {
                    // If customer record doesn't exist, try to create it
                    if (userId > 0) {
                        createCustomerRecordAndBooking(session, userId, timeSlot, pax);
                    } else {
                        Toast.makeText(MakeBookingActivity.this,
                                "Customer record not found. Please try logging out and back in.",
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                session.saveCustomerId(custIdFound);
                createBooking(timeSlot, pax, custIdFound);
            }

            @Override
            public void onFailure(Call<List<Customer>> call, Throwable t) {
                Toast.makeText(MakeBookingActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createCustomerRecordAndBooking(SessionManager session, int userId, String timeSlot, int pax) {
        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);
        
        // Use username as CustName, empty string as CustPhone
        String username = session.getUsername();
        String custName = username != null && !username.trim().isEmpty() ? username : "Customer";
        String custPhone = "";
        
        // Try primary endpoint first
        customerApi.createCustomer(userId, custName, custPhone).enqueue(new Callback<com.example.kedaimakankai.model.SimpleResponse>() {
            @Override
            public void onResponse(Call<com.example.kedaimakankai.model.SimpleResponse> call, 
                                 Response<com.example.kedaimakankai.model.SimpleResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    // Customer created, now try to fetch it again
                    Toast.makeText(MakeBookingActivity.this, "Customer record created. Creating booking...", Toast.LENGTH_SHORT).show();
                    fetchCustomerIdAndCreate(timeSlot, pax);
                } else {
                    // Try alternative endpoint
                    tryAlternativeCustomerCreationForBooking(customerApi, userId, timeSlot, pax, custName, custPhone);
                }
            }

            @Override
            public void onFailure(Call<com.example.kedaimakankai.model.SimpleResponse> call, Throwable t) {
                // Try alternative endpoint
                tryAlternativeCustomerCreationForBooking(customerApi, userId, timeSlot, pax, custName, custPhone);
            }
        });
    }
    
    private void tryAlternativeCustomerCreationForBooking(CustomerService customerApi, int userId, String timeSlot, int pax, String custName, String custPhone) {
        customerApi.createCustomerAlt(userId, custName, custPhone).enqueue(new Callback<com.example.kedaimakankai.model.SimpleResponse>() {
            @Override
            public void onResponse(Call<com.example.kedaimakankai.model.SimpleResponse> call, 
                                 Response<com.example.kedaimakankai.model.SimpleResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    Toast.makeText(MakeBookingActivity.this, "Customer record created. Creating booking...", Toast.LENGTH_SHORT).show();
                    fetchCustomerIdAndCreate(timeSlot, pax);
                } else {
                    Toast.makeText(MakeBookingActivity.this,
                            "Could not create customer record. Please contact support.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.kedaimakankai.model.SimpleResponse> call, Throwable t) {
                Toast.makeText(MakeBookingActivity.this,
                        "Error creating customer: " + t.getMessage() + ". Please contact support.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createBooking(String timeSlot, int pax, int custId) {

        BookingService api = ApiClient.getClient(this).create(BookingService.class);

        api.create(timeSlot, pax, custId).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {

                if (!res.isSuccessful()) {
                    Toast.makeText(MakeBookingActivity.this,
                            "Create failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(MakeBookingActivity.this,
                        "Booking created",
                        Toast.LENGTH_SHORT).show();

                finish();
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(MakeBookingActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText et) {
        return (et.getText() == null) ? "" : et.getText().toString().trim();
    }
}
