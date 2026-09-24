package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.Booking;
import com.example.kedaimakankai.model.SimpleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BookingService {

    // ✅ ADMIN view all
    @GET("BOOKING")
    Call<List<Booking>> listAll();

    // ✅ CUSTOMER view own
    @GET("BOOKING")
    Call<List<Booking>> listByCustomer(@Query("CustID") int custId);

    // ✅ CUSTOMER create booking (FORM, not JSON)
    @FormUrlEncoded
    @POST("BOOKING")
    Call<SimpleResponse> create(
            @Field("TimeSlot") String timeSlot,
            @Field("Capacity") int capacity,
            @Field("CustID") int custId
    );
}
