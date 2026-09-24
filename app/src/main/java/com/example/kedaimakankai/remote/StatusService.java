package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.Status;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface StatusService {

    @GET("STATUS")
    Call<List<Status>> listAll();

    @GET("STATUS")
    Call<List<Status>> listByBooking(@Query("BookID") int bookId);

    @FormUrlEncoded
    @POST("STATUS")
    Call<ResponseBody> create(
            @Field("BookingStatus") String bookingStatus,
            @Field("BookID") int bookId
    );

    @FormUrlEncoded
    @POST("STATUS")
    Call<ResponseBody> createWithNote(
            @Field("BookingStatus") String bookingStatus,
            @Field("BookID") int bookId,
            @Field("AdminNote") String adminNote
    );
}
