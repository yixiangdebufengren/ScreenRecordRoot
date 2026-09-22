package com.fengyi.screenrecord;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnRecord;
    private RecyclerView recycler;
    private RecordManager recordManager;
    private VideoAdapter adapter;
    private final List<File> videos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btn_record);
        recycler = findViewById(R.id.recycler);

        recordManager = RecordManager.get(this);

        adapter = new VideoAdapter(this, videos, this::deleteVideo);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnRecord.setOnClickListener(v -> toggleRecord());

        refreshVideos();
        updateButton();
    }

    private void toggleRecord() {
        if (recordManager.isRecording()) {
            recordManager.stopRecording();
            Toast.makeText(this, "已停止录制", Toast.LENGTH_SHORT).show();
        } else {
            boolean ok = recordManager.startRecording();
            if (ok) {
                Toast.makeText(this, "录制中（screenrecord 已启动）", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "启动失败，请确认已授予 root 权限", Toast.LENGTH_LONG).show();
            }
        }
        updateButton();
    }

    private void updateButton() {
        boolean rec = recordManager.isRecording();
        btnRecord.setText(rec ? R.string.btn_stop : R.string.btn_record);
        btnRecord.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.red)));
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

    @Override
    protected void onResume() {
        super.onResume();
        refreshVideos();
        updateButton();
    }
}
