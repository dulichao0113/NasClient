package com.example.miniodemo.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.example.miniodemo.R;
import com.example.miniodemo.model.Image;

import java.util.ArrayList;
import java.util.List;

public class MediaPagerAdapter extends PagerAdapter {
    private Context context;
    private List<Image> mediaList; // 包含图片和视频的列表
    private List<View> cachedViews = new ArrayList<>(); // 缓存已创建的View

    public MediaPagerAdapter(Context context, List<Image> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    @Override
    public int getCount() {
        return mediaList.size() <= 1 ? mediaList.size() : Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        // 计算真实位置
        int realPosition = position % mediaList.size();
        Image media = mediaList.get(realPosition);

        // 加载布局
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_image_viewer, container, false);
        view.setTag(position);
        ImageView ivMedia = view.findViewById(R.id.iv_media);
        VideoView vvMedia = view.findViewById(R.id.vv_media);
        ImageView ivPlayButton = view.findViewById(R.id.iv_play_button);


        vvMedia.setVisibility(View.GONE);
        ivPlayButton.setVisibility(View.GONE);
        ivMedia.setVisibility(View.VISIBLE);
        android.util.Log.i("dulichao:","instantiateItem isVideo :"+media.isVideo()+", realPosition:"+realPosition);
        if (media.isVideo()) {
            ivPlayButton.setVisibility(View.VISIBLE);
            // 设置视频源
            vvMedia.setVideoURI(Uri.parse(media.getUrl()));
            // 播放按钮点击事件
            ivPlayButton.setOnClickListener(v -> {
                android.util.Log.i("dulichao:","start video"+", realPosition:"+realPosition);
                vvMedia.start();
                ivPlayButton.setVisibility(View.GONE);
                vvMedia.setVisibility(View.VISIBLE);
            });

            vvMedia.setOnPreparedListener(mp -> {
                // 准备完成后，设置循环播放（可选）
                mp.setLooping(false);
                // 监听视频第一帧开始渲染的事件
                vvMedia.setOnInfoListener((mp1, what, extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        // 第一帧已渲染，此时隐藏封面图
                        android.util.Log.i("dulichao:","on video first rendering"+", realPosition:"+realPosition);
                        ivMedia.animate()
                                .alpha(0f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    android.util.Log.i("dulichao:","video started first render, gone image view"+", realPosition:"+realPosition);
                                    ivMedia.setVisibility(View.GONE);
                                })
                                .start();
                        return true;
                    }
                    return false;
                });
            });

            // 视频播放完成后显示播放按钮
            vvMedia.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    android.util.Log.i("dulichao:","video play back completion"+", realPosition:"+realPosition);
//                    ivPlayButton.setVisibility(View.VISIBLE);
//                    vvMedia.setVisibility(View.GONE);
//                    ivMedia.setVisibility(View.VISIBLE);
//                    vvMedia.setVideoURI(Uri.parse(media.getUrl()));
                    vvMedia.seekTo(0);
                    vvMedia.start();
                }
            });

//            MediaController mediaController = new MediaController(context);
//            mediaController.setAnchorView(vvMedia);
//            vvMedia.setMediaController(mediaController);

        }
        Glide.with(context)
                .load(media.getUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .fitCenter()
                .into(ivMedia);

        container.addView(view);
//        cachedViews.add(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        VideoView vvMedia = view.findViewById(R.id.vv_media);
        if (vvMedia != null) {
            vvMedia.stopPlayback();
        }
//        cachedViews.remove(view);
        container.removeView(view);
    }
}