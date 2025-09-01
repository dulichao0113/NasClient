package com.example.miniodemo.model;

import java.util.List;

public class DeleteRequest {
    private List<Integer> imageIds;

    public DeleteRequest(List<Integer> imageIds) {
        this.imageIds = imageIds;
    }

    public List<Integer> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<Integer> imageIds) {
        this.imageIds = imageIds;
    }
}