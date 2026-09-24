package com.example.kedaimakankai;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "kai_session";

    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CUST_ID = "cust_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String token, String role, int userId, String username, String email) {
        prefs.edit()
                .putString(KEY_TOKEN, token == null ? "" : token)
                .putString(KEY_ROLE, role == null ? "" : role)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username == null ? "" : username)
                .putString(KEY_EMAIL, email == null ? "" : email)
                .apply();
    }

    public void saveCustomerId(int custId) {
        prefs.edit().putInt(KEY_CUST_ID, custId).apply();
    }

    public int getCustomerId() {
        return prefs.getInt(KEY_CUST_ID, -1);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
