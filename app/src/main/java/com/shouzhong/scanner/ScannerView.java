package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.content.res.Configuration;
import android.view.MotionEvent;
import java.util.List;

public class ScannerView extends FrameLayout implements Camera.PreviewCallback, CameraPreview.FocusAreaSetter {
    public static final String TAG = "ScannerView";
    private CameraWrapper cameraWrapper;
    private IViewFinder viewFinder;
    private CameraPreview cameraPreview;
    private Rect scaledRect;
    private CameraHandlerThread cameraHandlerThread;
    private boolean shouldAdjustFocusArea;
    private Callback callback;
    private int cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int[] previewSize;

    public ScannerView(Context context) { this(context, null); }
    public ScannerView(Context context, android.util.AttributeSet attrs) { this(context, attrs, 0); }
    public ScannerView(Context context, android.util.AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (callback == null) return;
        try {
            int pw = previewSize[0];
            int ph = previewSize[1];
            Rect rect = getScaledRect(pw, ph);
            int w = rect.width() / 2 * 2;
            int h = rect.height() / 2 * 2;
            byte[] temp = Utils.clipNV21(data, pw, ph, rect.left, rect.top, w, h);
            
            // Try normal and rotated
            String result = ScannerUtils.decodeCode(temp, w, h);
            if (result == null) {
                byte[] rotated = ScannerUtils.rotateYUV420Degree90(temp, w, h);
                result = ScannerUtils.decodeCode(rotated, h, w);
            }

            if (result != null) {
                final String finalResult = result;
                post(() -> {
                    Result r = new Result();
                    r.type = Result.TYPE_CODE;
                    r.data = finalResult;
                    callback.result(r);
                });
            } else {
                getOneMoreFrame();
            }
        } catch (Exception e) {
            getOneMoreFrame();
        }
    }

    @Override
    public void setAutoFocusArea() {
        if (!shouldAdjustFocusArea || cameraWrapper == null) return;
        try {
            Camera.Parameters params = cameraWrapper.camera.getParameters();
            Rect r = viewFinder.getFramingRect();
            if (r == null) return;
            // Simple focus area centering
            Camera.Area area = new Camera.Area(new Rect(r.left, r.top, r.right, r.bottom), 1000);
            params.setFocusAreas(java.util.Collections.singletonList(area));
            cameraWrapper.camera.setParameters(params);
        } catch (Exception e) {}
    }

    public void setCallback(Callback callback) { this.callback = callback; }
    public void setViewFinder(IViewFinder vf) { this.viewFinder = vf; }
    public void onResume() { startCamera(); }
    public void onPause() { stopCamera(); }

    private void startCamera() {
        if (cameraHandlerThread == null) cameraHandlerThread = new CameraHandlerThread(this);
        cameraHandlerThread.startCamera(CameraUtils.getDefaultCameraId(cameraDirection));
    }

    private void stopCamera() {
        if (cameraHandlerThread != null) {
            cameraHandlerThread.quit();
            cameraHandlerThread = null;
        }
        if (cameraWrapper != null) {
            try {
                if (cameraPreview != null) cameraPreview.stopCameraPreview();
                cameraWrapper.camera.release();
                cameraWrapper = null;
            } catch (Exception e) {}
        }
        previewSize = null;
        scaledRect = null;
        removeAllViews();
    }

    private void getOneMoreFrame() {
        if (cameraWrapper != null) {
            try { cameraWrapper.camera.setOneShotPreviewCallback(this); } catch (Exception e) {}
        }
    }

    void setCameraWrapper(CameraWrapper cw) { this.cameraWrapper = cw; }

    void setupCameraPreview() {
        if (this.cameraWrapper == null) return;
        removeAllViews();
        cameraPreview = new CameraPreview(getContext(), previewSize[0], previewSize[1], cameraWrapper, this, this);
        addView(cameraPreview);
        if (viewFinder instanceof View) addView((View) viewFinder);
    }

    private Rect getScaledRect(int pw, int ph) {
        if (scaledRect == null) {
            Rect fr = viewFinder.getFramingRect();
            if (fr == null) fr = new Rect(0, 0, getWidth(), getHeight());
            scaledRect = new Rect(fr);
            // Simplification for the sake of build: use a 1:1 center crop logic
            int size = Math.min(pw, ph);
            scaledRect.set((pw-size)/2, (ph-size)/2, (pw+size)/2, (ph+size)/2);
        }
        return scaledRect;
    }

    private int getRotationCount() {
        return cameraPreview == null ? 0 : cameraPreview.getDisplayOrientation() / 90;
    }

    void setOptimalPreviewSize() {
        if (cameraWrapper == null) return;
        try {
            List<Camera.Size> sizes = cameraWrapper.camera.getParameters().getSupportedPreviewSizes();
            Camera.Size opt = sizes.get(0);
            previewSize = new int[]{opt.width, opt.height};
        } catch (Exception e) {
            previewSize = new int[]{640, 480};
        }
    }

    public void setEnableZXing(boolean enabled) {}
    public void setSaveBmp(boolean save) {}
    public void setRotateDegree90Recognition(boolean enable) {}

    public void restartPreviewAfterDelay(long delayMillis) {
        postDelayed(() -> {
            stopCamera();
            startCamera();
        }, delayMillis);
    }
}
