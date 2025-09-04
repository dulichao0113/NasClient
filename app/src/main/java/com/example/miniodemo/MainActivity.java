package com.example.miniodemo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miniodemo.R;
import com.example.miniodemo.model.BaseResponse;
import com.example.miniodemo.model.DeleteRequest;
import com.example.miniodemo.model.Image;
import com.example.miniodemo.model.ImageListResponse;
import com.example.miniodemo.model.LoginRequest;
import com.example.miniodemo.model.LoginResponse;
import com.example.miniodemo.model.MultiUploadResponse;
import com.example.miniodemo.service.ApiService;
import com.example.miniodemo.service.RetrofitClient;
import com.example.miniodemo.adapter.ImageAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    // 常量定义
    private static final int UPLOAD_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "token";

    // 视图与数据成员
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<Image> imageList = new ArrayList<>();
    private ProgressBar progressBar;
    private FloatingActionButton fabUploadSingle;
    private FloatingActionButton fabUploadMultiple;
    private ApiService apiService;
    private String authToken;

    private Button btnDelete;
    private Toolbar toolbar;
    private String originalToolbarTitle;
    private SwipeRefreshLayout swipeRefreshLayout;

    // 多图选择启动器
    private ActivityResultLauncher<Intent> multipleImageLauncher;

    private int currentAlbumId;
    private String currentAlbumName;
    private boolean isFavoriteAlbum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取从AlbumListActivity传递的参数
        Intent intent = getIntent();
        currentAlbumId = intent.getIntExtra("albumId", -1);
        currentAlbumName = intent.getStringExtra("albumName");
        isFavoriteAlbum = intent.getBooleanExtra("isFavoriteAlbum", false);

        // 初始化 Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 设置标题为当前相册名
        if (currentAlbumName != null) {
            getSupportActionBar().setTitle(currentAlbumName);
        }
        originalToolbarTitle = getSupportActionBar().getTitle().toString();

        // 初始化视图
        initViews();

        // 初始化 Token（登录校验）
        initToken();

        // 初始化 API 服务
        apiService = RetrofitClient.getInstance(getApplicationContext()).getApiService();

        // 初始化 RecyclerView
        initRecyclerView();

        // 加载图片列表
        loadImages();

        // 初始化多图选择启动器
        initMultipleImageLauncher();

        // 绑定按钮点击事件
        bindButtonEvents();
    }

    /**
     * 初始化视图控件
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_images);
        progressBar = findViewById(R.id.progress_bar);
        fabUploadSingle = findViewById(R.id.fab_upload_single);
        fabUploadMultiple = findViewById(R.id.fab_upload_multiple);
        btnDelete = findViewById(R.id.btn_delete);

        btnDelete.setOnClickListener(v -> deleteSelectedImages());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 下拉时触发加载图片（复用原有 loadImages() 方法）
            loadImages();
        });
    }

    // 修改loadImages方法
    private void loadImages() {
        // 启动刷新动画
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.postDelayed(() -> startImageLoad(), 100);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            startImageLoad();
        }
    }

    private void startImageLoad() {
        Log.d(TAG, "开始加载图片，请求头: Bearer " + authToken);

        // 根据相册类型加载不同图片
        Call<List<Image>> call;
        if (isFavoriteAlbum) {
            // 加载收藏图片
            call = apiService.getImages("Bearer " + authToken, null, true);
        } else if (currentAlbumId != -1) {
            // 加载指定文件夹图片
            call = apiService.getImages("Bearer " + authToken, String.valueOf(currentAlbumId), false);
        } else {
            // 加载所有图片
            call = apiService.getImages("Bearer " + authToken, "all", false);
        }

        call.enqueue(new Callback<List<Image>>() {
            @Override
            public void onResponse(Call<List<Image>> call, Response<List<Image>> response) {
                // 停止刷新动画
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "加载图片响应状态码: " + response.code());

                // 检查Token是否失效
                if (response.code() == 401 || response.code() == 403) {
                    Toast.makeText(MainActivity.this, "登录已过期，正在重新登录...", Toast.LENGTH_SHORT).show();
                    SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    sp.edit().putString("token", "").apply();
                    handleTokenExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Image> newImages = response.body();
                    Log.d(TAG, "成功获取 " + newImages.size() + " 张图片");

                    // 更新列表
                    imageList.clear();
                    imageList.addAll(newImages);
                    adapter.updateData(newImages);

                    if (imageList.isEmpty()) {
                        Toast.makeText(MainActivity.this, "该相册暂无图片", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "加载失败，状态码: " + response.code();
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Image>> call, Throwable t) {
                // 处理失败情况
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                progressBar.setVisibility(View.GONE);
                String errorMsg = "加载失败: " + t.getMessage();
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "获取图片列表失败: ", t);
            }
        });
    }

    /**
     * 初始化 Token，未登录则跳转登录页
     */
    private void initToken() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        authToken = sp.getString(KEY_TOKEN, "");

        if (authToken.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 初始化 RecyclerView
     */
    private void initRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ImageAdapter(this, imageList);
        recyclerView.setAdapter(adapter);

        // 添加图片点击和长按事件
        adapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Image media = imageList.get(position);
//                if ("video".equals(media.getType())) {
//                    // 视频点击 - 打开视频播放器
//                    Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
//                    intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO, media);
//                    startActivity(intent);
//                } else {
//                    // 图片点击 - 打开图片查看器
                    Intent intent = new Intent(MainActivity.this, ImageViewerActivity.class);
                    intent.putExtra(ImageViewerActivity.EXTRA_IMAGES, (Serializable) imageList);
                    intent.putExtra(ImageViewerActivity.EXTRA_POSITION, position);
                    startActivity(intent);
//                }
            }

            @Override
            public void onItemLongClick(int position) {
                // 长按进入选择模式
                enterSelectionMode(position);
            }
        });
    }

    /**
     * 初始化多图选择启动器
     */
    private void initMultipleImageLauncher() {
        multipleImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleMultipleImageResult(result.getData());
                    }
                }
        );
    }

    /**
     * 绑定按钮点击事件
     */
    private void bindButtonEvents() {
        // 单图上传：跳转 UploadActivity
        fabUploadSingle.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UploadActivity.class);
            startActivityForResult(intent, UPLOAD_REQUEST_CODE);
        });

        // 多图上传：打开系统相册选择多张图片
        fabUploadMultiple.setOnClickListener(v -> openMultipleImagePicker());
    }

    /**
     * 处理单图上传后刷新列表
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPLOAD_REQUEST_CODE && resultCode == RESULT_OK) {
            loadImages(); // 单图上传成功后刷新
        }
    }

    /**
     * 打开系统相册选择多张图片
     */
    private void openMultipleImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 支持所有类型
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_TITLE, "选择图片或视频");
        multipleImageLauncher.launch(Intent.createChooser(intent, "选择媒体文件"));
    }

    /**
     * 处理多图选择结果
     */
    private void handleMultipleImageResult(Intent data) {
        List<Uri> selectedUris = new ArrayList<>();

        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                selectedUris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            selectedUris.add(data.getData());
        }

        if (!selectedUris.isEmpty()) {
            uploadMultipleImages(selectedUris);
        } else {
            Toast.makeText(this, "未选择任何图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 多图上传核心逻辑
     */
    private void uploadMultipleImages(List<Uri> imageUris) {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "开始多图上传，共 " + imageUris.size() + " 张图片");

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : imageUris) {
            try {
                File tempFile = uriToTempFile(uri);
                if (tempFile == null) {
                    Log.e(TAG, "文件转换失败，跳过 Uri: " + uri);
                    continue;
                }

                RequestBody requestBody = RequestBody.create(
                        MediaType.parse(getContentResolver().getType(uri)),
                        tempFile
                );

                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                        "images",
                        tempFile.getName(),
                        requestBody
                );

                imageParts.add(imagePart);
            } catch (Exception e) {
                Log.e(TAG, "处理图片失败: " + e.getMessage());
                Toast.makeText(this, "处理图片 " + getFileName(uri) + " 失败", Toast.LENGTH_SHORT).show();
            }
        }

        if (imageParts.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "没有可上传的图片", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<MultiUploadResponse> call = apiService.uploadMultipleImages(
                "Bearer " + authToken,
                imageParts
        );
        call.enqueue(new Callback<MultiUploadResponse>() {
            @Override
            public void onResponse(Call<MultiUploadResponse> call, Response<MultiUploadResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    MultiUploadResponse result = response.body();
                    String resultMsg = String.format(
                            "上传完成！共 %d 张，成功 %d 张，失败 %d 张",
                            result.getTotal(),
                            result.getSuccess(),
                            result.getFailed()
                    );
                    Toast.makeText(MainActivity.this, resultMsg, Toast.LENGTH_LONG).show();
                    loadImages();

                    for (MultiUploadResponse.UploadDetail detail : result.getDetails()) {
                        if (!detail.isSuccess()) {
                            Log.e(TAG, "失败图片: " + detail.getFileName() + "，原因: " + detail.getMessage());
                        }
                    }
                } else {
                    String errorMsg = "上传失败，状态码: " + response.code();
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MultiUploadResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = "网络错误: " + t.getMessage();
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "多图上传失败: ", t);
            }
        });
    }

    /**
     * Uri 转换为临时文件
     */
    private File uriToTempFile(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }

        String fileName = "temp_img_" + System.currentTimeMillis() + getFileExtension(uri);
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
    private String getFileName(Uri uri) {
        String path = uri.getLastPathSegment();
        return path != null ? path : "未知图片";
    }

    /**
     * 从 Uri 获取文件后缀
     */
    private String getFileExtension(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) {
            return ".jpg";
        }
        switch (mimeType) {
            // 图片类型
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "image/gif": return ".gif";
            case "image/webp": return ".webp";
            // 视频类型
            case "video/mp4": return ".mp4";
            case "video/3gpp": return ".3gp";
            case "video/mpeg": return ".mpeg";
            case "video/quicktime": return ".mov";
            default: return ".jpg";
        }
    }

    /**
     * 加载图片列表（全用户可见）
     */
