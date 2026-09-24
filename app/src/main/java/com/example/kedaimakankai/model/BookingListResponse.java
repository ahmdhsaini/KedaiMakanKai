package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookingListResponse {

    @SerializedName("success")
    public Boolean success;

    @SerializedName("message")
    public String message;

    // Common wrapper names used by APIs
    @SerializedName("data")
    public List<Booking> data;

    @SerializedName("bookings")
    public List<Booking> bookings;

    @SerializedName("result")
    public List<Booking> result;

    public List<Booking> getList() {
        if (data != null) return data;
        if (bookings != null) return bookings;
        return result;
    }
}
