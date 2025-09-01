package com.example.miniodemo.model;

import java.util.List;

public class ImageListResponse {
    private String message;
    private List<Image> images;

    public ImageListResponse() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }
}
    