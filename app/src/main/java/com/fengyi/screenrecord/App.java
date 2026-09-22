package com.fengyi.screenrecord;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * 应用入口，在最早时机启用 Material You 动态取色。
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 动态取色（Monet）：跟随系统壁纸/主题生成配色
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
