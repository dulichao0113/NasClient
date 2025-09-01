package com.example.miniodemo.model;

import java.io.Serializable;

public class Image implements Serializable {
    private int id;
    private int user_id;
    private String minio_object_key;
    private String file_name;
    private long file_size;
    private String content_type;
    private String upload_time;
    private String last_accessed;
    private int is_favorite;
    private Integer folder_id;
    private String url;
    private boolean isSelected = false;

    public Image() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getMinio_object_key() {
        return minio_object_key;
    }

    public void setMinio_object_key(String minio_object_key) {
        this.minio_object_key = minio_object_key;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    public String getUpload_time() {
        return upload_time;
    }

    public void setUpload_time(String upload_time) {
        this.upload_time = upload_time;
    }

    public String getLast_accessed() {
        return last_accessed;
    }

    public void setLast_accessed(String last_accessed) {
        this.last_accessed = last_accessed;
    }

    public boolean isIs_favorite() {
        return is_favorite != 0;
    }

    public void setIs_favorite(int is_favorite) {
        this.is_favorite = is_favorite;
    }

    public void setIs_favorite(boolean is_favorite) {
        this.is_favorite = is_favorite ? 1 : 0;
    }

    public Integer getFolder_id() {
        return folder_id;
    }

    public void setFolder_id(Integer folder_id) {
        this.folder_id = folder_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
    