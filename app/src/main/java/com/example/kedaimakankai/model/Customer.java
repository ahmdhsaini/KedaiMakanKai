package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class Customer {

    @SerializedName(value = "CustID", alternate = {"CustId", "cust_id", "custid"})
    public Integer CustID;

    // NOTE: Do not repeat the primary name inside `alternate` (Gson will treat it as a duplicate field).
    @SerializedName(value = "CustName", alternate = {"cust_name", "custname"})
    public String CustName;

    @SerializedName(value = "CustPhone", alternate = {"cust_phone", "custphone"})
    public String CustPhone;

    // API returns nested user object in `UserID` (e.g. { "id": 11, "email": "...", ... })
    @SerializedName(value = "UserID", alternate = {"UserId", "user_id", "userid"})
    public User UserID;

    // ✅ API returns nested Account object here
    @SerializedName(value = "AccID", alternate = {"AccId", "acc_id", "accid"})
    public Account AccID;
}
