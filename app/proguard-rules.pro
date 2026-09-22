# 保留通过 AndroidManifest 注册、由系统反射调用的组件
-keep class com.fengyi.screenrecord.App { *; }
-keep class com.fengyi.screenrecord.ScreenRecordTileService { *; }
-keep class com.fengyi.screenrecord.MainActivity { *; }

# 自定义 View 在 XML 布局中引用，需保留类名避免 inflate 失败
-keep class com.fengyi.screenrecord.WaveProgressView { *; }

# FileProvider 及其相关
-keep class androidx.core.content.FileProvider { *; }

# 保留 @interface 注解（避免 Material 反射报错）
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 保持 Material 组件相关
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
