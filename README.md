# ScreenRecordRoot

一个 Material 3（Material You 动态取色）风格的安卓录屏应用。它本身**不参与录屏**，只是 Android 系统自带 `screenrecord` 命令的一个「开关控制器」。通过 root 权限启动/停止 `screenrecord`，因此可以录制息屏、任意界面、任何 App 的画面。

## 特性

- 🎨 Material 3 + 动态取色（Monet），随系统壁纸自动变色
- 🔴 居中大录制按钮，点击后切换为停止图标，并显示 M3 圆形进度指示器（旋转波浪）
- 📹 通过 root 执行 `screenrecord` 命令（Magisk / KernelSU / APatch，含仅 ADB 授权的 root）
- 🎛️ 控制中心磁贴：点一下开始录制，再点一下停止
- 🎞️ 录制完成后在底部显示视频列表，支持播放与删除
- 🌙 完全脱离 App 进程运行，息屏也能录

## 工作原理

- **开始录制**：以 root 执行 `screenrecord --time-limit=1800 --bit-rate=8000000 <路径>`，并用 `setsid nohup ... &` 让它完全独立运行。
- **停止录制**：`pkill -INT -x screenrecord`，让视频正常落盘。
- **状态判断**：`pgrep -x screenrecord`，录制与否完全由系统进程决定，APP 重启也不丢失状态。

视频保存在应用专属目录 `/sdcard/Android/data/com.fengyi.screenrecord/files/recordings/`（应用内列表可直接读取、播放、删除）。

## 版本号

版本号由 git 提交次数决定：`versionCode` 与 `versionName`（`1.0.<提交次数>`）在每次构建时自动生成。

## 构建与发布

项目使用 GitHub Actions 自动构建 **release 签名**（release.jks 以 Secret 形式加密存放），每次 push 到 `main` 或手动触发都会：

1. 用 release keystore 签名打包 `app-release.apk`
2. 自动创建一个 GitHub Release，tag 为 `v1.0.<提交次数>`

## 使用

1. 从 Release 页下载并安装 `app-release.apk`。
2. 授予 root 权限。
3. 打开应用点中间按钮，或把「屏幕录制」磁贴拖到控制中心。
4. 点击开始 / 停止录制，在应用底部查看、播放或删除录好的视频。

## License

MIT
