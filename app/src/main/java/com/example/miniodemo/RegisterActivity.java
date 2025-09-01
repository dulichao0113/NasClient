package com.example.miniodemo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.miniodemo.R;
import com.example.miniodemo.model.RegisterRequest;
import com.example.miniodemo.model.RegisterResponse;
import com.example.miniodemo.service.ApiService;
import com.example.miniodemo.service.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText etUsername, etEmail, etPassword;
    private Button btnRegister, btnLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化视图
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);

        // 初始化API服务
        apiService = RetrofitClient.getInstance(getApplicationContext()).getApiService();

        // 注册按钮点击事件
        btnRegister.setOnClickListener(v -> register());

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 简单验证
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建注册请求
        RegisterRequest request = new RegisterRequest(username, email, password);

        // 发起注册请求
        Call<RegisterResponse> call = apiService.register(request);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                Log.d(TAG, "onResponse: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    finish(); // 返回登录界面
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，用户名或邮箱已被使用", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                String errorMsg = "网络错误: " + t.getMessage();
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailure: " + errorMsg, t);
                t.printStackTrace();
            }
        });
    }
}
