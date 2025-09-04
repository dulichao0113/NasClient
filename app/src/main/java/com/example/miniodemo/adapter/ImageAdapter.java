package com.example.miniodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.miniodemo.R;
import com.example.miniodemo.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private Context context;
    private List<Image> imageList;
    private OnItemClickListener listener;
    private boolean isSelectionMode = false; // 选择模式标志

    // 点击事件接口
    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    public ImageAdapter(Context context, List<Image> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // 切换选择模式
    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    // 获取选择模式状态
    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    // 切换单个项目的选择状态
    public void toggleSelection(int position) {
        if (isSelectionMode) {
            imageList.get(position).setSelected(!imageList.get(position).isSelected());
            notifyItemChanged(position);
        }
    }

    // 清除所有选择
    public void clearSelections() {
        for (Image image : imageList) {
            image.setSelected(false);
        }
        notifyDataSetChanged();
    }

    // 获取选中的图片数量
    public int getSelectedItemCount() {
        int count = 0;
        for (Image image : imageList) {
            if (image.isSelected()) {
                count++;
            }
        }
        return count;
    }

    // 获取所有选中的图片ID
    public List<Integer> getSelectedImageIds() {
        List<Integer> selectedIds = new ArrayList<>();
        for (Image image : imageList) {
            if (image.isSelected()) {
                selectedIds.add(image.getId());
            }
        }
        return selectedIds;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = imageList.get(position);

        // 显示视频/图片标识
        holder.ivVideoIndicator.setVisibility(
                "video".equals(image.getType()) ? View.VISIBLE : View.GONE
        );

        // 加载缩略图
        String loadUrl = "video".equals(image.getType())
                ? image.getUrl()
                : image.getUrl();

        // 加载图片
        Glide.with(context)
                .load(loadUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(holder.ivImage);


        // 显示或隐藏选择框
        holder.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(image.isSelected());

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (isSelectionMode) {
                    toggleSelection(position);
                } else {
                    listener.onItemClick(position);
                }
            }
        });

        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    /**
     * 更新数据（刷新列表）
     */
    public void updateData(List<Image> newImages) {
        if (newImages != null) {
            imageList.clear();
            imageList.addAll(newImages);
            notifyDataSetChanged(); // 通知列表刷新
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        CheckBox cbSelect;
        RelativeLayout container;
        ImageView ivVideoIndicator;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            cbSelect = itemView.findViewById(R.id.cb_select);
            container = itemView.findViewById(R.id.container);
            ivVideoIndicator = itemView.findViewById(R.id.iv_video_indicator);
        }
    }
}