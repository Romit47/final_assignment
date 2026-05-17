package com.example.final_assignment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Barcode Reader image -> Activity 2
        ImageButton imageButtonBarcode = findViewById(R.id.imageButtonBarcode);
        imageButtonBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMLKit("Barcode Reader");
            }
        });

        // Content Reader image -> Activity 2
        ImageButton imageButtonContent = findViewById(R.id.imageButtonContent);
        imageButtonContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMLKit("Content Reader");
            }
        });

        // Text Reader image -> Activity 2
        ImageButton imageButtonText = findViewById(R.id.imageButtonText);
        imageButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMLKit("Text Reader");
            }
        });

        // List of Analysed Images -> Activity 6
        Button buttonListAnalysed = findViewById(R.id.buttonListAnalysed);
        buttonListAnalysed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ListViewActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openMLKit(String readerType) {
        Intent intent = new Intent(MainActivity.this, MLKitActivity.class);
        intent.putExtra("readerType", readerType);
        startActivity(intent);
    }
}