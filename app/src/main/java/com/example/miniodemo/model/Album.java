package com.example.miniodemo.model;

public class Album {
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_FAVORITE = 1;

    private int id;
    private String name;
    private int type;
    private boolean isVirtual;
    private String firstImageUrl;
    private int imageCount;

    public Album(int id, String name, int type, boolean isVirtual) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isVirtual = isVirtual;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    public String getFirstImageUrl() {
        return firstImageUrl;
    }

    public void setFirstImageUrl(String url) {
        this.firstImageUrl = url;
    }

    public int getImageCount() { return imageCount; }

    public void setImageCount(int imageCount) { this.imageCount = imageCount; }
}