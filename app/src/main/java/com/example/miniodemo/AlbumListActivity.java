package com.example.miniodemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miniodemo.adapter.AlbumAdapter;
import com.example.miniodemo.model.Album;
import com.example.miniodemo.model.BaseResponse;
import com.example.miniodemo.model.Folder;
import com.example.miniodemo.model.Image;
import com.example.miniodemo.model.LoginRequest;
import com.example.miniodemo.model.LoginResponse;
import com.example.miniodemo.service.ApiService;
import com.example.miniodemo.service.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumListActivity extends AppCompatActivity {
    private static final String TAG = "AlbumListActivity";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "token";

    private RecyclerView recyclerView;
    private AlbumAdapter adapter;
    private List<Album> albumList = new ArrayList<>();
    private ProgressBar progressBar;
    private ApiService apiService;
    private String authToken;
    private int allImagesFolderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);

        // 初始化视图
        initViews();

        // 初始化Token
        initToken();

        // 初始化API服务
        apiService = RetrofitClient.getInstance(getApplicationContext()).getApiService();

        // 加载相册列表
        loadAlbums();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_albums);
        progressBar = findViewById(R.id.progress_bar);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new AlbumAdapter(this, albumList, position -> {
            Album album = albumList.get(position);
            openAlbum(album);
        });
        recyclerView.setAdapter(adapter);
    }

    private void initToken() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        authToken = sp.getString(KEY_TOKEN, "");

        if (authToken.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

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
                    loadAlbums();
                    Toast.makeText(AlbumListActivity.this, "Token已刷新", Toast.LENGTH_SHORT).show();
                } else {
                    // 自动登录失败（如密码错误），跳转登录页
                    Toast.makeText(AlbumListActivity.this, "登录信息已失效，请重新登录", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AlbumListActivity.this, "网络错误，无法自动登录", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // 关闭当前页面，避免返回
    }

    private void loadAlbums() {
        progressBar.setVisibility(View.VISIBLE);

        // 先确保"all image"文件夹存在
        apiService.ensureAllImagesFolder("Bearer " + authToken)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {

                        // 检查Token是否失效
                        if (response.code() == 401 || response.code() == 403) {
                            Toast.makeText(AlbumListActivity.this, "登录已过期，正在重新登录...", Toast.LENGTH_SHORT).show();
                            SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            sp.edit().putString("token", "").apply();
                            handleTokenExpired();
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null) {
//                            // 获取all image文件夹ID
//                            allImagesFolderId = response.body().getFolderId();

                            // 获取所有文件夹
                            loadFolders();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AlbumListActivity.this, "获取默认文件夹失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "获取默认文件夹失败: ", t);
                        Toast.makeText(AlbumListActivity.this, "获取文件夹失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFolders() {
        apiService.getFolders("Bearer " + authToken)
                .enqueue(new Callback<List<Folder>>() {
                    @Override
                    public void onResponse(Call<List<Folder>> call, Response<List<Folder>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Folder> folders = response.body();

                            // 清空列表
                            albumList.clear();

                            // 添加所有文件夹
                            for (Folder folder : folders) {
                                albumList.add(new Album(
                                        folder.getId(),
                                        folder.getName(),
                                        Album.TYPE_FOLDER,
                                        false
                                ));
                            }

                            // 添加收藏夹虚拟相册
                            albumList.add(new Album(
                                    -1, // 虚拟ID
                                    "收藏",
                                    Album.TYPE_FAVORITE,
                                    true
                            ));

                            loadAlbumCoversAndCounts();
                        } else {
                            Toast.makeText(AlbumListActivity.this, "获取相册列表失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Folder>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "获取相册失败: ", t);
                        Toast.makeText(AlbumListActivity.this, "获取相册失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAlbumCoversAndCounts() {
        // 记录未完成的请求数，避免频繁刷新适配器
        int totalAlbums = albumList.size();
        int[] pendingRequests = {totalAlbums}; // 用数组存储，方便匿名类修改

        for (Album album : albumList) {
            // 复用startImageLoad的逻辑：根据相册类型构造不同的图片请求
            Call<List<Image>> call;
            if (album.getType() == Album.TYPE_FAVORITE) {
                // 收藏夹：加载所有收藏的图片
                call = apiService.getImages("Bearer " + authToken, null, true);
            } else {
                // 普通文件夹：加载该文件夹下的图片
                call = apiService.getImages("Bearer " + authToken, String.valueOf(album.getId()), false);
            }

            // 发起请求
            call.enqueue(new Callback<List<Image>>() {
                @Override
                public void onResponse(Call<List<Image>> call, Response<List<Image>> response) {
                    pendingRequests[0]--; // 减少未完成请求数

                    if (response.isSuccessful() && response.body() != null) {
                        List<Image> albumImages = response.body();
                        // 设置图片数量（列表大小）
                        album.setImageCount(albumImages.size());
                        // 设置封面URL（取第一张图片的URL，无图片则为null）
                        if (!albumImages.isEmpty()) {
                            album.setFirstImageUrl(albumImages.get(0).getUrl());
                        } else {
                            album.setFirstImageUrl(null);
                        }
                    } else {
                        // 接口失败时默认数量为0，无封面
                        album.setImageCount(0);
                        album.setFirstImageUrl(null);
                        Log.e(TAG, "获取相册[" + album.getName() + "]图片失败，状态码: " + response.code());
                    }

                    // 所有请求完成后刷新列表
                    if (pendingRequests[0] == 0) {
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE); // 所有加载完成后隐藏进度条
                    }
                }

                @Override
                public void onFailure(Call<List<Image>> call, Throwable t) {
                    pendingRequests[0]--;
                    // 网络失败时默认数量为0，无封面
                    album.setImageCount(0);
                    album.setFirstImageUrl(null);
                    Log.e(TAG, "获取相册[" + album.getName() + "]图片失败: ", t);

                    // 所有请求完成后刷新列表
                    if (pendingRequests[0] == 0) {
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE); // 所有加载完成后隐藏进度条
                    }
                }
            });
        }
    }

    private void openAlbum(Album album) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("albumId", album.getId());
        intent.putExtra("albumName", album.getName());
        intent.putExtra("isFavoriteAlbum", album.getType() == Album.TYPE_FAVORITE);
        startActivity(intent);
    }
}