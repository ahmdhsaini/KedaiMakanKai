package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class BookingCreateRequest {

    @SerializedName("TimeSlot")
    public String timeSlot;

    @SerializedName("Capacity")
    public int capacity;

    @SerializedName("CustID")
    public int custId;


    public BookingCreateRequest(String timeSlot, int capacity, int custId) {
        this.timeSlot = timeSlot;
        this.capacity = capacity;
        this.custId = custId;
    }
}
