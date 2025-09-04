package com.example.miniodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.miniodemo.R;
import com.example.miniodemo.model.Album;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private Context context;
    private List<Album> albumList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public AlbumAdapter(Context context, List<Album> albumList, OnItemClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        // 设置相册名称
        holder.tvAlbumName.setText(album.getName());
        // 设置图片数量（假设有getImageCount()方法）
        holder.tvImageCount.setText(album.getImageCount() + " 张图片");
        // 加载封面图（使用Glide，复用现有占位图和错误图）
        String coverUrl = album.getFirstImageUrl();
        Glide.with(context)
                .load(coverUrl)
                .placeholder(R.drawable.ic_image_placeholder) // 复用现有占位图
                .error(R.drawable.ic_image_error) // 复用现有错误图
                .centerCrop()
                .into(holder.ivAlbumCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumCover;
        TextView tvAlbumName;
        TextView tvImageCount;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.iv_album_cover);
            tvAlbumName = itemView.findViewById(R.id.tv_album_name);
            tvImageCount = itemView.findViewById(R.id.tv_image_count);
        }
    }
}