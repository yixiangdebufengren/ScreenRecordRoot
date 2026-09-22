package com.fengyi.screenrecord;

import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 控制中心磁贴：点一下启动 screenrecord，再点一下停止。
 *
 * 关键：点击后立即（乐观）翻转磁贴状态，root 的实际启动/停止放到后台线程，
 * 避免 su 命令的阻塞导致磁贴"点了过一会儿才灭"的延迟感。
 */
public class ScreenRecordTileService extends TileService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 期望状态（乐观值），用于点击后立即刷新 UI，不等 root 命令返回
    private boolean targetRecording = false;

    @Override
    public void onStartListening() {
        super.onStartListening();
        // 进入控制中心时刷新真实状态
        executor.execute(() -> {
            boolean rec = RecordManager.get(this).isRecording();
            mainHandler.post(() -> {
                targetRecording = rec;
                renderTile(rec);
            });
        });
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        // 1. 立即翻转期望状态并刷新磁贴（乐观更新，消除延迟感）
        targetRecording = !targetRecording;
        renderTile(targetRecording);
        runInBackground(() -> {
            RecordManager manager = RecordManager.get(this);
            if (targetRecording) {
                manager.startRecording();
            } else {
                manager.stopRecording();
                // 磁贴停止后自动导出刚录好的视频到 Download，让相册可见，
                // 用户无需再进 APP 手动点导出。
                exportLatest(manager);
            }
            // 2. 等 root 命令真正完成后，校正状态，确保和实际一致
            boolean real = manager.isRecording();
            mainHandler.post(() -> {
                targetRecording = real;
                renderTile(real);
            });
        });
    }

    /**
     * 后台线程执行 root 操作，避免阻塞主线程。
     * （不能与父类的 unlockAndRun 重名，故用独立方法名）
     */
    private void runInBackground(Runnable r) {
        executor.execute(r);
    }

    /**
     * 导出刚录制的最新视频到 Download，并触发 MediaScanner 让相册识别。
     */
    private void exportLatest(RecordManager manager) {
        File latest = manager.getLatestRecording();
        if (latest == null) return;
        File exported = manager.exportToGallery(latest);
        if (exported != null) {
            // 触发系统媒体扫描，让相册立即识别导出文件
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{exported.getAbsolutePath()},
                    new String[]{"video/mp4"},
                    null);
        }
    }

    private void renderTile(boolean recording) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(recording ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(recording ? R.string.btn_stop : R.string.btn_record));
        tile.updateTile();
    }
}