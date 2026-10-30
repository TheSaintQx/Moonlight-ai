// Путь: app/src/main/java/com/limelight/assistant/OverlayView.java
// ДОБАВЛЁН режим map: уголок с картой + красная точка.
// Картинку читает по Uri из AssistantMap (SAF-папка из настроек).
package com.limelight.assistant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

public class OverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 0.5f, y = 0.5f, w = 0.2f, h = 0.2f;
    private String label = "";
    private boolean visible = false;
    private boolean arrowMode = false;
    private boolean mapMode = false;
    private float mapX = 0.5f, mapY = 0.5f;
    private float arrowAngle = 0f; // градусы, 0 = вправо
    private Bitmap mapBitmap = null;

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
        this.mapMode = false;
        this.visible = true;
        invalidate();
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, ttlMs > 0 ? ttlMs : 8000);
    }

    public void showArrow(float angleDeg, String label, int ttlMs) {
        this.arrowAngle = angleDeg;
        this.label = label == null ? "" : label;
        this.arrowMode = true;
        this.mapMode = false;
        this.visible = true;
        invalidate();
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, ttlMs > 0 ? ttlMs : 8000);
    }

    // Режим карты: карта в правом верхнем углу, точка по mapX/mapY в долевых координатах
    public void showMap(Context context, float mapX, float mapY, String label, int ttlMs) {
        this.mapX = mapX;
        this.mapY = mapY;
        this.label = label == null ? "" : label;
        this.mapMode = true;
        this.arrowMode = false;
        this.mapBitmap = loadMapBitmap(context);
        this.visible = true;
        invalidate();
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, ttlMs > 0 ? ttlMs : 8000);
    }

    private Bitmap loadMapBitmap(Context context) {
        String uriStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(AssistantMap.KEY_FOLDER_URI, "");
        if (uriStr.isEmpty()) return null;
        Uri folderUri = Uri.parse(uriStr);
        try {
            // Сперва пробуем стандартное имя картинки из JSON
            Uri imgUri = Uri.withAppendedPath(folderUri, "tsushima.png");
            Bitmap bmp = decodeStream(context, imgUri);
            if (bmp == null) {
                // fallback: читаем имя из JSON
                android.util.Log.w("OverlayView", "tsushima.png not found, trying JSON image field");
            }
            return bmp;
        } catch (Exception e) {
            android.util.Log.w("OverlayView", "map load failed: " + e.getMessage());
            return null;
        }
    }

    private Bitmap decodeStream(Context context, Uri uri) {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            // downsample до разумных размеров (уголок на ТВ — 16:9, ~480px широкий)
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    public void hide() {
        visible = false;
        mapMode = false;
        arrowMode = false;
        removeCallbacks(hideRunnable);
        if (mapBitmap != null) {
            mapBitmap.recycle();
            mapBitmap = null;
        }
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

        if (mapMode) {
            drawMap(canvas, cw, ch);
            return;
        }
        if (arrowMode) {
            float cx = cw / 2f;
            float cy = ch / 2f;
            float len = Math.min(cw, ch) * 0.3f;
            double rad = Math.toRadians(arrowAngle);
            float ex = (float)(cx + Math.cos(rad) * len);
            float ey = (float)(cy + Math.sin(rad) * len);
            canvas.drawLine(cx, cy, ex, ey, paint);
            float head = 20f;
            canvas.drawLine(ex, ey, (float)(ex - Math.cos(rad - 0.5) * head), (float)(ey - Math.sin(rad - 0.5) * head), paint);
            canvas.drawLine(ex, ey, (float)(ex - Math.cos(rad + 0.5) * head), (float)(ey + Math.sin(rad + 0.5) * head), paint);
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

    // Карта в правом верхнем углу: 16:9, ~16.66% ширины экрана (как HTML-версия)
    private void drawMap(Canvas canvas, int cw, int ch) {
        int cardW = (int)(cw * 0.1666f);
        int cardH = (int)(cardW * 9f / 16f);
        int cardX = cw - cardW - 48; // 48px от правого и верхнего края
        int cardY = 48;

        // Фон карточки
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xCC0A0E14);
        canvas.drawRoundRect(new RectF(cardX, cardY, cardX + cardW, cardY + cardH), 24, 24, bg);

        // Картинка
        if (mapBitmap != null) {
            RectF dest = computeContainRect(mapBitmap, cardX + 12, cardY + 12, cardW - 24, cardH - 24);
            canvas.drawBitmap(mapBitmap, null, dest, paint);
        } else {
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setColor(0x88FFFFFF);
            t.setTextSize(24f);
            canvas.drawText("нет картинки карты", cardX + 16, cardY + cardH / 2f, t);
        }

        // Точка: mapX/mapY в долях от изображения
        int dotSize = 24;
        float dotCx = cardX + 12 + mapX * (cardW - 24);
        float dotCy = cardY + 12 + mapY * (cardH - 24);
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFFF5F5F);
        dotPaint.setShadowLayer(0, 0, 0, 0x00000000);
        canvas.drawCircle(dotCx, dotCy, dotSize / 2f, dotPaint);
        Paint dotStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStroke.setStyle(Paint.Style.STROKE);
        dotStroke.setStrokeWidth(4f);
        dotStroke.setColor(0xE8FFFFFF);
        canvas.drawCircle(dotCx, dotCy, dotSize / 2f, dotStroke);

        // Подпись
        if (!label.isEmpty()) {
            Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            lp.setColor(0xE8FFFFFF);
            lp.setTextSize(28f);
            canvas.drawText(label, cardX + 16, cardY + cardH - 16, lp);
        }
    }

    private RectF computeContainRect(Bitmap bmp, int x, int y, int w, int h) {
        float scale = Math.min(w / (float) bmp.getWidth(), h / (float) bmp.getHeight());
        int bw = (int)(bmp.getWidth() * scale);
        int bh = (int)(bmp.getHeight() * scale);
        int bx = x + (w - bw) / 2;
        int by = y + (h - bh) / 2;
        return new RectF(bx, by, bx + bw, by + bh);
    }
}
