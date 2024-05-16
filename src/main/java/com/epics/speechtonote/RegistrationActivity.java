package com.epics.speechtonote;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.safetynet.SafetyNet;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationActivity extends AppCompatActivity{

    private EditText editTextEmail, editTextFullName, editTextUsername, editTextPassword1, editTextPassword2;
    private Spinner spinnerRole;
    private RequestQueue requestQueue;

    CheckBox GoogleCaptcha;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword1 = findViewById(R.id.editTextPassword1);
        editTextPassword2 = findViewById(R.id.editTextPassword2);
        spinnerRole = findViewById(R.id.spinnerRole);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        Button buttonHome = findViewById(R.id.buttonHome);
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        GoogleCaptcha = findViewById(R.id.GoogleCaptcha);

        GoogleCaptcha.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                verifyCaptcha();
            } else {
                GoogleCaptcha.setText("Verify CAPTCHA");
            }
        });

        buttonRegister.setOnClickListener(v -> registerUser());

        requestQueue = Volley.newRequestQueue(this);
    }
    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String fullName = editTextFullName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password1 = editTextPassword1.getText().toString().trim();
        String password2 = editTextPassword2.getText().toString().trim();

        String role = spinnerRole.getSelectedItem().toString();

        String emailPattern = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:student\\.nitandhra\\.ac\\.in|gmail\\.com)$";

        if (TextUtils.isEmpty(password1) || !isPasswordValid(password1)) {
            Toast.makeText(this, "Password must be Alphanumeric and at least 6 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password2)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.matches(emailPattern)) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password1.equals(password2)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("email", email);
            jsonObject.put("password", password1);
            jsonObject.put("role", role);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://192.168.210.111:5000/register";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        String responseData = response.getString("message");
                        if(responseData.equals("Username already exists")) {
                            Toast.makeText(RegistrationActivity.this, responseData, Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(RegistrationActivity.this, responseData, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                        finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(RegistrationActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(RegistrationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        requestQueue.add(request);
    }
    private boolean isPasswordValid(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$";
        return password.matches(passwordPattern);
    }
    public void verifyCaptcha() {
        SafetyNet.getClient(RegistrationActivity.this)
                .verifyWithRecaptcha("6LfOxMwpAAAAAGD06ZH-PSj5oKt0p6S71hexXxdH")
                .addOnSuccessListener(this, recaptchaTokenResponse -> {
                    String userResponseToken = recaptchaTokenResponse.getTokenResult();
                    if (!TextUtils.isEmpty(userResponseToken)) {
                        GoogleCaptcha.setText("You're not a Robot");
                    } else {
                        Toast.makeText(RegistrationActivity.this, "reCAPTCHA token is empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "reCAPTCHA verification failed: " + e.getMessage());
                    Toast.makeText(RegistrationActivity.this, "reCAPTCHA verification failed", Toast.LENGTH_SHORT).show();
                });
    }
}
