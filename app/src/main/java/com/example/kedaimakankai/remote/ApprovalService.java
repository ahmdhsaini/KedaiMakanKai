package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.Booking;
import com.example.kedaimakankai.model.SimpleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApprovalService {

    @GET("bookings/list_pending")
    Call<List<Booking>> listPending();

    @POST("bookings/approve")
    Call<SimpleResponse> approve(@Query("BookingID") int bookingId);

    @POST("bookings/reject")
    Call<SimpleResponse> reject(@Query("BookingID") int bookingId);
}
