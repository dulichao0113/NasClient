package com.example.miniodemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.miniodemo.adapter.LoopImagePagerAdapter;
import com.example.miniodemo.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGES = "images";
    public static final String EXTRA_POSITION = "position";

    private ViewPager viewPager;
    private LoopImagePagerAdapter adapter;
    private List<Image> imageList;
    private List<String> imageUrls;
    private int currentPosition; // 真实位置（0 ~ imageList.size()-1）

    // 自动播放相关
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isAutoPlaying = false;
    private static final long INACTIVITY_DELAY = 10000; // 10秒无操作
    private static final long SLIDE_INTERVAL = 3000; // 3秒切换一张

    private final Runnable inactivityRunnable = this::startAutoPlay;
    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoPlaying && imageUrls.size() > 1) {
                // 计算下一个虚拟位置（保证循环）
                int nextRealPosition = (currentPosition + 1) % imageUrls.size();
                int currentVirtualPosition = viewPager.getCurrentItem();
                int virtualOffset = currentVirtualPosition - currentPosition;
                int nextVirtualPosition = nextRealPosition + virtualOffset;

                viewPager.setCurrentItem(nextVirtualPosition, true);
                handler.postDelayed(this, SLIDE_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取传递的图片数据和当前位置
        imageList = (List<Image>) getIntent().getSerializableExtra(EXTRA_IMAGES);
        currentPosition = getIntent().getIntExtra(EXTRA_POSITION, 0);

        if (imageList == null || imageList.isEmpty()) {
            Toast.makeText(this, "没有图片可查看", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 提取图片URL列表
        imageUrls = new ArrayList<>();
        for (Image image : imageList) {
            String url = image.getUrl();
            if (url != null && !url.isEmpty()) {
                imageUrls.add(url);
            }
        }

        // 初始化ViewPager（注意是ViewPager而非ViewPager2）
        viewPager = findViewById(R.id.view_pager);
        adapter = new LoopImagePagerAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);

        // 设置初始位置（适配循环逻辑）
        if (imageUrls.size() > 1) {
            int initialPosition = Integer.MAX_VALUE / 2;
            initialPosition -= initialPosition % imageUrls.size();
            initialPosition += currentPosition;
            viewPager.setCurrentItem(initialPosition);
        } else {
            viewPager.setCurrentItem(currentPosition);
        }

        // 监听ViewPager页面变化（使用ViewPager的OnPageChangeListener）
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                // 更新真实位置（通过取模计算）
                currentPosition = position % imageUrls.size();
                resetInactivityTimer(); // 用户手动滑动时重置计时器
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // 开始监听用户操作
        resetInactivityTimer();
    }

    // 重置无操作计时器
    private void resetInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable);
        handler.removeCallbacks(slideRunnable); // 停止当前自动播放
        isAutoPlaying = false;
        handler.postDelayed(inactivityRunnable, INACTIVITY_DELAY); // 重新计时
    }

    // 开始自动播放
    private void startAutoPlay() {
        if (imageUrls.size() <= 1) return; // 只有一张图时不自动播放
        isAutoPlaying = true;
        handler.postDelayed(slideRunnable, SLIDE_INTERVAL);
    }

    // 停止自动播放
    private void stopAutoPlay() {
        isAutoPlaying = false;
        handler.removeCallbacks(slideRunnable);
    }

    // 监听用户触摸事件，重置计时器
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        resetInactivityTimer();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoPlay();
        handler.removeCallbacks(inactivityRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetInactivityTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        viewPager.removeOnPageChangeListener(null); // 移除监听器
    }
}