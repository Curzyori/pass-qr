package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

public class DefaultViewFinder extends View implements IViewFinder {
    private Rect framingRect;
    private final float widthRatio = 0.7f;
    private final int maskColor = 0x80000000;
    private final int borderColor = 0xFFFF5722;
    private final int borderStrokeWidth = 10;
    private final int borderLineLength = 50;
    private final Paint maskPaint;
    private final Paint borderPaint;

    public DefaultViewFinder(Context context) {
        super(context);
        setWillNotDraw(false);
        maskPaint = new Paint();
        maskPaint.setColor(maskColor);
        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderStrokeWidth);
        borderPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int size = (int) (w * widthRatio);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        framingRect = new Rect(left, top, left + size, top + size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (framingRect == null) return;
        drawMask(canvas);
        drawBorder(canvas);
    }

    private void drawMask(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        canvas.drawRect(0, 0, w, framingRect.top, maskPaint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);
        canvas.drawRect(framingRect.right, framingRect.top, w, framingRect.bottom, maskPaint);
        canvas.drawRect(0, framingRect.bottom, w, h, maskPaint);
    }

    private void drawBorder(Canvas canvas) {
        Path path = new Path();
        path.moveTo(framingRect.left, framingRect.top + borderLineLength);
        path.lineTo(framingRect.left, framingRect.top);
        path.lineTo(framingRect.left + borderLineLength, framingRect.top);
        path.moveTo(framingRect.right, framingRect.top + borderLineLength);
        path.lineTo(framingRect.right, framingRect.top);
        path.lineTo(framingRect.right - borderLineLength, framingRect.top);
        path.moveTo(framingRect.right, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.right, framingRect.bottom);
        path.lineTo(framingRect.right - borderLineLength, framingRect.bottom);
        path.moveTo(framingRect.left, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.left, framingRect.bottom);
        path.lineTo(framingRect.left + borderLineLength, framingRect.bottom);
        canvas.drawPath(path, borderPaint);
    }

    @Override
    public Rect getFramingRect() { return framingRect; }
}
