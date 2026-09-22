# ScreenRecordRoot

一个 Material 3 风格的安卓录屏应用。它本身**不参与录屏**，只是 Android 系统自带 `screenrecord` 命令的一个「开关控制器」。通过 root 权限启动/停止 `screenrecord`，因此可以录制息屏、任意界面、任何 App 的画面。

## 特性

- 🔴 Material 3 风格，中间一个大录制按钮
- 📹 通过 root 执行 `screenrecord` 命令（Magisk / KernelSU，含仅 ADB 授权的 root）
- 🎛️ 控制中心磁贴（Quick Settings Tile）：点一下开始录制，再点一下停止
- 🎞️ 录制完成后在底部显示视频列表，支持播放与删除
- 🌙 完全脱离 App 进程运行，息屏也能录

## 工作原理

- **开始录制**：以 root 执行 `screenrecord --time-limit=1800 --bit-rate=8000000 <输出路径>`，并用 `setsid nohup ... &` 让它完全独立运行。
- **停止录制**：`pkill -INT -x screenrecord`，让视频正常落盘。
- **状态判断**：`pgrep -x screenrecord`，录制与否完全由系统进程决定，APP 重启也不丢失状态。

视频保存到 `/sdcard/Movies/ScreenRecordRoot/`。

## 构建

项目使用 GitHub Actions 自动构建，每次 push 到 `main` 或手动触发都会产出一个 debug APK。

本地构建：

```bash
gradle assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 使用

1. 安装 APK，授予 root 权限。
2. 打开应用点中间按钮，或把「屏幕录制」磁贴拖到控制中心。
3. 点击开始 / 停止录制。
4. 在应用底部查看、播放或删除录好的视频。

## License

MIT
