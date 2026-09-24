package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class Booking {

    @SerializedName("BookID")
    public Integer BookID;

    @SerializedName("TimeSlot")
    public String TimeSlot;

    @SerializedName("Capacity")
    public Integer Capacity;

    // API returns CustID as OBJECT (not int)
    @SerializedName("CustID")
    public Customer CustID;

    // API returns AdminID as OBJECT or null
    @SerializedName("AdminID")
    public Admin AdminID;

    /**
     * UI helper (not from API): latest status for this booking.
     * Values: PENDING / APPROVED / REJECTED / CANCELLED
     */
    public transient String LatestStatus;

    /**
     * UI helper (not from API): admin note/rejection reason for this booking.
     * Only populated when status is REJECTED.
     */
    public transient String LatestAdminNote;
}
