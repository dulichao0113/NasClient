package com.example.miniodemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.miniodemo.R;
import com.example.miniodemo.model.UploadResponse;
import com.example.miniodemo.service.ApiService;
import com.example.miniodemo.service.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "token";

    // 视图控件
    private ImageView ivPreview;
    private EditText etFileName;
    private Button btnSelect;
    private Button btnUpload;
    private ProgressBar progressBar;

    // 数据成员
    private Uri selectedImageUri;
    private ApiService apiService;
    private String authToken;
    private int folderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        folderId = getIntent().getIntExtra("folderId", -1);

        // 初始化视图
        initViews();

        // 初始化 Token
        initToken();

        // 初始化 API 服务
        apiService = RetrofitClient.getInstance(getApplicationContext()).getApiService();

        // 绑定按钮点击事件
        bindEvents();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        ivPreview = findViewById(R.id.iv_image_preview);
        etFileName = findViewById(R.id.et_file_name);
        btnSelect = findViewById(R.id.btn_select_image);
        btnUpload = findViewById(R.id.btn_upload);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * 初始化 Token
     */
    private void initToken() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        authToken = sp.getString(KEY_TOKEN, "");

        if (authToken.isEmpty()) {
            finish();
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 绑定按钮事件
     */
    private void bindEvents() {
        btnSelect.setOnClickListener(v -> openImagePicker());
        btnUpload.setOnClickListener(v -> uploadSingleImage());
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * 处理图片选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(ivPreview);

            String fileName = getFileNameFromUri(selectedImageUri);
            etFileName.setText(fileName);
        }
    }

    /**
     * 单图上传逻辑
     */
    private void uploadSingleImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = etFileName.getText().toString().trim();
        if (fileName.isEmpty()) {
            fileName = getFileNameFromUri(selectedImageUri);
            etFileName.setText(fileName);
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        try {
            File tempFile = uriToTempFile(selectedImageUri);
            if (tempFile == null) {
                progressBar.setVisibility(View.GONE);
                btnUpload.setEnabled(true);
                Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建MultipartBody.Part
            RequestBody imageBody = RequestBody.create(
                    MediaType.parse(getContentResolver().getType(selectedImageUri)),
                    tempFile
            );
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image",
                    tempFile.getName(),
                    imageBody
            );
            // 添加文件夹ID参数
            RequestBody folderIdBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(folderId)
            );
            RequestBody fileNameBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    fileName
            );

            Call<UploadResponse> call = apiService.uploadSingleImage(
                    "Bearer " + authToken,
                    imagePart,
                    fileNameBody,
                    folderIdBody
            );
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(UploadActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        String errorMsg = "上传失败，状态码: " + response.code();
                        Toast.makeText(UploadActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    String errorMsg = "网络错误: " + t.getMessage();
                    Toast.makeText(UploadActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "单图上传失败: ", t);
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnUpload.setEnabled(true);
            Toast.makeText(this, "上传异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Uri 转临时文件
     */
    private File uriToTempFile(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }

        String fileName = "temp_single_" + System.currentTimeMillis() + getFileExtension(uri);
        File tempFile = new File(getCacheDir(), fileName);

        OutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();

        return tempFile;
    }

    /**
     * 从 Uri 获取文件名
     */
    private String getFileNameFromUri(Uri uri) {
        String path = uri.getLastPathSegment();
        if (path == null) {
            return "未命名图片";
        }
        int dotIndex = path.lastIndexOf(".");
        return dotIndex != -1 ? path.substring(0, dotIndex) : path;
    }

    /**
     * 获取文件后缀
     */
    private String getFileExtension(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) {
            return ".jpg";
        }
        switch (mimeType) {
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            default: return ".jpg";
        }
    }
}