package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.Customer;
import com.example.kedaimakankai.model.SimpleResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface CustomerService {

    /**
     * GET all customers.
     * The API sometimes responds with arrays, single objects, or wrapped payloads,
     * so we expose both a typed and a raw variant. The raw call is used in places
     * where we need to manually normalize unexpected shapes.
     */
    @GET("CUSTOMER")
    Call<List<Customer>> listAll();

    @GET("CUSTOMER")
    Call<ResponseBody> listAllRaw();

    // Try multiple possible endpoints for creating customer
    @FormUrlEncoded
    @POST("CUSTOMER")
    Call<SimpleResponse> createCustomer(
            @Field("UserID") int userId,
            @Field("CustName") String custName,
            @Field("CustPhone") String custPhone
    );
    
    // Try with different field name (camelCase)
    @FormUrlEncoded
    @POST("CUSTOMER")
    Call<SimpleResponse> createCustomerCamelCase(
            @Field("userId") int userId,
            @Field("custName") String custName,
            @Field("custPhone") String custPhone
    );
    
    @FormUrlEncoded
    @POST("customers")
    Call<SimpleResponse> createCustomerAlt(
            @Field("UserID") int userId,
            @Field("CustName") String custName,
            @Field("CustPhone") String custPhone
    );
    
    // Alternative: if API expects JSON body with object
    @POST("CUSTOMER")
    Call<SimpleResponse> createCustomerJson(@Body com.example.kedaimakankai.model.CustomerCreateRequest request);
}
