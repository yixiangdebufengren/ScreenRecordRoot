package com.fengyi.screenrecord;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnRecord;
    private CircularProgressIndicator progress;
    private TextView tvStatus;
    private RecyclerView recycler;
    private RecordManager recordManager;
    private VideoAdapter adapter;
    private final List<File> videos = new ArrayList<>();

    private boolean recording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 动态取色（Material You）
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btn_record);
        progress = findViewById(R.id.progress);
        tvStatus = findViewById(R.id.tv_status);
        recycler = findViewById(R.id.recycler);

        recordManager = RecordManager.get(this);

        adapter = new VideoAdapter(this, videos, new VideoAdapter.Listener() {
            @Override
            public void onDelete(File file) {
                deleteVideo(file);
            }

            @Override
            public void onExport(File file) {
                exportVideo(file);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnRecord.setOnClickListener(v -> toggleRecord());

        requestStoragePermission();

        recording = recordManager.isRecording();
        refreshVideos();
        updateUi();
    }

    /** 从当前主题解析 primary 颜色（跟随 Material You 动态取色）。 */
    private int resolveColorPrimary() {
        int color = 0xFF6750A4; // M3 默认 primary
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, tv, true)) {
                color = tv.data;
            }
        } catch (Exception ignored) {
        }
        return color;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11+ 应用专属目录无需权限；若想录到公共目录可申请 MANAGE_EXTERNAL_STORAGE
            return;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 100);
        }
    }

    private void toggleRecord() {
        if (recording) {
            // 停止录制是阻塞操作（root 命令 + sleep），放后台线程执行，避免 UI 卡顿
            btnRecord.setEnabled(false);
            tvStatus.setText("正在停止…");
            new Thread(() -> {
                recordManager.stopRecording();
                runOnUiThread(() -> {
                    btnRecord.setEnabled(true);
                    recording = recordManager.isRecording();
                    refreshVideos();
                    updateUi();
                    Toast.makeText(this, "已停止录制", Toast.LENGTH_SHORT).show();
                });
            }).start();
        } else {
            boolean ok = recordManager.startRecording();
            if (ok) {
                Toast.makeText(this, "录制中…", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "启动失败，请确认已授予 root 权限", Toast.LENGTH_LONG).show();
            }
            recording = recordManager.isRecording();
            updateUi();
        }
    }

    private void updateUi() {
        if (recording) {
            btnRecord.setIconResource(R.drawable.ic_stop);
            progress.setVisibility(android.view.View.VISIBLE);
            tvStatus.setText("正在录制 · 点击停止");
        } else {
            btnRecord.setIconResource(R.drawable.ic_record);
            progress.setVisibility(android.view.View.INVISIBLE);
            tvStatus.setText("点击开始录制");
        }
    }

    private void refreshVideos() {
        videos.clear();
        File dir = recordManager.getOutputDir();
        File[] files = dir.listFiles((f, name) -> name.endsWith(".mp4"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            videos.addAll(Arrays.asList(files));
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteVideo(File file) {
        boolean ok = recordManager.deleteFile(file);
        Toast.makeText(this, ok ? "已删除 " + file.getName() : "删除失败",
                Toast.LENGTH_SHORT).show();
        refreshVideos();
    }

    private void exportVideo(File file) {
        // 导出到 Download 目录是阻塞的 root 复制操作，放后台线程
        new Thread(() -> {
            File target = recordManager.exportToGallery(file);
            runOnUiThread(() -> {
                if (target != null) {
                    Toast.makeText(this, "已保存到相册：" + target.getName(),
                            Toast.LENGTH_SHORT).show();
                    // 触发媒体扫描，让相册立即识别新视频
                    MediaScannerConnection.scanFile(
                            this,
                            new String[]{target.getAbsolutePath()},
                            new String[]{"video/mp4"},
                            null);
                } else {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        recording = recordManager.isRecording();
        refreshVideos();
        updateUi();
    }
}
