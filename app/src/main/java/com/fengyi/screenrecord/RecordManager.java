package com.fengyi.screenrecord;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.libsu.core.Shell;

/**
 * screenrecord 命令的开关控制器。
 *
 * 本 APP 不参与实际的录屏，只是通过 root 启动/停止系统的 screenrecord 命令。
 * 因此可以录制息屏、任意界面，录制与否完全取决于命令本身是否在运行。
 */
public class RecordManager {

    private static RecordManager instance;

    private final File outputDir;

    private RecordManager(Context context) {
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        outputDir = new File(moviesDir, "ScreenRecordRoot");
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

    /**
     * 用 root 执行任意 shell 命令，返回结果文本（失败返回 null）。
     */
    private String sh(String... cmds) {
        try {
            Shell.Result r = Shell.getShell().newJob().add(cmds).exec();
            if (r.isSuccess()) {
                return String.join("\n", r.getOut());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 当前是否正在录制：直接检查系统里有没有 screenrecord 进程。
     */
    public boolean isRecording() {
        String out = sh("pgrep -x screenrecord");
        return out != null && !out.trim().isEmpty();
    }

    /**
     * 开始录制。screenrecord 是阻塞命令，用后台方式挂到 root 守护进程。
     * 用 setsid + 输出重定向 + & 彻底脱离，即使 APP 被杀死命令也继续跑。
     */
    public boolean startRecording() {
        if (isRecording()) return true;

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(outputDir, "rec_" + time + ".mp4");

        // 确保目录存在
        sh("mkdir -p " + outputDir.getAbsolutePath(),
           "chmod 777 " + outputDir.getAbsolutePath());

        // screenrecord 默认最长 3 分钟，这里延长到 1800 秒（30 分钟）
        // setsid + nohup + & 让命令完全脱离 APP 进程独立运行
        String cmd = "setsid nohup screenrecord "
                + "--time-limit=1800 "
                + "--bit-rate=8000000 "
                + out.getAbsolutePath()
                + " > /dev/null 2>&1 &";

        String r = sh(cmd);
        // sh 返回 null 表示执行失败；即使返回空字符串也表示命令已提交
        return r != null;
    }

    /**
     * 停止录制：向 screenrecord 发 SIGINT，让它把视频正常写入并退出。
     */
    public void stopRecording() {
        sh("pkill -INT -x screenrecord");
    }

    /** 强制杀掉（兜底），视频可能不完整。 */
    public void killRecording() {
        sh("pkill -9 -x screenrecord");
    }

    /** 用 root 删除文件。 */
    public boolean deleteFile(File f) {
        return sh("rm -f " + f.getAbsolutePath()) != null;
    }
}
