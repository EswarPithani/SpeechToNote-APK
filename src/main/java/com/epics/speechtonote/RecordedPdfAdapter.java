package com.epics.speechtonote;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class RecordedPdfAdapter extends RecyclerView.Adapter<RecordedPdfAdapter.PdfViewHolder> {

    private Context context;
    private List<File> pdfList;

    public RecordedPdfAdapter(Context context, List<File> pdfList) {
        this.context = context;
        this.pdfList = pdfList;
    }

    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        File pdfFile = pdfList.get(position);
        holder.fileNameButton.setText(pdfFile.getName());
        holder.fileNameButton.setOnClickListener(v -> openRecordedPdf(pdfFile));
    }

    public int getItemCount() {
        return pdfList.size();
    }
    public class PdfViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        Button fileNameButton;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameButton = itemView.findViewById(R.id.fileNameButton);
            fileNameButton.setOnLongClickListener(this);
        }

        public boolean onLongClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                File file = pdfList.get(position);
                deletePdf(file, position);
                return true;
            }
            return false;
        }
    }
    private void openRecordedPdf(File file) {
        Uri pdfUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider", file);

        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(pdfUri, "application/pdf");
        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooserIntent = Intent.createChooser(pdfIntent, "Open PDF with");

        if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(chooserIntent);
            } catch (ActivityNotFoundException e) {
                Log.e("RecordedPdfAdapter", "PDF viewer not found: " + e.getMessage());
                Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("RecordedPdfAdapter", "No activity found to handle PDF viewing");
            Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private void deletePdf(File file, int position) {
        if (file.delete()) {
            pdfList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, pdfList.size());
            Toast.makeText(context, "PDF file deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to delete PDF file", Toast.LENGTH_SHORT).show();
        }
    }
}
