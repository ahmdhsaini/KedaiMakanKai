package com.example.kedaimakankai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kedaimakankai.model.Customer;
import com.example.kedaimakankai.model.LoginResponse;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.AuthService;
import com.example.kedaimakankai.remote.CustomerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);     // ✅ email only
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String email = val(etEmail);
        String password = val(etPassword);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService api = ApiClient.getClient(this).create(AuthService.class);

        api.login(email, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> res) {

                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(LoginActivity.this,
                            "Login failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse body = res.body();

                if (body.token == null || body.token.trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "Login failed: token missing",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SessionManager session = new SessionManager(LoginActivity.this);
                session.saveLogin(
                        body.token,
                        body.role,
                        body.id,
                        body.username,
                        body.email
                );

                // If customer role, fetch and save customer ID
                if (!"admin".equalsIgnoreCase(body.role) && !"superadmin".equalsIgnoreCase(body.role)) {
                    fetchAndSaveCustomerId(session, body.email);
                } else {
                    // Admin - go directly to dashboard
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAndSaveCustomerId(SessionManager session, String email) {
        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);
        
        customerApi.listAll().enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(Call<List<Customer>> call, Response<List<Customer>> res) {
                int custId = -1;
                
                if (res.isSuccessful() && res.body() != null) {
                    List<Customer> customers = res.body();
                    for (Customer c : customers) {
                        if (c != null) {
                            // Method 1: Match by UserID.id (most reliable - direct foreign key)
                            int loggedInUserId = session.getUserId();
                            if (loggedInUserId > 0 && c.UserID != null && c.UserID.id == loggedInUserId) {
                                custId = (c.CustID != null) ? c.CustID : -1;
                                break;
                            }
                            
                            // Method 2: Try to match by AccID.AccEmail
                            if (custId <= 0 && c.AccID != null && c.AccID.AccEmail != null && 
                                email.equalsIgnoreCase(c.AccID.AccEmail)) {
                                custId = (c.CustID != null) ? c.CustID : -1;
                                break;
                            }
                            
                            // Method 3: Fallback: try UserID.email
                            if (custId <= 0 && c.UserID != null && c.UserID.email != null && 
                                email.equalsIgnoreCase(c.UserID.email)) {
                                custId = (c.CustID != null) ? c.CustID : -1;
                                break;
                            }
                        }
                    }
                }
                
                if (custId > 0) {
                    session.saveCustomerId(custId);
                }
                
                // Navigate to customer dashboard regardless
                startActivity(new Intent(LoginActivity.this, CustomerDashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<List<Customer>> call, Throwable t) {
                // Still navigate - customer ID will be fetched when making booking
                Toast.makeText(LoginActivity.this, 
                        "Warning: Could not fetch customer ID. You may need to create a booking first.", 
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(LoginActivity.this, CustomerDashboardActivity.class));
                finish();
            }
        });
    }

    private String val(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
