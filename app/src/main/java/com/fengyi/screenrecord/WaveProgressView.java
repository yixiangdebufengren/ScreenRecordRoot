package com.fengyi.screenrecord;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * 自定义"波浪线圆环"进度视图。
 *
 * 区别于 com.google.android.material.progressindicator.CircularProgressIndicator
 * （后者只是一段光滑圆弧在圆周上平移），本视图绘制一个半径随角度按正弦规律
 * 起伏（波浪形）的闭合圆环，并整体旋转，实现"波浪线围成的圆圈在转"。
 */
public class WaveProgressView extends View {

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 基础半径（不含笔画宽度），由 onSizeChanged 计算
    private float baseRadius = 0f;
    // 波浪振幅（半径起伏的幅度）
    private float amplitude = 0f;
    // 一圈上的波峰数量
    private int waveCount = 8;

    // 当前旋转角度（弧度）
    private float rotation = 0f;

    private ValueAnimator animator;

    public WaveProgressView(Context context) {
        super(context);
        init();
    }

    public WaveProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        setColor(0xFF6750A4); // M3 默认 primary 紫色，可被 setColor 覆盖
    }

    /** 设置波浪线颜色 */
    public void setColor(int color) {
        strokePaint.setColor(color);
        invalidate();
    }

    /** 设置笔画宽度（dp） */
    public void setStrokeWidth(float dp) {
        strokePaint.setStrokeWidth(dp * getResources().getDisplayMetrics().density);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float strokeW = strokePaint.getStrokeWidth();
        float minSide = Math.min(w, h);
        // 圆环外边缘留出半个笔画宽，避免被裁剪
        baseRadius = minSide / 2f - strokeW / 2f - dp(2);
        amplitude = Math.max(3f, minSide * 0.05f); // 振幅约 5% 边长
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (baseRadius <= 0) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        Path path = new Path();
        // 用足够多的采样点逼近正弦起伏的闭合环
        int steps = 360;
        for (int i = 0; i <= steps; i++) {
            float angle = (float) (i * 2 * Math.PI / steps);
            // 半径 = 基础半径 + 振幅 * sin(波数 * 角度)
            float r = baseRadius + amplitude * (float) Math.sin(waveCount * angle);
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();

        canvas.save();
        // 整体旋转，实现"波浪线圆圈在转"
        canvas.rotate((float) Math.toDegrees(rotation), cx, cy);
        canvas.drawPath(path, strokePaint);
        canvas.restore();
    }

    /** 开始旋转动画 */
    public void start() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
            animator.setDuration(2000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                rotation = (float) a.getAnimatedValue();
                invalidate();
            });
        }
        if (!animator.isStarted()) {
            animator.start();
        }
    }

    /** 停止旋转动画 */
    public void stop() {
        if (animator != null) {
            animator.cancel();
        }
        rotation = 0f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }
}
