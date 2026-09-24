package com.example.kedaimakankai;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kedaimakankai.adapter.BookingAdapter;
import com.example.kedaimakankai.model.Booking;
import com.example.kedaimakankai.model.Customer;
import com.example.kedaimakankai.model.Status;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.BookingService;
import com.example.kedaimakankai.remote.CustomerService;
import com.example.kedaimakankai.remote.StatusService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewBookingsActivity extends AppCompatActivity {

    private BookingAdapter adapter;
    private static final String TAG = "ViewBookings";
    private String mode = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookings);

        RecyclerView rv = findViewById(R.id.rvBookings);
        rv.setLayoutManager(new LinearLayoutManager(this));

        mode = getIntent() != null ? getIntent().getStringExtra("mode") : "customer";
        boolean allowCancel = !"admin".equalsIgnoreCase(mode);
        adapter = new BookingAdapter(null, booking -> confirmCancel(booking), allowCancel);
        rv.setAdapter(adapter);

        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload bookings when activity resumes to show latest statuses
        loadBookings();
    }

    private void loadBookings() {
        SessionManager session = new SessionManager(this);
        String role = session.getRole();
        if ("admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role)) {
            loadBookingsAdmin();
            return;
        }

        int custId = session.getCustomerId();
        if (custId > 0) {
            loadBookingsForCustomer(custId);
        } else {
            resolveCustomerIdThenLoad();
        }
    }

    private void loadBookingsAdmin() {
        BookingService api = ApiClient.getClient(this).create(BookingService.class);
        api.listAll().enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> res) {
                if (!res.isSuccessful()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Load bookings failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Booking> data = res.body();
                if (data == null || data.isEmpty()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "No bookings found",
                            Toast.LENGTH_SHORT).show();
                    adapter.setData(null);
                    return;
                }
                
                // Clear old status data to ensure fresh fetch
                for (Booking b : data) {
                    if (b != null) {
                        b.LatestStatus = null;
                        b.LatestAdminNote = null;
                    }
                }
                
                enrichBookingsWithStatusThenShow(data);
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                Toast.makeText(ViewBookingsActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBookingsForCustomer(int custId) {
        BookingService api = ApiClient.getClient(this).create(BookingService.class);
        api.listByCustomer(custId).enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> res) {
                if (!res.isSuccessful()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Load bookings failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Booking> data = res.body();
                if (data == null || data.isEmpty()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "No bookings found",
                            Toast.LENGTH_SHORT).show();
                    adapter.setData(null);
                    return;
                }
                
                // Clear old status data to ensure fresh fetch
                for (Booking b : data) {
                    if (b != null) {
                        b.LatestStatus = null;
                        b.LatestAdminNote = null;
                    }
                }
                
                enrichBookingsWithStatusThenShow(data);
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                Toast.makeText(ViewBookingsActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * If we don't have CustID saved yet, fetch CUSTOMER list and map logged-in user -> customer.
     * (Same idea as MakeBooking.)
     */
    private void resolveCustomerIdThenLoad() {
        SessionManager session = new SessionManager(this);
        int loggedInUserId = session.getUserId();
        String email = session.getEmail();

        if (loggedInUserId <= 0 && (email == null || email.trim().isEmpty())) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);
        customerApi.listAllRaw().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Load customer failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Customer> list = parseCustomerList(res.body());
                if (list == null || list.isEmpty()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Cannot resolve customer profile for this account.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                int custIdFound = -1;
                for (Customer c : list) {
                    if (c == null) continue;
                    if (c.UserID != null && c.UserID.id == loggedInUserId) {
                        custIdFound = (c.CustID == null) ? -1 : c.CustID;
                        break;
                    }
                    if (c.AccID != null && c.AccID.AccEmail != null && email != null
                            && email.equalsIgnoreCase(c.AccID.AccEmail)) {
                        custIdFound = (c.CustID == null) ? -1 : c.CustID;
                        break;
                    }
                    if (c.UserID != null && c.UserID.email != null && email != null
                            && email.equalsIgnoreCase(c.UserID.email)) {
                        custIdFound = (c.CustID == null) ? -1 : c.CustID;
                        break;
                    }
                }

                if (custIdFound <= 0) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Customer profile not found for this account.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                session.saveCustomerId(custIdFound);
                loadBookingsForCustomer(custIdFound);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "Unknown error";
                Toast.makeText(ViewBookingsActivity.this, "Network error: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Parse GET CUSTOMER response: raw array, single object, or wrapped { "data": [...] }. */
    private List<Customer> parseCustomerList(ResponseBody body) {
        String json = null;
        try {
            json = body.string();
            if (json == null || json.trim().isEmpty()) return new ArrayList<>();
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Customer>>() {}.getType();
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonArray()) {
                return gson.fromJson(root, listType);
            }
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                for (String key : new String[]{"data", "CUSTOMER", "customers", "result", "records"}) {
                    if (obj.has(key) && obj.get(key).isJsonArray()) {
                        return gson.fromJson(obj.getAsJsonArray(key), listType);
                    }
                }
                for (String key : obj.keySet()) {
                    if (obj.get(key).isJsonArray()) {
                        return gson.fromJson(obj.getAsJsonArray(key), listType);
                    }
                }
                for (String key : new String[]{"customer", "Customer", "data", "record"}) {
                    if (obj.has(key) && obj.get(key).isJsonObject()) {
                        Customer single = gson.fromJson(obj.getAsJsonObject(key), Customer.class);
                        if (single != null) {
                            List<Customer> list = new ArrayList<>();
                            list.add(single);
                            return list;
                        }
                    }
                }
                Customer single = gson.fromJson(obj, Customer.class);
                if (single != null) {
                    List<Customer> list = new ArrayList<>();
                    list.add(single);
                    return list;
                }
            }
            logUnrecognizedCustomerResponse(json, null);
            return null;
        } catch (Exception e) {
            logUnrecognizedCustomerResponse(json, e);
            return null;
        }
    }

    private void logUnrecognizedCustomerResponse(String json, Exception e) {
        String preview = (json != null && json.length() > 0)
                ? json.trim().substring(0, Math.min(600, json.trim().length())) : "(empty)";
        Log.w(TAG, "CUSTOMER response not recognized. Preview: " + preview);
        if (e != null) Log.w(TAG, "Parse error: " + e.getMessage(), e);
    }

    private void enrichBookingsWithStatusThenShow(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            adapter.setData(null);
            return;
        }

        StatusService statusApi = ApiClient.getClient(this).create(StatusService.class);
        
        // First, try to fetch all statuses at once (more efficient)
        statusApi.listAll().enqueue(new Callback<List<Status>>() {
            @Override
            public void onResponse(Call<List<Status>> call, Response<List<Status>> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    Log.d(TAG, "Fetched " + res.body().size() + " status records");
                    // Map all statuses by BookID, keeping only the latest for each booking
                    java.util.Map<Integer, Status> latestStatusMap = new java.util.HashMap<>();
                    for (Status s : res.body()) {
                        Integer bookId = (s != null) ? s.getBookId() : null;
                        if (s == null || bookId == null || s.BookingStatus == null) {
                            Log.w(TAG, "Skipping invalid status record: " + s);
                            continue;
                        }
                        Log.d(TAG, "Processing Status: BookID=" + bookId + ", Status=" + s.BookingStatus +
                                ", StatusID=" + s.StatusID + ", Time=" + s.StatusTime);

                        Status existing = latestStatusMap.get(bookId);
                        if (existing == null) {
                            latestStatusMap.put(bookId, s);
                        } else {
                            // Compare by StatusTime (most recent) first, then use StatusID as tiebreaker
                            String sTime = s.StatusTime != null ? s.StatusTime.trim() : "";
                            String existingTime = existing.StatusTime != null ? existing.StatusTime.trim() : "";
                            
                            boolean shouldUpdate = false;
                            
                            if (!sTime.isEmpty() && !existingTime.isEmpty()) {
                                // Compare timestamps (format: "2026-01-27 08:31:39")
                                int timeCompare = sTime.compareTo(existingTime);
                                if (timeCompare > 0) {
                                    // New timestamp is later
                                    shouldUpdate = true;
                                } else if (timeCompare == 0) {
                                    // Same timestamp - use StatusID as tiebreaker (higher = newer)
                                    if (s.StatusID != null && existing.StatusID != null && s.StatusID > existing.StatusID) {
                                        shouldUpdate = true;
                                    }
                                }
                            } else if (s.StatusID != null && existing.StatusID != null && s.StatusID > existing.StatusID) {
                                // Fallback to StatusID if timestamps are missing
                                shouldUpdate = true;
                            }
                            
                            if (shouldUpdate) {
                                latestStatusMap.put(bookId, s);
                            }
                        }
                    }
                    
                    // Apply statuses to bookings
                    for (Booking b : bookings) {
                        if (b == null || b.BookID == null) continue;
                        Status latest = latestStatusMap.get(b.BookID);
                        if (latest != null && latest.BookingStatus != null) {
                            b.LatestStatus = latest.BookingStatus;
                            b.LatestAdminNote = latest.AdminNote; // Store admin note
                            Log.d(TAG, "✓ Booking #" + b.BookID + " → Status: " + latest.BookingStatus + 
                                    " (StatusID: " + latest.StatusID + ", Time: " + latest.StatusTime + 
                                    ", AdminNote: " + latest.AdminNote + ")");
                        } else {
                            b.LatestStatus = "PENDING";
                            b.LatestAdminNote = null;
                            Log.w(TAG, "✗ Booking #" + b.BookID + " - No status found in map (total statuses: " + 
                                    latestStatusMap.size() + "), defaulting to PENDING");
                            // Debug: show all BookIDs in map
                            Log.d(TAG, "Available BookIDs in status map: " + latestStatusMap.keySet());
                        }
                    }
                } else {
                    // Fallback: if listAll fails, set all to PENDING
                    for (Booking b : bookings) {
                        if (b != null) {
                            if (b.LatestStatus == null) {
                                b.LatestStatus = "PENDING";
                            }
                            b.LatestAdminNote = null;
                        }
                    }
                }
                adapter.setData(bookings);
            }

            @Override
            public void onFailure(Call<List<Status>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch all statuses, trying individual queries: " + t.getMessage());
                // Fallback to individual queries
                enrichBookingsWithStatusIndividual(bookings, statusApi);
            }
        });
    }

    private void enrichBookingsWithStatusIndividual(List<Booking> bookings, StatusService statusApi) {
        final int total = bookings.size();
        final int[] done = {0};

        for (Booking b : bookings) {
            Integer bookId = b != null ? b.BookID : null;
            if (bookId == null || bookId <= 0) {
                if (b != null) {
                    if (b.LatestStatus == null) b.LatestStatus = "PENDING";
                    b.LatestAdminNote = null;
                }
                done[0]++;
                if (done[0] == total) adapter.setData(bookings);
                continue;
            }

            statusApi.listByBooking(bookId).enqueue(new Callback<List<Status>>() {
                @Override
                public void onResponse(Call<List<Status>> call, Response<List<Status>> res) {
                    try {
                        if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                            Status latest = null;
                            for (Status s : res.body()) {
                                Integer sBookId = (s != null) ? s.getBookId() : null;
                                if (s == null || s.BookingStatus == null || sBookId == null) continue;
                                if (latest == null) {
                                    latest = s;
                                    continue;
                                }
                                // Compare by StatusTime (most recent) first, then use StatusID as tiebreaker
                                String sTime = s.StatusTime != null ? s.StatusTime.trim() : "";
                                String latestTime = latest.StatusTime != null ? latest.StatusTime.trim() : "";
                                
                                boolean shouldUpdate = false;
                                
                                if (!sTime.isEmpty() && !latestTime.isEmpty()) {
                                    // Compare timestamps (format: "2026-01-27 08:31:39")
                                    int timeCompare = sTime.compareTo(latestTime);
                                    if (timeCompare > 0) {
                                        // New timestamp is later
                                        shouldUpdate = true;
                                    } else if (timeCompare == 0) {
                                        // Same timestamp - use StatusID as tiebreaker (higher = newer)
                                        if (s.StatusID != null && latest.StatusID != null && s.StatusID > latest.StatusID) {
                                            shouldUpdate = true;
                                        }
                                    }
                                } else if (s.StatusID != null && latest.StatusID != null && s.StatusID > latest.StatusID) {
                                    // Fallback to StatusID if timestamps are missing
                                    shouldUpdate = true;
                                }
                                
                                if (shouldUpdate) {
                                    latest = s;
                                }
                            }
                            if (latest != null && latest.BookingStatus != null) {
                                b.LatestStatus = latest.BookingStatus;
                                b.LatestAdminNote = latest.AdminNote; // Store admin note
                                Log.d(TAG, "Booking #" + bookId + " status: " + latest.BookingStatus + 
                                        ", AdminNote: " + latest.AdminNote);
                            } else {
                                b.LatestStatus = "PENDING";
                                b.LatestAdminNote = null;
                            }
                        } else {
                            // No status found - default to PENDING
                            if (b.LatestStatus == null) {
                                b.LatestStatus = "PENDING";
                                b.LatestAdminNote = null;
                                Log.w(TAG, "No status found for Booking #" + bookId + ", defaulting to PENDING");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing status for Booking #" + bookId, e);
                        if (b.LatestStatus == null) {
                            b.LatestStatus = "PENDING";
                            b.LatestAdminNote = null;
                        }
                    } finally {
                        done[0]++;
                        if (done[0] == total) adapter.setData(bookings);
                    }
                }

                @Override
                public void onFailure(Call<List<Status>> call, Throwable t) {
                    Log.e(TAG, "Failed to fetch status for Booking #" + bookId + ": " + t.getMessage());
                    if (b.LatestStatus == null) b.LatestStatus = "PENDING";
                    done[0]++;
                    if (done[0] == total) adapter.setData(bookings);
                }
            });
        }
    }

    private void confirmCancel(Booking booking) {
        if (booking == null || booking.BookID == null || booking.BookID <= 0) return;

        String status = booking.LatestStatus == null ? "PENDING" : booking.LatestStatus.toUpperCase();
        
        if ("CANCELLED".equalsIgnoreCase(status)) {
            Toast.makeText(this, "This booking is already cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("REJECTED".equalsIgnoreCase(status)) {
            Toast.makeText(this, "This booking has been rejected by admin. Cannot cancel.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("APPROVED".equalsIgnoreCase(status)) {
            Toast.makeText(this, "This booking has been approved. Please contact admin to cancel.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Only allow canceling PENDING bookings
        if (!"PENDING".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Cannot cancel booking with status: " + status, Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Booking #" + booking.BookID;
        if (booking.TimeSlot != null) {
            message += "\nTime: " + booking.TimeSlot;
        }
        message += "\n\nAre you sure you want to cancel this booking?";
        
        new AlertDialog.Builder(this)
                .setTitle("Cancel booking?")
                .setMessage(message)
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .setPositiveButton("Yes, cancel", (d, w) -> doCancel(booking))
                .show();
    }

    private void doCancel(Booking booking) {
        if (booking == null || booking.BookID == null || booking.BookID <= 0) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }
        StatusService statusApi = ApiClient.getClient(this).create(StatusService.class);
        statusApi.create("CANCELLED", booking.BookID).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> res) {
                if (!res.isSuccessful()) {
                    Toast.makeText(ViewBookingsActivity.this,
                            "Cancel failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(ViewBookingsActivity.this, 
                        "Booking cancelled successfully", 
                        Toast.LENGTH_SHORT).show();
                
                // Reload bookings to get fresh status from database
                // Small delay to ensure database has saved
                RecyclerView rv = findViewById(R.id.rvBookings);
                if (rv != null) {
                    rv.postDelayed(() -> loadBookings(), 500);
                } else {
                    loadBookings();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ViewBookingsActivity.this,
                        "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
