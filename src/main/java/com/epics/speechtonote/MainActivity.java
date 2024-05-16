package com.epics.speechtonote;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

        private RecyclerView recyclerView;
        private RecordedPdfAdapter adapter;
        private final List<File> recordedPdfList = new ArrayList<>();

        private final ActivityResultLauncher<Intent> micVoiceActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                        if (result.getResultCode() == RESULT_OK) {
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
                setContentView(R.layout.activity_main);

                recyclerView = findViewById(R.id.recyclerView);
                adapter = new RecordedPdfAdapter(this, recordedPdfList);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);

                Button startRecordingButton = findViewById(R.id.btn_start_recording);
                Button stopRecordingButton = findViewById(R.id.btn_stop_recording);
                Button viewPdfButton = findViewById(R.id.btn_view_pdf);
                Button viewRecordingsButton = findViewById(R.id.btn_view_recordings);

                startRecordingButton.setOnClickListener(v -> {
                        startRecordingButton.setVisibility(View.GONE);
                        stopRecordingButton.setVisibility(View.VISIBLE);
                        viewPdfButton.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        onMicClick();
                });

                stopRecordingButton.setOnClickListener(v -> {
                        stopRecording();
                });

                viewPdfButton.setOnClickListener(v -> {
                        if (!recordedPdfList.isEmpty()) {
                                openRecordedPdf(recordedPdfList.get(0));
                        } else {
                                Toast.makeText(this, "No PDFs available", Toast.LENGTH_SHORT).show();
                        }
                });

                viewRecordingsButton.setOnClickListener(v -> {
                        recyclerView.setVisibility(View.VISIBLE);
                        loadRecordedPdfs();
                });
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
                                adapter.notifyDataSetChanged(); // Notify adapter after updating the list
                        }
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

        private void stopRecording() {
                Button startRecordingButton = findViewById(R.id.btn_start_recording);
                Button stopRecordingButton = findViewById(R.id.btn_stop_recording);
                Button viewPdfButton = findViewById(R.id.btn_view_pdf);
                Button viewRecordingsButton = findViewById(R.id.btn_view_recordings);

                startRecordingButton.setVisibility(View.VISIBLE);
                stopRecordingButton.setVisibility(View.GONE);
                viewPdfButton.setVisibility(View.VISIBLE);
                viewRecordingsButton.setVisibility(View.VISIBLE);

                Log.d("SpeechToTextTAG", "Recording stopped");
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
                                // Cancelled, do nothing
                        })
                        .show();
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
        
}
