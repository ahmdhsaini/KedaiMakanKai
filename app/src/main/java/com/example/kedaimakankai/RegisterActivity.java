package com.example.kedaimakankai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kedaimakankai.model.Customer;
import com.example.kedaimakankai.model.SimpleResponse;
import com.example.kedaimakankai.remote.ApiClient;
import com.example.kedaimakankai.remote.AuthService;
import com.example.kedaimakankai.remote.CustomerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    
    private TextInputEditText etEmail, etUsername, etPassword, etCustName, etCustPhone;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegEmail);
        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        etCustName = findViewById(R.id.etRegCustName);
        etCustPhone = findViewById(R.id.etRegCustPhone);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String email = getText(etEmail);
        String username = getText(etUsername);
        String password = getText(etPassword);
        String custName = getText(etCustName);
        String custPhone = getText(etCustPhone);

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || custName.isEmpty() || custPhone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService api = ApiClient.getClient(this).create(AuthService.class);

        // Register the user in the "users" table
        api.register(email, username, password).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(RegisterActivity.this,
                            "Register failed (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(RegisterActivity.this,
                        res.body().message,
                        Toast.LENGTH_SHORT).show();

                if (res.body().success) {
                    int userId = res.body().userId;
                    if (userId > 0) {
                        // Successfully registered, now create customer record in CUSTOMER table
                        // Use the actual name and phone from registration form
                        android.util.Log.d(TAG, "✅ User registered with ID: " + userId);
                        createCustomerRecord(userId, custName, custPhone);
                    } else {
                        android.util.Log.w("RegisterActivity", "Registration successful but userId is invalid: " + userId);
                        Toast.makeText(RegisterActivity.this, 
                                "Registration successful, but user ID not returned. Please contact support.", 
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, 
                            "Registration failed: " + (res.body().message != null ? res.body().message : "Unknown error"), 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createCustomerRecord(int userId, String custName, String custPhone) {
        // Create customer record in the CUSTOMER table with the UserID
        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);
        
        android.util.Log.d(TAG, "═══════════════════════════════════════════════════════");
        android.util.Log.d(TAG, "🔄 Attempting to create customer record");
        android.util.Log.d(TAG, "   UserID: " + userId);
        android.util.Log.d(TAG, "   CustName: " + custName);
        android.util.Log.d(TAG, "   CustPhone: " + custPhone);
        android.util.Log.d(TAG, "═══════════════════════════════════════════════════════");
        
        // Try primary endpoint first (CUSTOMER) with form-urlencoded
        android.util.Log.d(TAG, "📤 Sending POST /CUSTOMER (form-urlencoded)");
        
        customerApi.createCustomer(userId, custName, custPhone).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {
                android.util.Log.d(TAG, "📥 Response received - HTTP: " + res.code());
                android.util.Log.d(TAG, "   Success: " + (res.body() != null && res.body().success));
                android.util.Log.d(TAG, "   Message: " + (res.body() != null ? res.body().message : "null"));
                
                // Log raw response for debugging
                if (!res.isSuccessful()) {
                    try {
                        String errorBody = res.errorBody() != null ? res.errorBody().string() : "null";
                        android.util.Log.e(TAG, "❌ Error response body: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "❌ Could not read error body: " + e.getMessage());
                    }
                }
                
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    android.util.Log.d(TAG, "✅ Customer record created successfully!");
                    // Verify the customer was actually created by checking the database
                    verifyCustomerCreated(userId, custName, custPhone);
                } else {
                    // Log the error response body for debugging
                    if (res.body() != null) {
                        android.util.Log.e("RegisterActivity", "Customer creation failed - Message: " + res.body().message + 
                                ", Success flag: " + res.body().success);
                    } else {
                        android.util.Log.e("RegisterActivity", "Customer creation failed - Response body is null, HTTP: " + res.code());
                    }
                    // Try camelCase field name
                    tryCamelCaseCustomerCreation(customerApi, userId, custName, custPhone);
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                android.util.Log.e(TAG, "❌ Network error: " + t.getMessage());
                android.util.Log.e(TAG, "   Request URL: " + call.request().url());
                android.util.Log.e(TAG, "   Request method: " + call.request().method());
                if (t.getCause() != null) {
                    android.util.Log.e(TAG, "   Cause: " + t.getCause().getMessage());
                }
                // Try JSON body format
                tryJsonCustomerCreation(customerApi, userId, custName, custPhone);
            }
        });
    }
    
    private void tryCamelCaseCustomerCreation(CustomerService customerApi, int userId, String custName, String custPhone) {
        android.util.Log.d("RegisterActivity", "Trying camelCase field name (userId) for customer creation");
        customerApi.createCustomerCamelCase(userId, custName, custPhone).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {
                android.util.Log.d("RegisterActivity", "CamelCase Customer creation response - HTTP: " + res.code() + 
                        ", Success: " + (res.body() != null && res.body().success));
                
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    android.util.Log.d(TAG, "✅ Customer record created successfully with camelCase!");
                    verifyCustomerCreated(userId, custName, custPhone);
                } else {
                    // Try JSON body format
                    tryJsonCustomerCreation(customerApi, userId, custName, custPhone);
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                android.util.Log.e("RegisterActivity", "CamelCase Customer creation failed: " + t.getMessage());
                // Try JSON body format
                tryJsonCustomerCreation(customerApi, userId, custName, custPhone);
            }
        });
    }
    
    private void tryJsonCustomerCreation(CustomerService customerApi, int userId, String custName, String custPhone) {
        android.util.Log.d("RegisterActivity", "Trying JSON body format for customer creation");
        // Try JSON body format with proper request object
        com.example.kedaimakankai.model.CustomerCreateRequest request = 
                new com.example.kedaimakankai.model.CustomerCreateRequest(userId, custName, custPhone);
        customerApi.createCustomerJson(request).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {
                android.util.Log.d("RegisterActivity", "JSON Customer creation response - HTTP: " + res.code() + 
                        ", Success: " + (res.body() != null && res.body().success));
                
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    android.util.Log.d(TAG, "✅ Customer record created successfully with JSON!");
                    verifyCustomerCreated(userId, custName, custPhone);
                } else {
                    // Try alternative endpoint
                    tryAlternativeCustomerCreation(customerApi, userId, custName, custPhone);
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                android.util.Log.e("RegisterActivity", "JSON Customer creation failed: " + t.getMessage());
                // Try alternative endpoint
                tryAlternativeCustomerCreation(customerApi, userId, custName, custPhone);
            }
        });
    }
    
    private void tryAlternativeCustomerCreation(CustomerService customerApi, int userId, String custName, String custPhone) {
        android.util.Log.d("RegisterActivity", "Trying alternative endpoint (customers)");
        // Try alternative endpoint (customers)
        customerApi.createCustomerAlt(userId, custName, custPhone).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> res) {
                android.util.Log.d("RegisterActivity", "Alt Customer creation response - HTTP: " + res.code() + 
                        ", Success: " + (res.body() != null && res.body().success));
                
                if (res.isSuccessful() && res.body() != null && res.body().success) {
                    android.util.Log.d(TAG, "✅ Customer record created successfully with alt endpoint!");
                    verifyCustomerCreated(userId, custName, custPhone);
                } else {
                    String errorMsg = (res.body() != null && res.body().message != null) 
                            ? res.body().message 
                            : "Failed to create customer record (HTTP " + res.code() + ")";
                    
                    // Try to read error body
                    try {
                        if (res.errorBody() != null) {
                            String errorBody = res.errorBody().string();
                            android.util.Log.e("RegisterActivity", "Error response body: " + errorBody);
                            errorMsg += "\nError: " + errorBody;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RegisterActivity", "Could not read error body: " + e.getMessage());
                    }
                    
                    android.util.Log.e("RegisterActivity", "All customer creation attempts failed. Last error: " + errorMsg);
                    Toast.makeText(RegisterActivity.this, 
                            "Registration complete, but customer record creation failed.\n" +
                            "HTTP " + res.code() + ": " + errorMsg + 
                            "\n\nPlease check Logcat for details. You may need to contact support or the backend API may need to be updated to allow customer creation during registration.", 
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                android.util.Log.e("RegisterActivity", "All customer creation attempts failed. Last error: " + t.getMessage(), t);
                android.util.Log.e("RegisterActivity", "Request that failed: " + call.request().url());
                Toast.makeText(RegisterActivity.this, 
                        "Registration complete, but customer record creation failed: " + t.getMessage() + 
                        "\n\nPlease check Logcat for details. The backend API might require authentication or the endpoint might not be configured correctly.", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Verify that the customer record was actually created in the database.
     * Sometimes the API returns success but doesn't actually save the record.
     */
    private void verifyCustomerCreated(int userId, String custName, String custPhone) {
        android.util.Log.d(TAG, "🔍 Verifying customer record was created...");
        CustomerService customerApi = ApiClient.getClient(this).create(CustomerService.class);
        
        customerApi.listAll().enqueue(new Callback<List<Customer>>() {
            @Override
            public void onResponse(Call<List<Customer>> call, 
                                 Response<List<Customer>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    boolean found = false;
                    for (Customer c : res.body()) {
                        if (c != null && c.UserID != null && c.UserID.id == userId) {
                            found = true;
                            android.util.Log.d(TAG, "✅ Verification SUCCESS: Customer record found with CustID=" + c.CustID);
                            Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    
                    if (!found) {
                        android.util.Log.e(TAG, "❌ Verification FAILED: Customer record NOT found in database!");
                        android.util.Log.e(TAG, "   The API returned success, but the record doesn't exist.");
                        android.util.Log.e(TAG, "   This suggests the backend API is not actually saving to the database.");
                        Toast.makeText(RegisterActivity.this, 
                                "⚠️ Registration completed, but customer profile creation failed.\n" +
                                "The backend API may not be saving the customer record.\n" +
                                "Please contact support or check backend logs.", 
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    android.util.Log.w(TAG, "⚠️ Could not verify customer creation (HTTP " + res.code() + ")");
                    // Assume success if we can't verify
                    Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<Customer>> call, Throwable t) {
                android.util.Log.w(TAG, "⚠️ Could not verify customer creation: " + t.getMessage());
                // Assume success if we can't verify
                Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private String getText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