//    private void loadImages() {
//        progressBar.setVisibility(View.VISIBLE);
//        Log.d(TAG, "开始加载图片，请求头: Bearer " + authToken);
//
//        // 新调用：接收 List<Image> 数组
//        Call<List<Image>> call = apiService.getImages("Bearer " + authToken);
//        call.enqueue(new Callback<List<Image>>() {
//            @Override
//            public void onResponse(Call<List<Image>> call, Response<List<Image>> response) {
//                progressBar.setVisibility(View.GONE);
//                Log.d(TAG, "加载图片响应状态码: " + response.code());
//
//                // 直接解析数组数据
//                if (response.isSuccessful() && response.body() != null) {
//                    List<Image> newImages = response.body(); // 后端返回的纯数组
//                    Log.d(TAG, "成功获取 " + newImages.size() + " 张图片");
//
//                    // 更新列表
//                    imageList.clear();
//                    imageList.addAll(newImages);
//                    adapter.updateData(newImages);
//
//                    if (imageList.isEmpty()) {
//                        Toast.makeText(MainActivity.this, "暂无图片，快去上传吧～", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    String errorMsg = "加载失败，状态码: " + response.code();
//                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<Image>> call, Throwable t) {
//                progressBar.setVisibility(View.GONE);
//                String errorMsg = "加载失败: " + t.getMessage();
//                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "获取图片列表失败: ", t);
//            }
//        });
//    }

    /**
     * 创建菜单（刷新+退出登录）
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * 菜单点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadImages();
            return true;
        } else if (id == R.id.action_logout) {
            SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            sp.edit().clear().apply();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 进入选择模式
     */
    private void enterSelectionMode(int initialPosition) {
        adapter.setSelectionMode(true);
        adapter.toggleSelection(initialPosition);
        btnDelete.setVisibility(View.VISIBLE);
        updateToolbarForSelectionMode();

        // 隐藏上传按钮
        fabUploadSingle.setVisibility(View.GONE);
        fabUploadMultiple.setVisibility(View.GONE);
    }

    /**
     * 退出选择模式
     */
    private void exitSelectionMode() {
        adapter.setSelectionMode(false);
        adapter.clearSelections();
        btnDelete.setVisibility(View.GONE);
        getSupportActionBar().setTitle(originalToolbarTitle);

        // 显示上传按钮
        fabUploadSingle.setVisibility(View.VISIBLE);
        fabUploadMultiple.setVisibility(View.VISIBLE);
    }

    /**
     * 更新选择模式下的工具栏
     */
    private void updateToolbarForSelectionMode() {
        int selectedCount = adapter.getSelectedItemCount();
        getSupportActionBar().setTitle(String.format("已选择 %d 项", selectedCount));

        // 添加返回按钮处理
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> exitSelectionMode());
    }

    /**
     * 删除选中的图片
     */
    private void deleteSelectedImages() {
        int selectedCount = adapter.getSelectedItemCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "请先选择要删除的图片", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示确认对话框
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage(String.format("确定要删除选中的 %d 张图片吗？", selectedCount))
                .setPositiveButton("删除", (dialog, which) -> {
                    // 执行删除操作
                    List<Integer> selectedIds = adapter.getSelectedImageIds();
                    performDeleteImages(selectedIds);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行删除图片的网络请求
     */
    private void performDeleteImages(List<Integer> imageIds) {
        progressBar.setVisibility(View.VISIBLE);

        Call<BaseResponse> call = apiService.deleteImages(
                "Bearer " + authToken,
                new DeleteRequest(imageIds)
        );

        call.enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this,
                            response.body().getMessage(), Toast.LENGTH_SHORT).show();

                    // 退出选择模式并刷新列表
                    exitSelectionMode();
                    loadImages(); // 重新加载图片列表
                } else {
                    Toast.makeText(MainActivity.this,
                            "删除失败，状态码: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "删除图片失败: ", t);
            }
        });
    }

    // 重写onBackPressed方法，处理选择模式下的返回键
    @Override
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 处理Token过期：尝试自动登录获取新Token
     */
    private void handleTokenExpired() {
        // 从SharedPreferences获取保存的登录凭证
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedEmail = sp.getString("email", "");
        String savedPassword = sp.getString("password", "");

        // 无保存的凭证，直接跳转登录页
        if (savedEmail.isEmpty() || savedPassword.isEmpty()) {
            navigateToLogin();
            return;
        }

        // 显示加载状态
        progressBar.setVisibility(View.VISIBLE);

        // 自动登录请求
        LoginRequest loginRequest = new LoginRequest(savedEmail, savedPassword);
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    // 自动登录成功，更新Token
                    String newToken = response.body().getToken();
                    sp.edit()
                            .putString(KEY_TOKEN, newToken)
                            .apply();
                    authToken = newToken;

                    // 重新加载图片列表
                    loadImages();
                    Toast.makeText(MainActivity.this, "Token已刷新", Toast.LENGTH_SHORT).show();
                } else {
                    // 自动登录失败（如密码错误），跳转登录页
                    Toast.makeText(MainActivity.this, "登录信息已失效，请重新登录", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "网络错误，无法自动登录", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
    }

    /**
     * 跳转登录页并清除当前页面
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // 关闭当前页面，避免返回
    }
}