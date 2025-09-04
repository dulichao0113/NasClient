package com.example.miniodemo;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class MemoryCheck {
    public static void checkLargeHeap(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            int normalMemory = am.getMemoryClass();
            int largeMemory = am.getLargeMemoryClass();

            Log.d("MemoryCheck", "=== LargeHeap 检查 ===");
            Log.d("MemoryCheck", "标准内存限制: " + normalMemory + "MB");
            Log.d("MemoryCheck", "大内存限制: " + largeMemory + "MB");
            Log.d("MemoryCheck", "是否启用LargeHeap: " + (largeMemory > normalMemory));

            // 检查实际堆内存
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            Log.d("MemoryCheck", "实际最大堆内存: " + maxMemory + "MB");

        } catch (Exception e) {
            Log.e("MemoryCheck", "检查失败", e);
        }
    }
}
