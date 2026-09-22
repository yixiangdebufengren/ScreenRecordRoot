package com.fengyi.screenrecord;

import android.content.Context;
import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * screenrecord 命令的开关控制器。
 *
 * 本 APP 不参与实际的录屏，只是通过 root 启动/停止系统的 screenrecord 命令。
 * 录制与否完全取决于命令本身是否在运行，因此可录制息屏、任意界面。
 *
 * 关键点：
 * 1. screenrecord 以 root 运行，写出的文件 owner 是 root、权限 600，
 *    APP（普通 uid）无法读取，所以停止时必须 chown/chmod 让 APP 可读，
 *    否则视频无法播放、也无法提取缩略图。
 * 2. 停止必须发 SIGINT（而非 SIGKILL），screenrecord 收到 SIGINT 才会把
 *    moov 索引写到文件末尾，否则文件头缺失、播放器无法解析。
 */
public class RecordManager {

    private static RecordManager instance;

    private final File outputDir;
    private final int appUid;

    private RecordManager(Context context) {
        appUid = Process.myUid();
        File ext = context.getExternalFilesDir(null);
        outputDir = new File(ext, "recordings");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    public static synchronized RecordManager get(Context context) {
        if (instance == null) {
            instance = new RecordManager(context.getApplicationContext());
        }
        return instance;
    }

    public File getOutputDir() {
        return outputDir;
    }

    /** 用 root 权限执行命令，返回 stdout（异常返回 null）。 */
    private String sh(String cmd) {
        try {
            Process p = new ProcessBuilder("su", "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 当前是否正在录制：检查系统里有没有 screenrecord 进程。 */
    public boolean isRecording() {
        String exact = sh("pgrep -x screenrecord");
        return exact != null && !exact.trim().isEmpty();
    }

    /**
     * 开始录制。用 setsid + nohup 让 screenrecord 完全脱离 APP 独立运行，
     * 并把 PID 写入 pidfile，便于停止时精准发 SIGINT。
     */
    public boolean startRecording() {
        if (isRecording()) return true;

        // 确保目录存在且 root 可写
        sh("mkdir -p " + outputDir.getAbsolutePath()
                + " && chmod 775 " + outputDir.getAbsolutePath());

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(outputDir, "rec_" + time + ".mp4");
        File pidFile = new File(outputDir, ".record_pid");

        // 启动 screenrecord，把 PID 落到 pidfile（$! 是后台进程 PID）
        String inner = "screenrecord --time-limit=1800 --bit-rate=8000000 "
                + out.getAbsolutePath();
        String cmd = "sh -c \"setsid nohup " + inner
                + " > /dev/null 2>&1 & echo $! > " + pidFile.getAbsolutePath() + "\"";

        String r = sh(cmd);

        // 让 screenrecord 写出的文件对 APP 可读（启动后立即放宽目录即可，
        // 文件本身的 chmod 在停止时统一处理）
        sh("chmod 775 " + outputDir.getAbsolutePath());

        return r != null || isRecording();
    }

    /**
     * 停止录制：向 screenrecord 发 SIGINT，让它把 moov 索引写入文件并正常退出。
     * 之后把文件权限放宽，让 APP 可以读取、提取缩略图、播放。
     */
    public void stopRecording() {
        // 发 SIGINT 停止 screenrecord，等待其写 moov 头退出后，
        // 放宽文件权限让 APP 可读（root 写出的文件默认 600，APP 读不了）。
        // 统一在一条 root 命令里完成，减少多次 su 调用的延迟。
        sh("pkill -INT -x screenrecord 2>/dev/null; "
                + "sleep 2; "
                + "pkill -INT -x screenrecord 2>/dev/null; "
                + "chmod 664 " + outputDir.getAbsolutePath() + "/*.mp4 2>/dev/null; "
                + "chown " + appUid + ":" + appUid + " "
                + outputDir.getAbsolutePath() + "/*.mp4 2>/dev/null; "
                + "rm -f " + outputDir.getAbsolutePath() + "/.record_pid 2>/dev/null");
    }

    /** 用 root 删除文件。 */
    public boolean deleteFile(File f) {
        return sh("rm -f " + f.getAbsolutePath()) != null;
    }
}