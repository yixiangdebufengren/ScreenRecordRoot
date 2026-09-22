package com.fengyi.screenrecord;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * screenrecord 命令的开关控制器。
 *
 * 本 APP 不参与实际的录屏，只是通过 root 启动/停止系统的 screenrecord 命令，
 * 因此可以录制息屏、任意界面。录制与否完全取决于命令本身是否在运行。
 *
 * 通过 ProcessBuilder 执行 `su -c`，兼容 Magisk / KernelSU / APatch，
 * 也兼容仅授予 ADB 权限的 root（su 可用即可）。
 */
public class RecordManager {

    private static RecordManager instance;

    private final File outputDir;

    private RecordManager(Context context) {
        // 使用应用专属外部目录，普通 App 有完整读写权限（可列出/删除），
        // root 的 screenrecord 也能写入（通过 chmod 保证），列表一定能正常显示。
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
        String out = sh("pgrep -x screenrecord");
        return out != null && !out.trim().isEmpty();
    }

    /**
     * 开始录制。用 setsid + nohup + & 让 screenrecord 完全脱离 APP 独立运行。
     */
    public boolean startRecording() {
        if (isRecording()) return true;

        // 确保目录存在且 root 可写
        sh("mkdir -p " + outputDir.getAbsolutePath()
                + " && chmod 775 " + outputDir.getAbsolutePath());

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(outputDir, "rec_" + time + ".mp4");

        // screenrecord 默认最长 3 分钟，延长到 1800 秒（30 分钟）
        String inner = "screenrecord --time-limit=1800 --bit-rate=8000000 "
                + out.getAbsolutePath();
        String cmd = "sh -c \"setsid nohup " + inner + " > /dev/null 2>&1 &\"";

        String r = sh(cmd);
        return r != null;
    }

    /** 停止录制：向 screenrecord 发 SIGINT 让视频正常写入并退出。 */
    public void stopRecording() {
        sh("pkill -INT -x screenrecord");
    }

    /** 用 root 删除文件。 */
    public boolean deleteFile(File f) {
        return sh("rm -f " + f.getAbsolutePath()) != null;
    }
}
