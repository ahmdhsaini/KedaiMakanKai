package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    public int id;

    // Some endpoints return `name`, others return `username`
    @SerializedName(value = "name", alternate = {"username"})
    public String name;

    @SerializedName("email")
    public String email;

    @SerializedName("role")
    public String role; // "customer" / "admin" / "user"

    @SerializedName(value = "api_key", alternate = {"token"})
    public String api_key; // if API returns it here
}
