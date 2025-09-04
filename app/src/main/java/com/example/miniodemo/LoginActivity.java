package com.example.miniodemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.miniodemo.R;
import com.example.miniodemo.model.LoginRequest;
import com.example.miniodemo.model.LoginResponse;
import com.example.miniodemo.service.ApiService;
import com.example.miniodemo.service.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ApiService apiService;
    private EditText etIpAddress;
    private String customIpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MemoryCheck.checkLargeHeap(this);
        // 初始化视图
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        etIpAddress = findViewById(R.id.et_ip_address);

        SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String savedIp = sp.getString("server_ip", "");
        if (!TextUtils.isEmpty(savedIp)) {
            etIpAddress.setText(savedIp);
        }

        // 检查是否已登录
        checkLoggedInStatus();

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> login());

        // 注册按钮点击事件
        btnRegister.setOnClickListener(v -> {
            customIpAddress = etIpAddress.getText().toString().trim();
            if (TextUtils.isEmpty(customIpAddress)) {
                Toast.makeText(LoginActivity.this, "请输入IP地址", Toast.LENGTH_LONG).show();
                return;
            }
            apiService = RetrofitClient.getInstance(getApplicationContext(), customIpAddress).getApiService();
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void checkLoggedInStatus() {
        SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String token = sp.getString("token", null);

        if (!TextUtils.isEmpty(token)) {
            // 初始化API服务
            String savedIp = sp.getString("server_ip", "");
            apiService = RetrofitClient.getInstance(getApplicationContext(), savedIp).getApiService();
            // 跳转到相册列表界面
            Intent intent = new Intent(LoginActivity.this, AlbumListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 简单验证
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取用户输入的IP地址
        customIpAddress = etIpAddress.getText().toString().trim();
        apiService = RetrofitClient.getInstance(getApplicationContext(), customIpAddress).getApiService();

        // 创建登录请求
        LoginRequest request = new LoginRequest(email, password);

        // 发起登录请求
        Call<LoginResponse> call = apiService.login(request);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.i(TAG,"onResponse 状态码: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    // 保存令牌和用户信息
                    String token = response.body().getToken();
                    SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    sp.edit()
                            .putString("token", token)
                            .putInt("user_id", response.body().getUser().getId())
                            .putString("username", response.body().getUser().getUsername())
                            .putString("email", email)  // 保存用户输入的邮箱
                            .putString("password", password)  // 保存用户输入的密码
                            .putString("server_ip", customIpAddress)
                            .apply();

                    // 跳转到相册列表界面
                    Intent intent = new Intent(LoginActivity.this, AlbumListActivity.class);
                    startActivity(intent);
                    finish();

                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败，请检查邮箱和密码", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG,"onFailure : " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
}
