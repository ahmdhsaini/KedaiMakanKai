package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.LoginResponse;
import com.example.kedaimakankai.model.SimpleResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface AuthService {

    // ✅ LOGIN (EMAIL ONLY)
    @FormUrlEncoded
    @POST("users/login")
    Call<LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );

    // ✅ REGISTER (email + username + password)
    @FormUrlEncoded
    @POST("users/register")
    Call<SimpleResponse> register(
            @Field("email") String email,
            @Field("username") String username,
            @Field("password") String password
    );
}
