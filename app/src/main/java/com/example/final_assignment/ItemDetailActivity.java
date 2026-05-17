package com.example.final_assignment;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ItemDetailActivity extends AppCompatActivity {

    private String firebaseKey;
    private String filename;
    private String reader;
    private String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseKey = getIntent().getStringExtra("firebaseKey");
        filename = getIntent().getStringExtra("filename");
        reader = getIntent().getStringExtra("reader");
        text = getIntent().getStringExtra("text");

        TextView textViewReaderType = findViewById(R.id.textViewReaderType);
        textViewReaderType.setText(reader);

        TextView textViewResult = findViewById(R.id.textViewResult);
        textViewResult.setText(text);

        ImageView imageViewDetail = findViewById(R.id.imageViewDetail);
        Uri imageUri = getImageUri(filename);
        if (imageUri != null) {
            imageViewDetail.setImageURI(imageUri);
        }

        // Edit -> Activity 5, pass existingKey so EditActivity will UPDATE not insert
        Button buttonEdit = findViewById(R.id.buttonEdit);
        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(ItemDetailActivity.this, EditActivity.class);
            intent.putExtra("readerType", reader);
            intent.putExtra("result", text);
            intent.putExtra("existingKey", firebaseKey);
            Uri uri = getImageUri(filename);
            if (uri != null) intent.putExtra("imageUri", uri.toString());
            startActivity(intent);
        });

        // Delete -> remove from Firebase, then back to Activity 6 (updated list)
        Button buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(v -> {
            if (firebaseKey == null) {
                Toast.makeText(this, "Cannot delete: missing key.", Toast.LENGTH_SHORT).show();
                return;
            }
            DatabaseReference dbRef =
                    FirebaseDatabase.getInstance().getReference("Storage").child(firebaseKey);
            dbRef.removeValue().addOnCompleteListener(task -> {
                Intent intent = new Intent(ItemDetailActivity.this, ListViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        });

        // Cancel -> back to Activity 6, no changes
        Button buttonCancel = findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(v -> {
            Intent intent = new Intent(ItemDetailActivity.this, ListViewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private Uri getImageUri(String filename) {
        if (filename == null) return null;
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME};
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
        String[] selectionArgs = {filename + ".png"};
        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(idColumn);
                return ContentUris.withAppendedId(uri, imageId);
            }
        }
        return null;
    }
}