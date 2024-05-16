package com.epics.speechtonote;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecordedPdfAdapter adapter;
    private final List<File> recordedPdfList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> micVoiceActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        ArrayList<String> resultList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (resultList != null && !resultList.isEmpty()) {
                            String text = resultList.get(0);
                            showSetPdfNameDialog(text);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new RecordedPdfAdapter(this, recordedPdfList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button  startRecordingButton = findViewById(R.id.btn_start_recording);

        Button viewPdfButton = findViewById(R.id.btn_view_pdf);
        Button viewRecordingsButton = findViewById(R.id.btn_view_recordings);

        startRecordingButton.setOnClickListener(v -> {
            startRecordingButton.setVisibility(View.VISIBLE);
            viewPdfButton.setVisibility(View.VISIBLE);
            viewRecordingsButton.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            onMicClick();
        });

        viewPdfButton.setOnClickListener(v -> viewPdf());

        viewRecordingsButton.setOnClickListener(v -> {
            if (recyclerView.getVisibility() == View.VISIBLE) {
                recyclerView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                loadRecordedPdfs();
            }
        });

        Button logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AdminActivity.this, HomeActivity.class));
            finish();
        });

        Button profileButton = findViewById(R.id.buttonProfile);
        profileButton.setOnClickListener(v -> {
            String username = getSessionToken();
            fetchProfileInfo(username);
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    private void onMicClick() {
        try {
            Intent mIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
            micVoiceActivity.launch(mIntent);
        } catch (ActivityNotFoundException ex) {
            Log.d("SpeechToTextTAG", "Speech recognition not available: " + ex.getMessage());
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSetPdfNameDialog(String text) {
        EditText input = new EditText(this);
        input.setText(text);
        new AlertDialog.Builder(this)
                .setTitle("Set PDF Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String pdfName = input.getText().toString();
                    convertToPdf(text, pdfName);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    showAllButtons();
                })
                .setOnDismissListener(dialog -> {
                    showAllButtons();
                })
                .show();
    }
    private void showAllButtons() {
        Button startRecordingButton = findViewById(R.id.btn_start_recording);
        Button viewPdfButton = findViewById(R.id.btn_view_pdf);
        Button viewRecordingsButton = findViewById(R.id.btn_view_recordings);
        Button logoutButton = findViewById(R.id.buttonLogout);

        startRecordingButton.setVisibility(View.VISIBLE);
        viewPdfButton.setVisibility(View.VISIBLE);
        viewRecordingsButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
    }

    private void convertToPdf(String text, String pdfName) {
        try {
            File directory = new File(getExternalFilesDir(null) + "/RecordedPDFs");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, pdfName + ".pdf");

            FileOutputStream outputStream = new FileOutputStream(file);

            PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));

            Document document = new Document(pdf);
            document.add(new Paragraph(text));
            document.close();
            outputStream.close();
            Log.d("SpeechToTextTAG", "PDF generated successfully");

            loadRecordedPdfs();
        } catch (IOException e) {
            Log.e("SpeechToTextTAG", "Error generating PDF: " + e.getMessage());
        }
    }

    private void loadRecordedPdfs() {
        recordedPdfList.clear();
        File directory = new File(getExternalFilesDir(null) + "/RecordedPDFs");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".pdf")) {
                        recordedPdfList.add(file);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void viewPdf() {
        if (!recordedPdfList.isEmpty()) {
            File lastRecordedPdf = recordedPdfList.get(recordedPdfList.size() - 1);
            Log.d("SpeechToTextTAG", "Last recorded PDF: " + lastRecordedPdf.getAbsolutePath());
            openRecordedPdf(lastRecordedPdf);
        } else {
            Toast.makeText(this, "No PDFs available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openRecordedPdf(File file) {
        Uri pdfUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", file);
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(pdfUri, "application/pdf");
        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (pdfIntent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(pdfIntent);
            } catch (ActivityNotFoundException e) {
                Log.e("SpeechToTextTAG", "PDF viewer not found: " + e.getMessage());
                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("SpeechToTextTAG", "No activity found to handle PDF viewing");
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSessionToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("sessionToken", "");
    }

    private void fetchProfileInfo(String username) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://192.168.210.111:5000/profile/" + username)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            public void onResponse(okhttp3.Call call, Response response) {
                try {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        String username = json.getString("username");
                        String email = json.getString("email");
                        String role = json.getString("role");

                        showProfileDialog(username, email, role);
                    } else if (response.code() == 401) {
                        showToast("User not authenticated. Please log in.");
                    } else {
                        String errorMessage = "Error: " + response.code() + " " + response.message();
                        showToast(errorMessage);
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    showToast("Error processing response");
                }
            }
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                showToast("Failed to fetch profile information");
            }
        });
    }
    private void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }
    private void showProfileDialog(String username, String email, String role) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Profile Information");
            builder.setMessage("Username: " + username + "\nEmail: " + email + "\nRole: " + role);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }
}
