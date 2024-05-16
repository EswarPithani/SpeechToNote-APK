package com.epics.speechtonote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.safetynet.SafetyNet;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText editTextUsername, editTextPassword;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private CheckBox GoogleCaptcha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        CheckBox checkBoxShowPassword = findViewById(R.id.checkBoxShowPassword);
        GoogleCaptcha = findViewById(R.id.GoogleCaptcha);

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);

        Button buttonHome = findViewById(R.id.buttonHome);
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
            finish();
        });

        GoogleCaptcha.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Catcha();
            } else {
                GoogleCaptcha.setText("Verify CAPTCHA");
            }
        });

        buttonLogin.setOnClickListener(v -> {
            if (GoogleCaptcha.isChecked()) {
                loginUser();
            } else {
                Toast.makeText(this, "Please verify CAPTCHA", Toast.LENGTH_SHORT).show();
            }
        });

        checkBoxShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editTextPassword.setTransformationMethod(null);
            } else {
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });
    }
    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Username and password are required", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "http://192.168.210.111:5000/login";
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("username", username);
            requestData.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestData,
                response -> {
                    try {
                        String message = response.getString("message");
                        if ("Login successful".equals(message)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("username", username);
                            editor.apply();
                            Log.d(TAG, "Stored username: " + username);
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            if (response.has("role")) {
                                String role = response.getString("role");
                                Log.d(TAG, "Retrieved role: " + role);
                                openRoleActivity(role);
                            } else {
                                Toast.makeText(LoginActivity.this, "Role not found in response", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode == 401) {
                            Toast.makeText(LoginActivity.this, "Incorrect Username or Password", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = new String(error.networkResponse.data);
                            Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: Network issue", Toast.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(request);
    }
    private void openRoleActivity(String role) {
        Log.d(TAG, "Retrieved role: " + role);
        Intent intent;
        switch (role) {
            case "admin":
                Log.d(TAG, "Opening AdminActivity");
                intent = new Intent(LoginActivity.this, AdminActivity.class);
                startActivity(intent);
                break;
            case "teacher":
                Log.d(TAG, "Opening TeacherActivity");
                intent = new Intent(LoginActivity.this, TeacherActivity.class);
                startActivity(intent);
                break;
            case "student":
                Log.d(TAG, "Opening StudentActivity");
                intent = new Intent(LoginActivity.this, StudentActivity.class);
                startActivity(intent);
                break;
            default:
                Log.d(TAG, "Unknown role: " + role);
                Toast.makeText(LoginActivity.this, "Unrecognized role: " + role, Toast.LENGTH_SHORT).show();
                intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                break;
        }
        finish();
    }
    public void Catcha() {
        SafetyNet.getClient(LoginActivity.this)
                .verifyWithRecaptcha("6LfOxMwpAAAAAGD06ZH-PSj5oKt0p6S71hexXxdH")
                .addOnSuccessListener(this, recaptchaTokenResponse -> {
                    String userResponseToken = recaptchaTokenResponse.getTokenResult();
                    if (!TextUtils.isEmpty(userResponseToken)) {
                        GoogleCaptcha.setText("You're not a Robot");
                    } else {
                        Toast.makeText(LoginActivity.this, "reCAPTCHA token is empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "reCAPTCHA verification failed: " + e.getMessage());
                    Toast.makeText(LoginActivity.this, "reCAPTCHA verification failed", Toast.LENGTH_SHORT).show();
                });
    }
}