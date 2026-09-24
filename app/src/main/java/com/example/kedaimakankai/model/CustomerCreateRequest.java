package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class CustomerCreateRequest {

    @SerializedName("UserID")
    public int userId;

    @SerializedName("CustName")
    public String custName;

    @SerializedName("CustPhone")
    public String custPhone;

    public CustomerCreateRequest(int userId, String custName, String custPhone) {
        this.userId = userId;
        this.custName = custName;
        this.custPhone = custPhone;
    }
}
