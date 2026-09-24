package com.example.kedaimakankai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private String selectedRole = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RadioGroup rg = findViewById(R.id.rgRole);
        RadioButton rbCustomer = findViewById(R.id.rbCustomer);
        RadioButton rbAdmin = findViewById(R.id.rbAdmin);

        MaterialButton btnGoLogin = findViewById(R.id.btnGoLogin);
        MaterialButton btnGoRegister = findViewById(R.id.btnGoRegister);

        // default checked in XML = customer
        selectedRole = rbCustomer.isChecked() ? "customer" : "admin";

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAdmin) selectedRole = "admin";
            else selectedRole = "customer";
        });

        btnGoLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("role", selectedRole);
            startActivity(i);
        });

        btnGoRegister.setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterActivity.class);
            startActivity(i);
        });
    }
}
