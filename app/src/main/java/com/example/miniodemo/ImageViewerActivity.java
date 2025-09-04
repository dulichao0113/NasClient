package com.example.miniodemo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.miniodemo.adapter.MediaPagerAdapter;
import com.example.miniodemo.model.Image;

import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGES = "images";
    public static final String EXTRA_POSITION = "position";

    private ViewPager viewPager;
    private MediaPagerAdapter adapter;
    private List<Image> mediaList; // 图片+视频列表
    private int currentPosition;
    private int lastVirtualPosition = -1;

    // 自动播放相关（保持原有逻辑）
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isAutoPlaying = false;
    private static final long INACTIVITY_DELAY = 60000;
    private static final long SLIDE_INTERVAL = 3000;

    private final Runnable inactivityRunnable = this::startAutoPlay;
    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoPlaying && mediaList.size() > 1) {
                // 停止当前页面的视频播放（关键）
                stopCurrentVideo();

                // 计算下一个位置（保持原有循环逻辑）
                int nextRealPosition = (currentPosition + 1) % mediaList.size();
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

        // 获取媒体列表和初始位置
        mediaList = (List<Image>) getIntent().getSerializableExtra(EXTRA_IMAGES);
        currentPosition = getIntent().getIntExtra(EXTRA_POSITION, 0);

        if (mediaList == null || mediaList.isEmpty()) {
            Toast.makeText(this, "没有媒体可查看", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化ViewPager
        viewPager = findViewById(R.id.view_pager);
        adapter = new MediaPagerAdapter(this, mediaList);
        viewPager.setAdapter(adapter);

        // 设置初始位置（适配循环）
        if (mediaList.size() > 1) {
            int initialPosition = Integer.MAX_VALUE / 2;
            initialPosition -= initialPosition % mediaList.size();
            initialPosition += currentPosition;
            viewPager.setCurrentItem(initialPosition);
        } else {
            viewPager.setCurrentItem(currentPosition);
        }

        lastVirtualPosition = viewPager.getCurrentItem();
//        viewPager.setOffscreenPageLimit(3);

        // 监听页面变化
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                // 页面切换时，先停止上一个页面的视频
                if (lastVirtualPosition != -1) {
                    stopVideoByVirtualPosition(lastVirtualPosition);
                }
                // 更新当前位置和上一个位置
                currentPosition = position % mediaList.size();
                lastVirtualPosition = position; // 记录当前虚拟位置为下一次的上一个位置
//                resetInactivityTimer();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        resetInactivityTimer();
    }

    // 停止当前页面的视频播放（自动切换时关键）
    private void stopCurrentVideo() {
        stopVideoByVirtualPosition(viewPager.getCurrentItem());
    }

    private void stopVideoByVirtualPosition(int virtualPosition) {
        View view = viewPager.findViewWithTag(virtualPosition);
        if (view != null) {
            VideoView videoView = view.findViewById(R.id.vv_media);
            ImageView ivPlayButton = view.findViewById(R.id.iv_play_button);
            ImageView ivMedia = view.findViewById(R.id.iv_media);
            if (videoView != null && videoView.isPlaying()) {
//                videoView.stopPlayback();
                // 这里千万不能 stop 或者 release  会导致滑回来的时候视频无法正常播放
                videoView.pause();
                ivPlayButton.setVisibility(View.VISIBLE);
                ivMedia.setVisibility(View.VISIBLE);
            }
        }
    }

    // 重置无操作计时器（保持原有逻辑）
    private void resetInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable);
        handler.removeCallbacks(slideRunnable);
        isAutoPlaying = false;
        handler.postDelayed(inactivityRunnable, INACTIVITY_DELAY);
    }

    // 开始自动播放（保持原有逻辑）
    private void startAutoPlay() {
        if (mediaList.size() <= 1) return;
        isAutoPlaying = true;
        handler.postDelayed(slideRunnable, SLIDE_INTERVAL);
    }

    // 停止自动播放（保持原有逻辑）
    private void stopAutoPlay() {
        isAutoPlaying = false;
        handler.removeCallbacks(slideRunnable);
    }

    // 监听用户触摸事件（保持原有逻辑）
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        resetInactivityTimer();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoPlay();
        stopCurrentVideo(); // 暂停时停止视频播放
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
        viewPager.removeOnPageChangeListener(null);
    }
}