package com.example.final_assignment;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListViewActivity extends AppCompatActivity {

    // Light gray for odd rows, white for even rows
    private static final int COLOR_ODD = Color.parseColor("#EEEEEE");
    private static final int COLOR_EVEN = Color.WHITE;

    private ListView listView;
    private final List<FirebaseItem> items = new ArrayList<>();
    private final List<String> itemKeys = new ArrayList<>();   // parallel list of Firebase keys
    private FirebaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listView = findViewById(R.id.listView);
        adapter = new FirebaseAdapter(this, items);
        listView.setAdapter(adapter);

        // Load items from Firebase
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Storage");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                itemKeys.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FirebaseItem item = child.getValue(FirebaseItem.class);
                    if (item != null) {
                        items.add(item);
                        itemKeys.add(child.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ignore for now
            }
        });

        // Tap an item -> Activity 7 (Item Detail) with key
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FirebaseItem item = items.get(position);
            String key = itemKeys.get(position);
            Intent intent = new Intent(ListViewActivity.this, ItemDetailActivity.class);
            intent.putExtra("firebaseKey", key);
            intent.putExtra("filename", item.getFilename());
            intent.putExtra("reader", item.getReader());
            intent.putExtra("text", item.getText());
            startActivity(intent);
        });

        // Add -> Activity 1
        Button buttonAdd = findViewById(R.id.buttonAdd);
        buttonAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ListViewActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    // Look up image URI from gallery by filename
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

    // Custom adapter with alternating row backgrounds (rubric item 3 in Activity 6)
    class FirebaseAdapter extends ArrayAdapter<FirebaseItem> {
        private final List<FirebaseItem> itemList;

        public FirebaseAdapter(Context context, List<FirebaseItem> objects) {
            super(context, R.layout.list_item, objects);
            itemList = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item, parent, false);
            }
            FirebaseItem item = itemList.get(position);

            // Alternating background: position 0 (first item) is "odd" -> gray
            if (position % 2 == 0) {
                convertView.setBackgroundColor(COLOR_ODD);
            } else {
                convertView.setBackgroundColor(COLOR_EVEN);
            }

            ImageView imageView = convertView.findViewById(R.id.imageViewListItem);
            Uri imageUri = getImageUri(item.getFilename());
            if (imageUri != null) {
                imageView.setImageURI(imageUri);
            } else {
                imageView.setImageDrawable(null);
            }

            TextView textViewReader = convertView.findViewById(R.id.textViewReader);
            textViewReader.setText(item.getReader());

            TextView textViewResult = convertView.findViewById(R.id.textViewResult);
            textViewResult.setText(item.getText());

            return convertView;
        }
    }
}