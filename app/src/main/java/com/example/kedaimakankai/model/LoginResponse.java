package com.example.kedaimakankai.model;

public class LoginResponse {
    public int id;
    public String email;
    public String username;
    public String token;
    public String role;
    public int is_active;

    // extra fields from your API are okay
    public String secret;
    public String lease;
}
