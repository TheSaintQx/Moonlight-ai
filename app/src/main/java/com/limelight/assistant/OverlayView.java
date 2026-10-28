package com.limelight.assistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 0.5f, y = 0.5f, w = 0.2f, h = 0.2f;
    private String label = "";
    private boolean visible = false;
    private boolean arrowMode = false;
    private float arrowAngle = 0f; // градусы, 0 = вправо

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        setClickable(false);
        setFocusable(false);
    }

    public void show(float x, float y, float w, float h, String label, int ttlMs) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.label = label == null ? "" : label;
        this.arrowMode = false;
        this.visible = true;
        invalidate();
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, ttlMs > 0 ? ttlMs : 8000);
    }

    public void showArrow(float angleDeg, String label, int ttlMs) {
        this.arrowAngle = angleDeg;
        this.label = label == null ? "" : label;
        this.arrowMode = true;
        this.visible = true;
        invalidate();
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, ttlMs > 0 ? ttlMs : 8000);
    }

    public void hide() {
        visible = false;
        removeCallbacks(hideRunnable);
        invalidate();
    }

    private final Runnable hideRunnable = this::hide;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!visible) return;
        int cw = getWidth();
        int ch = getHeight();
        if (cw == 0 || ch == 0) return;
        if (arrowMode) {
            // рисуем стрелку в центре экрана под углом arrowAngle
            float cx = cw / 2f;
            float cy = ch / 2f;
            float len = Math.min(cw, ch) * 0.3f;
            double rad = Math.toRadians(arrowAngle);
            float ex = (float)(cx + Math.cos(rad) * len);
            float ey = (float)(cy + Math.sin(rad) * len);
            canvas.drawLine(cx, cy, ex, ey, paint);
            // наконечник
            float head = 20f;
            canvas.drawLine(ex, ey, (float)(ex - Math.cos(rad - 0.5) * head), (float)(ey - Math.sin(rad - 0.5) * head), paint);
            canvas.drawLine(ex, ey, (float)(ex - Math.cos(rad + 0.5) * head), (float)(ey - Math.sin(rad + 0.5) * head), paint);
            if (!label.isEmpty()) {
                Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(32f);
                canvas.drawText(label, cx - 40, cy - len - 8, textPaint);
            }
            return;
        }
        float cx = x * cw;
        float cy = y * ch;
        float rw = w * cw;
        float rh = h * ch;
        RectF rect = new RectF(cx - rw / 2f, cy - rh / 2f, cx + rw / 2f, cy + rh / 2f);
        canvas.drawOval(rect, paint);
        if (!label.isEmpty()) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(32f);
            canvas.drawText(label, cx - rw / 2f, cy - rh / 2f - 8f, textPaint);
        }
    }
}
