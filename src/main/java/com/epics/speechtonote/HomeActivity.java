package com.epics.speechtonote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences("user_login", MODE_PRIVATE);

        if (isLoggedIn()) {
            String role = sharedPreferences.getString("user_role", null);
            openRoleActivity(role);
            finish();
            return;
        }

        Button buttonAbout = findViewById(R.id.buttonAbout);
        buttonAbout.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        Button buttonRegister = findViewById(R.id.buttonRegister);
        Button buttonLogin = findViewById(R.id.buttonLogin);

        buttonRegister.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, RegistrationActivity.class)));

        buttonLogin.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, LoginActivity.class)));
    }

    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("is_logged_in", false);
    }
    private void openRoleActivity(String role) {
        Intent intent;
        switch (role) {
            case "admin":
                intent = new Intent(HomeActivity.this, AdminActivity.class);
                break;
            case "teacher":
                intent = new Intent(HomeActivity.this, StudentActivity.class);
                break;
            case "student":
                intent = new Intent(HomeActivity.this, StudentActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }
}
