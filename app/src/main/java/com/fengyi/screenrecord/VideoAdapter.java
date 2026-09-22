package com.fengyi.screenrecord;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {

    public interface Listener {
        void onDelete(File file);
    }

    private final Context context;
    private final List<File> files;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VideoAdapter(Context context, List<File> files, Listener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        File file = files.get(position);
        holder.tvName.setText(file.getName());
        holder.tvInfo.setText(formatInfo(file));
        holder.ivThumb.setImageBitmap(null);
        holder.ivThumb.setTag(file.getAbsolutePath());
        executor.execute(() -> {
            Bitmap bmp = extractThumbnail(file);
            mainHandler.post(() -> {
                if (holder.ivThumb.getTag() != null
                        && holder.ivThumb.getTag().equals(file.getAbsolutePath())) {
                    holder.ivThumb.setImageBitmap(bmp);
                }
            });
        });
        holder.itemView.setOnClickListener(v -> play(file));
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(file);
        });
    }

    private Bitmap extractThumbnail(File file) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(file.getAbsolutePath());
            Bitmap b = r.getFrameAtTime(1000000);
            if (b != null) {
                int w = b.getWidth(), h = b.getHeight();
                float ratio = (float) h / w;
                int targetW = 360;
                int targetH = (int) (targetW * ratio);
                return Bitmap.createScaledBitmap(b, targetW, targetH, true);
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    private String formatInfo(File file) {
        long size = file.length();
        String sizeStr;
        if (size >= 1024 * 1024) sizeStr = String.format("%.1f MB", size / 1024.0 / 1024.0);
        else sizeStr = String.format("%.0f KB", size / 1024.0);
        String time = DateUtils.getRelativeTimeSpanString(file.lastModified()).toString();
        return sizeStr + " · " + time;
    }

    private void play(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法播放：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvName;
        final TextView tvInfo;
        final View btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            tvName = itemView.findViewById(R.id.tv_name);
            tvInfo = itemView.findViewById(R.id.tv_info);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
