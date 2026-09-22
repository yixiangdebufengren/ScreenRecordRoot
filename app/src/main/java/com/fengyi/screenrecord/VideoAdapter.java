package com.fengyi.screenrecord;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {

    public interface Listener {
        void onDelete(File file);
    }

    private final Context context;
    private final List<File> files;
    private final Listener listener;

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
        holder.btnPlay.setOnClickListener(v -> play(file));
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(file);
        });
    }

    private void play(File file) {
        try {
            android.net.Uri uri = FileProvider.getUriForFile(
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
        final TextView tvName;
        final View btnPlay;
        final View btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
