package com.example.miniodemo.model;

import java.util.List;

/**
 * 多图上传响应模型：对应后端 /api/images/upload-multiple 接口
 */
public class MultiUploadResponse {
    private int total;
    private int success;
    private int failed;
    private List<UploadDetail> details;

    // 空构造函数（Gson 解析必须）
    public MultiUploadResponse() {}

    // Getter 和 Setter
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<UploadDetail> getDetails() {
        return details;
    }

    public void setDetails(List<UploadDetail> details) {
        this.details = details;
    }

    /**
     * 单张图片的上传详情
     */
    public static class UploadDetail {
        private boolean success;
        private int imageId;
        private String fileName;
        private String message;

        public UploadDetail() {}

        // Getter 和 Setter
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getImageId() {
            return imageId;
        }

        public void setImageId(int imageId) {
            this.imageId = imageId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}