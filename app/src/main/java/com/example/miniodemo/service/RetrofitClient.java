package com.example.miniodemo.service;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class RetrofitClient {
    private static String BASE_URL;
    private static RetrofitClient instance;
    private Retrofit retrofit;

    private RetrofitClient(Context context, String customIp) {

        String ipAddress = null;
        if (!TextUtils.isEmpty(customIp)) {
            ipAddress = customIp;
        } else {
            ipAddress = getLocalIpAddress(context);
        }

        BASE_URL = "http://" + (ipAddress != null ? ipAddress : "192.168.29.253") + ":3000/api/";
        // 添加日志拦截器
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            SharedPreferences sp = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String savedIp = sp.getString("server_ip", "");
            instance = new RetrofitClient(context, savedIp);
        }
        return instance;
    }

    public static synchronized RetrofitClient getInstance(Context context, String customIp) {
        if (instance == null) {
            instance = new RetrofitClient(context, customIp);
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    private String getLocalIpAddress(Context context) {
        // 先尝试通过 Wi-Fi 获取
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            String wifiIp = Formatter.formatIpAddress(ipInt);
            if (isValidIp(wifiIp)) {
                return wifiIp;
            }
        }

        // Wi-Fi 未连接时，尝试通过网络接口获取
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // 过滤回环地址和 IPv6 地址
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().indexOf(':') == -1) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0");
    }
}

