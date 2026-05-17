package com.example.final_assignment;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class MLKitActivity extends AppCompatActivity {

    private Uri imageFileUri;
    private ImageView imageView;
    private TextView textViewTitle;     // changes: "Get Image from Camera or Gallery" -> reader name
    private TextView textViewSubtitle;  // changes: "Tap OPEN CAMERA..." -> ML Kit results
    private Button buttonEditResults;
    private String readerType;
    private boolean imageProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mlkit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.imageViewMLKit);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubtitle = findViewById(R.id.textViewSubtitle);
        buttonEditResults = findViewById(R.id.buttonEditResults);

        readerType = getIntent().getStringExtra("readerType");
        if (readerType == null) readerType = "Barcode Reader";

        // Initial state: title stays "Get Image from Camera or Gallery",
        // subtitle stays "Tap OPEN CAMERA or LOAD IMAGE..." (set in XML).
        // Show the placeholder reader image (same image clicked on Activity 1).
        if (readerType.equals("Barcode Reader")) {
            imageView.setImageResource(R.drawable.barcode);
        } else if (readerType.equals("Content Reader")) {
            imageView.setImageResource(R.drawable.content);
        } else if (readerType.equals("Text Reader")) {
            imageView.setImageResource(R.drawable.text);
        }

        Button buttonOpenCamera = findViewById(R.id.buttonOpenCamera);
        buttonOpenCamera.setOnClickListener(v -> requestCameraPermissionThenOpen());

        Button buttonLoadImage = findViewById(R.id.buttonLoadImage);
        buttonLoadImage.setOnClickListener(v -> loadImage());

        buttonEditResults.setOnClickListener(v -> {
            // Null safety: only allow editing after an image is processed
            if (imageFileUri == null || !imageProcessed) {
                Toast.makeText(MLKitActivity.this,
                        "Please take or load an image first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MLKitActivity.this, EditActivity.class);
            intent.putExtra("readerType", readerType);
            intent.putExtra("result", textViewSubtitle.getText().toString());
            intent.putExtra("imageUri", imageFileUri.toString());
            // no existingKey -> EditActivity will treat as a NEW item
            startActivity(intent);
        });
    }

    // --- Camera permission: request, then launch on grant ----------------

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            launchCamera();
                        } else {
                            Toast.makeText(MLKitActivity.this,
                                    "Camera permission denied.", Toast.LENGTH_SHORT).show();
                        }
                    });

    private void requestCameraPermissionThenOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageFileUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        if (imageFileUri == null) {
            Toast.makeText(this, "Could not create image file.", Toast.LENGTH_SHORT).show();
            return;
        }
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
        cameraLauncher.launch(takePhotoIntent);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == RESULT_OK && imageFileUri != null) {
                            onImageSelected(imageFileUri);
                        }
                    });

    // --- Modern photo picker (replaces deprecated ACTION_PICK) -----------

    private void loadImage() {
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            imageFileUri = uri;
                            onImageSelected(uri);
                        }
                    });

    // --- Common: image picked, process with ML Kit -----------------------

    private void onImageSelected(Uri uri) {
        imageView.setImageURI(uri);
        // Switch to the "ML Kit results" state
        textViewTitle.setText(readerType);
        textViewSubtitle.setText("");
        buttonEditResults.setVisibility(View.GONE);
        imageProcessed = false;

        InputImage image;
        try {
            image = InputImage.fromFilePath(getBaseContext(), uri);
        } catch (IOException e) {
            textViewSubtitle.setText("Failed to load image.");
            return;
        }
        if (readerType.equals("Barcode Reader")) {
            processImageFromBarcodeReader(image);
        } else if (readerType.equals("Content Reader")) {
            processImageFromContentReader(image);
        } else if (readerType.equals("Text Reader")) {
            processImageFromTextReader(image);
        }
    }

    // --- ML Kit processors -----------------------------------------------

    private void processImageFromBarcodeReader(InputImage image) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    StringBuilder sb = new StringBuilder("Detected barcode:\n");
                    if (barcodes.isEmpty()) {
                        sb.append("Barcode not found.");
                    } else {
                        int i = 1;
                        for (Barcode b : barcodes) {
                            sb.append(i++).append(". ").append(b.getRawValue()).append("\n");
                        }
                    }
                    textViewSubtitle.setText(sb.toString().trim());
                    buttonEditResults.setVisibility(View.VISIBLE);
                    imageProcessed = true;
                })
                .addOnFailureListener(e -> textViewSubtitle.setText("Failed to process image."));
    }

    private void processImageFromContentReader(InputImage image) {
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    StringBuilder sb = new StringBuilder("Recognised image content:\n");
                    int count = 0;
                    for (ImageLabel label : labels) {
                        count++;
                        sb.append(count).append(". ").append(label.getText())
                                .append(" (").append(Math.round(label.getConfidence() * 100))
                                .append("% confidence)\n");
                        if (count >= 3) break;
                    }
                    if (count == 0) sb.append("Nothing recognised.");
                    textViewSubtitle.setText(sb.toString().trim());
                    buttonEditResults.setVisibility(View.VISIBLE);
                    imageProcessed = true;
                })
                .addOnFailureListener(e -> textViewSubtitle.setText("Failed to process image."));
    }

    private void processImageFromTextReader(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener((@NonNull Text visionText) -> {
                    String body = visionText.getText();
                    String out = "Extracted text:\n" + (body.isEmpty() ? "No text found." : body);
                    textViewSubtitle.setText(out);
                    buttonEditResults.setVisibility(View.VISIBLE);
                    imageProcessed = true;
                })
                .addOnFailureListener(e -> textViewSubtitle.setText("Failed to process image."));
    }
}