package com.example.dripclient;

public enum Category {
    RENDER("Render"),
    COMBAT("Combat"),
    WORLD("World");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
