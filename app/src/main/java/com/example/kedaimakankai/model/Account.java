package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class Account {

    @SerializedName("AccID")
    public Integer AccID;

    @SerializedName("AccEmail")
    public String AccEmail;

    @SerializedName("AccRole")
    public String AccRole;
}
