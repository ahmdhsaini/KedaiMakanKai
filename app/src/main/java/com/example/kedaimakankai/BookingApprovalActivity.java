package com.example.kedaimakankai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kedaimakankai.adapter.ApprovalAdapter;
import com.example.kedaimakankai.model.Booking;
import com.example.kedaimakankai.model.Status;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.BookingService;
import com.example.kedaimakankai.remote.StatusService;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingApprovalActivity extends AppCompatActivity {

    private ApprovalAdapter adapter;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_approval);

        rv = findViewById(R.id.rvApprovals);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ApprovalAdapter(new ApprovalAdapter.OnApprovalAction() {
            @Override
            public void onOpen(Booking booking) {
                if (booking == null || booking.BookID == null) return;
                String status = booking.LatestStatus == null ? "PENDING" : booking.LatestStatus.toUpperCase();
                
                AlertDialog.Builder builder = new AlertDialog.Builder(BookingApprovalActivity.this)
                        .setTitle("Booking #" + booking.BookID)
                        .setMessage("Current status: " + status)
                        .setNegativeButton("Close", (d, w) -> d.dismiss());
                
                // Only allow approve/reject if not cancelled
                if (!"CANCELLED".equalsIgnoreCase(status)) {
                    builder.setPositiveButton("Approve", (d, w) -> doSetStatus(booking, "APPROVED", null))
                           .setNeutralButton("Reject", (d, w) -> showRejectReasonDialog(booking));
                } else {
                    builder.setMessage("Current status: " + status + "\n\nThis booking has been cancelled by the customer. Cannot approve or reject.");
                }
                
                builder.show();
            }

            @Override
            public void onApprove(Booking booking) {
                if (booking == null || booking.BookID == null) return;
                String status = booking.LatestStatus == null ? "PENDING" : booking.LatestStatus.toUpperCase();
                
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    Toast.makeText(BookingApprovalActivity.this,
                            "Cannot approve a cancelled booking",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                doSetStatus(booking, "APPROVED", null);
            }

            @Override
            public void onReject(Booking booking) {
                if (booking == null || booking.BookID == null) return;
                String status = booking.LatestStatus == null ? "PENDING" : booking.LatestStatus.toUpperCase();
                
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    Toast.makeText(BookingApprovalActivity.this,
                            "Cannot reject a cancelled booking",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                showRejectReasonDialog(booking);
            }
        });

        rv.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        BookingService api = ApiClient.getClient(this).create(BookingService.class);

        // ✅ FIX: listAll() has NO parameter now
        api.listAll().enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> res) {

                if (!res.isSuccessful()) {
                    Toast.makeText(BookingApprovalActivity.this,
                            "Load failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Booking> list = res.body();
                if (list == null || list.isEmpty()) {
                    Toast.makeText(BookingApprovalActivity.this,
                            "No bookings",
                            Toast.LENGTH_SHORT).show();
                    adapter.setData(null);
                    return;
                }

                // Clear old status data to ensure fresh fetch
                for (Booking b : list) {
                    if (b != null) b.LatestStatus = null;
                }

                enrichBookingsWithStatusThenShow(list);
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                Toast.makeText(BookingApprovalActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enrichBookingsWithStatusThenShow(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            adapter.setData(null);
            return;
        }

        StatusService statusApi = ApiClient.getClient(this).create(StatusService.class);
        
        // First, try to fetch all statuses at once (more efficient and reliable)
        statusApi.listAll().enqueue(new Callback<List<Status>>() {
            @Override
            public void onResponse(Call<List<Status>> call, Response<List<Status>> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    // Map all statuses by BookID, keeping only the latest for each booking
                    java.util.Map<Integer, Status> latestStatusMap = new java.util.HashMap<>();
                    for (Status s : res.body()) {
                        Integer bookId = (s != null) ? s.getBookId() : null;
                        if (s == null || bookId == null || s.BookingStatus == null) continue;

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
                            android.util.Log.d("BookingApproval", "Booking #" + b.BookID + " status: " + latest.BookingStatus);
                        } else {
                            b.LatestStatus = "PENDING";
                            b.LatestAdminNote = null;
                            android.util.Log.w("BookingApproval", "No status found for Booking #" + b.BookID + ", defaulting to PENDING");
                        }
                    }
                } else {
                    // Fallback: if listAll fails, set all to PENDING and try individual queries
                    android.util.Log.w("BookingApproval", "Failed to fetch all statuses, trying individual queries");
                    enrichBookingsWithStatusIndividual(bookings, statusApi);
                    return;
                }
                adapter.setData(bookings);
            }

            @Override
            public void onFailure(Call<List<Status>> call, Throwable t) {
                android.util.Log.e("BookingApproval", "Failed to fetch all statuses, trying individual queries: " + t.getMessage());
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
                if (b != null && b.LatestStatus == null) b.LatestStatus = "PENDING";
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
                                android.util.Log.d("BookingApproval", "Booking #" + bookId + " status: " + latest.BookingStatus);
                            } else {
                                b.LatestStatus = "PENDING";
                                b.LatestAdminNote = null;
                            }
                        } else {
                            // No status found - default to PENDING
                            if (b.LatestStatus == null) {
                                b.LatestStatus = "PENDING";
                                android.util.Log.w("BookingApproval", "No status found for Booking #" + bookId + ", defaulting to PENDING");
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BookingApproval", "Error processing status for Booking #" + bookId, e);
                        if (b.LatestStatus == null) b.LatestStatus = "PENDING";
                    } finally {
                        done[0]++;
                        if (done[0] == total) adapter.setData(bookings);
                    }
                }

                @Override
                public void onFailure(Call<List<Status>> call, Throwable t) {
                    android.util.Log.e("BookingApproval", "Failed to fetch status for Booking #" + bookId + ": " + t.getMessage());
                    if (b.LatestStatus == null) b.LatestStatus = "PENDING";
                    done[0]++;
                    if (done[0] == total) adapter.setData(bookings);
                }
            });
        }
    }

    private void showRejectReasonDialog(Booking booking) {
        if (booking == null || booking.BookID == null) return;

        String[] reasons = {
            "Slot Unavailable",
            "Capacity Exceeded",
            "Booking Overlap"
        };

        // setItems() displays the list - cannot use setMessage() with setItems()
        new AlertDialog.Builder(this)
                .setTitle("Reject Booking #" + booking.BookID)
                .setItems(reasons, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    doSetStatus(booking, "REJECTED", selectedReason);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doSetStatus(Booking booking, String newStatus) {
        doSetStatus(booking, newStatus, null);
    }

    private void doSetStatus(Booking booking, String newStatus, String adminNote) {
        if (booking == null || booking.BookID == null || booking.BookID <= 0) return;

        StatusService statusApi = ApiClient.getClient(this).create(StatusService.class);
        
        // Use createWithNote if AdminNote is provided (for REJECTED status)
        Call<ResponseBody> call;
        if (adminNote != null && !adminNote.trim().isEmpty()) {
            call = statusApi.createWithNote(newStatus, booking.BookID, adminNote);
        } else {
            call = statusApi.create(newStatus, booking.BookID);
        }
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> res) {
                if (!res.isSuccessful()) {
                    Toast.makeText(BookingApprovalActivity.this,
                            "Update failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String message = "Booking " + newStatus + " successfully";
                if (adminNote != null && !adminNote.trim().isEmpty()) {
                    message += "\nReason: " + adminNote;
                }
                Toast.makeText(BookingApprovalActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // Reload bookings to get fresh status from database
                // Small delay to ensure database has saved
                rv.postDelayed(() -> loadBookings(), 500);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(BookingApprovalActivity.this,
                        "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
