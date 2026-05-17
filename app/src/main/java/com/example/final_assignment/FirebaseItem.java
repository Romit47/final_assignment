package com.example.final_assignment;

public class FirebaseItem {
    private String filename;
    private String reader;
    private String text;

    public FirebaseItem() {
        // Default constructor required for Firebase
    }

    public FirebaseItem(String filename, String reader, String text) {
        this.filename = filename;
        this.reader = reader;
        this.text = text;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getReader() { return reader; }
    public void setReader(String reader) { this.reader = reader; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}