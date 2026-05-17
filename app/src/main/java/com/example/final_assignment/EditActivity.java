package com.example.final_assignment;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

public class EditActivity extends AppCompatActivity {

    private ImageView imageViewEdit;
    private EditText editTextResult;
    private EditText editTextReaderType;

    private String readerType;
    private String imageUriString;
    private Uri imageFileUri;
    private String existingKey;   // null = new item, non-null = update existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewEdit = findViewById(R.id.imageViewEdit);
        editTextResult = findViewById(R.id.editTextResult);
        editTextReaderType = findViewById(R.id.editTextReaderType);

        readerType = getIntent().getStringExtra("readerType");
        String result = getIntent().getStringExtra("result");
        imageUriString = getIntent().getStringExtra("imageUri");
        existingKey = getIntent().getStringExtra("existingKey");

        if (readerType != null) editTextReaderType.setText(readerType);
        if (result != null) editTextResult.setText(result);

        if (imageUriString != null) {
            imageFileUri = Uri.parse(imageUriString);
            imageViewEdit.setImageURI(imageFileUri);
        }

        Button buttonSave = findViewById(R.id.buttonSave);
        buttonSave.setOnClickListener(v -> {
            String updatedReader = editTextReaderType.getText().toString();
            String updatedResult = editTextResult.getText().toString();

            DatabaseReference dbRef =
                    FirebaseDatabase.getInstance().getReference("Storage");

            if (existingKey != null) {
                // Coming from Activity 7 -> update EXISTING item (text only)
                DatabaseReference itemRef = dbRef.child(existingKey);
                itemRef.child("reader").setValue(updatedReader);
                itemRef.child("text").setValue(updatedResult);
                openListView();
            } else {
                // Coming from Activity 2 -> NEW item, save image too
                if (imageFileUri == null) {
                    Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap bitmap = getBitmapFromUri(imageFileUri);
                if (bitmap == null) {
                    Toast.makeText(this, "Could not read image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String filename = LocalDateTime.now().toString().replaceAll("\\D+", "");
                boolean saved = saveImageToGallery(bitmap, filename, EditActivity.this);
                if (!saved) {
                    Toast.makeText(this, "Could not save image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String key = dbRef.push().getKey();
                if (key == null) {
                    Toast.makeText(this, "Could not save record.", Toast.LENGTH_SHORT).show();
                    return;
                }
                DatabaseReference itemRef = dbRef.child(key);
                itemRef.child("filename").setValue(filename);
                itemRef.child("reader").setValue(updatedReader);
                itemRef.child("text").setValue(updatedResult);
                openListView();
            }
        });
    }

    private void openListView() {
        Intent intent = new Intent(EditActivity.this, ListViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } catch (IOException e) {
            Log.e("EditActivity", "Failed to load image", e);
            return null;
        }
    }

    private boolean saveImageToGallery(Bitmap bitmap, String fileName, Context context) {
        if (bitmap == null) return false;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        Uri imageUri = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri == null) return false;
        try (OutputStream outputStream =
                     context.getContentResolver().openOutputStream(imageUri)) {
            if (outputStream == null) return false;
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            Log.d("EditActivity", "Image saved: " + imageUri);
            return true;
        } catch (IOException e) {
            Log.e("EditActivity", "Error saving image", e);
            return false;
        }
    }
}