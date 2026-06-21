package com.shouzhong.scanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerView extends FrameLayout implements Camera.PreviewCallback, CameraPreview.FocusAreaSetter {

    public static final String TAG = "ScannerView";

    private CameraWrapper cameraWrapper;
    private IViewFinder viewFinder;
    private CameraPreview cameraPreview;
    private Rect scaledRect;
    private ArrayList<Camera.Area> focusAreas;
    private CameraHandlerThread cameraHandlerThread;
    private boolean shouldAdjustFocusArea;
    private MultiFormatReader multiFormatReader;
    private Callback callback;
    private int cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int[] previewSize;
    private boolean isSaveBmp = false;
    private boolean isRotateDegree90Recognition = false;
    private boolean enableZXing = false;
    private boolean enableQrcode = true;
    private boolean enableBarcode = true;
    private Map<DecodeHintType, Object> hints0;

    public ScannerView(Context context) { this(context, null); }
    public ScannerView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public ScannerView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (callback == null) return;
        try {
            int previewWidth = previewSize[0];
            int previewHeight = previewSize[1];
            int rotationCount = getRotationCount();
            boolean isRotated = rotationCount == 1 || rotationCount == 3;

            Rect rect = getScaledRect(previewWidth, previewHeight);
            int w = rect.width() / 2 * 2;
            int h = rect.height() / 2 * 2;
            byte[] temp = Utils.clipNV21(data, previewWidth, previewHeight, rect.left, rect.top, w, h);
            if (temp == null) { getOneMoreFrame(); return; }

            byte[] tempData;
            int width, height;
            byte[] tempData2 = null;
            int width2 = 0, height2 = 0;

            if (isRotated) {
                tempData = Utils.rotateNV21Degree90(temp, w, h);
                width = h;
                height = w;
                if (isRotateDegree90Recognition) {
                    tempData2 = temp;
                    width2 = w;
                    height2 = h;
                }
            } else {
                tempData = temp;
                width = w;
                height = h;
                if (isRotateDegree90Recognition) {
                    tempData2 = Utils.rotateNV21Degree90(temp, w, h);
                    width2 = h;
                    height2 = w;
                }
            }

            String resultText = null;
            if (enableZXing && (enableBarcode || enableQrcode)) {
                try {
                    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(tempData, width, height, 0, 0, width, height, false);
                    resultText = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints0).getText();
                } catch (Exception e) {
                    // ZXing decode failed
                }
                if (resultText == null && isRotateDegree90Recognition) {
                    try {
                        PlanarYUVLuminanceSource source2 = new PlanarYUVLuminanceSource(tempData2, width2, height2, 0, 0, width2, height2, false);
                        resultText = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source2)), hints0).getText();
                    } catch (Exception e) {
                        // ZXing decode failed on rotated
                    }
                }
            }

            if (resultText == null) {
                getOneMoreFrame();
                return;
            }

            final Result result = new Result();
            result.type = Result.TYPE_CODE;
            result.data = resultText;

            post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) callback.result(result);
                }
            });
        } catch (Exception e) {
            getOneMoreFrame();
        }
    }

    @Override
    public void setAutoFocusArea() {
        if (!shouldAdjustFocusArea || cameraWrapper == null) return;
        try {
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            if (parameters == null || parameters.getMaxNumFocusAreas() <= 0) return;
            if (focusAreas == null) {
                int width = 2000, height = 2000;
                Rect framingRect = viewFinder.getFramingRect();
                if (framingRect == null) return;
                int w = getWidth();
                int h = getHeight();
                Rect scaledRect = new Rect(framingRect);
                scaledRect.left = scaledRect.left * width / w;
                scaledRect.right = scaledRect.right * width / w;
                scaledRect.top = scaledRect.top * height / h;
                scaledRect.bottom = scaledRect.bottom * height / h;
                Rect rotatedRect = new Rect(scaledRect);
                int rotationCount = getRotationCount();
                if (rotationCount == 1) {
                    rotatedRect.left = scaledRect.top;
                    rotatedRect.top = 2000 - scaledRect.right;
                    rotatedRect.right = scaledRect.bottom;
                    rotatedRect.bottom = 2000 - scaledRect.left;
                } else if (rotationCount == 2) {
                    rotatedRect.left = 2000 - scaledRect.right;
                    rotatedRect.top = 2000 - scaledRect.bottom;
                    rotatedRect.right = 2000 - scaledRect.left;
                    rotatedRect.bottom = 2000 - scaledRect.top;
                } else if (rotationCount == 3) {
                    rotatedRect.left = 2000 - scaledRect.bottom;
                    rotatedRect.top = scaledRect.left;
                    rotatedRect.right = 2000 - scaledRect.top;
                    rotatedRect.bottom = scaledRect.right;
                }
                Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);
                focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(rect, 1000));
            }
            parameters.setFocusAreas(focusAreas);
            cameraWrapper.camera.setParameters(parameters);
        } catch (Exception e) {}
    }

    // ── Public API ──────────────────────────────────────────────────────

    public void setCallback(Callback callback) { this.callback = callback; }
    public void setViewFinder(IViewFinder viewFinder) { this.viewFinder = viewFinder; }
    public void onResume() { startCamera(); }
    public void onPause() { stopCamera(); }
    public void setEnableZXing(boolean boo) { this.enableZXing = boo; }
    public void setEnableQrcode(boolean boo) { if (boo == enableQrcode) return; this.enableQrcode = boo; hints0 = null; }
    public void setEnableBarcode(boolean boo) { if (boo == enableBarcode) return; this.enableBarcode = boo; hints0 = null; }
    public void setSaveBmp(boolean boo) { this.isSaveBmp = boo; }
    public void setRotateDegree90Recognition(boolean boo) { this.isRotateDegree90Recognition = boo; }
    public void setShouldAdjustFocusArea(boolean boo) { this.shouldAdjustFocusArea = boo; }

    public void restartPreviewAfterDelay(long delayMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                getOneMoreFrame();
            }
        }, delayMillis);
    }

    // ── Internal ────────────────────────────────────────────────────────

    private void getOneMoreFrame() {
        if (cameraWrapper != null) {
            try { cameraWrapper.camera.setOneShotPreviewCallback(this); } catch (Exception e) {}
        }
    }

    private synchronized MultiFormatReader getMultiFormatReader() {
        if (multiFormatReader == null) multiFormatReader = new MultiFormatReader();
        if (hints0 == null) {
            hints0 = new HashMap<>();
            List<BarcodeFormat> decodeFormats = new ArrayList<>();
            if (enableQrcode) decodeFormats.add(BarcodeFormat.QR_CODE);
            if (enableBarcode) {
                decodeFormats.add(BarcodeFormat.CODABAR);
                decodeFormats.add(BarcodeFormat.CODE_39);
                decodeFormats.add(BarcodeFormat.CODE_93);
                decodeFormats.add(BarcodeFormat.CODE_128);
                decodeFormats.add(BarcodeFormat.EAN_8);
                decodeFormats.add(BarcodeFormat.EAN_13);
                decodeFormats.add(BarcodeFormat.UPC_A);
                decodeFormats.add(BarcodeFormat.UPC_E);
                decodeFormats.add(BarcodeFormat.ITF);
                decodeFormats.add(BarcodeFormat.RSS_14);
            }
            hints0.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints0.put(DecodeHintType.CHARACTER_SET, "utf-8");
        }
        return multiFormatReader;
    }

    void setCameraWrapper(CameraWrapper cw) { this.cameraWrapper = cw; }

    void setupCameraPreview() {
        if (this.cameraWrapper == null) return;
        removeAllViews();
        cameraPreview = new CameraPreview(getContext(), previewSize[0], previewSize[1], cameraWrapper, this, this);
        addView(cameraPreview);
        if (viewFinder instanceof View) addView((View) viewFinder);
    }

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
                cameraPreview = null;
                cameraWrapper.camera.release();
                cameraWrapper = null;
            } catch (Exception e) {}
        }
        previewSize = null;
        scaledRect = null;
        focusAreas = null;
        multiFormatReader = null;
        removeAllViews();
    }

    /**
     * Scale viewFinder rect to preview frame coordinates, then rotate based on camera rotation.
     */
    private Rect getScaledRect(int previewWidth, int previewHeight) {
        if (scaledRect == null) {
            Rect framingRect = viewFinder.getFramingRect();
            int w = getWidth();
            int h = getHeight();
            scaledRect = new Rect(framingRect);

            Point p = new Point();
            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(p);
            int o = p.x == p.y ? 0 : p.x < p.y ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            float ratio = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight * 1f / previewWidth : previewWidth * 1f / previewHeight;
            float r = w * 1f / h;

            if (ratio < r) {
                int width = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight : previewWidth;
                scaledRect.left = scaledRect.left * width / w;
                scaledRect.right = scaledRect.right * width / w;
                scaledRect.top = scaledRect.top * width / w;
                scaledRect.bottom = scaledRect.bottom * width / w;
            } else {
                int height = o == Configuration.ORIENTATION_PORTRAIT ? previewWidth : previewHeight;
                scaledRect.left = scaledRect.left * height / h;
                scaledRect.right = scaledRect.right * height / h;
                scaledRect.top = scaledRect.top * height / h;
                scaledRect.bottom = scaledRect.bottom * height / h;
            }

            int rotationCount = getRotationCount();
            int left = scaledRect.left, top = scaledRect.top, right = scaledRect.right, bottom = scaledRect.bottom;
            if (rotationCount == 1) {
                scaledRect.left = top;
                scaledRect.top = previewHeight - right;
                scaledRect.right = bottom;
                scaledRect.bottom = previewHeight - left;
            } else if (rotationCount == 2) {
                scaledRect.left = previewWidth - right;
                scaledRect.top = previewHeight - bottom;
                scaledRect.right = previewWidth - left;
                scaledRect.bottom = previewHeight - top;
            } else if (rotationCount == 3) {
                scaledRect.left = previewWidth - bottom;
                scaledRect.top = left;
                scaledRect.right = previewWidth - top;
                scaledRect.bottom = right;
            }

            if (scaledRect.left < 0) scaledRect.left = 0;
            if (scaledRect.top < 0) scaledRect.top = 0;
            if (scaledRect.right > previewWidth) scaledRect.right = previewWidth;
            if (scaledRect.bottom > previewHeight) scaledRect.bottom = previewHeight;
        }
        return scaledRect;
    }

    private int getRotationCount() {
        return cameraPreview == null ? 0 : cameraPreview.getDisplayOrientation() / 90;
    }

    void setOptimalPreviewSize() {
        if (previewSize != null || cameraWrapper == null) return;
        int w, h;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            w = getMeasuredWidth();
            h = getMeasuredHeight();
        } else {
            w = getMeasuredHeight();
            h = getMeasuredWidth();
        }
        try {
            List<Camera.Size> sizes = cameraWrapper.camera.getParameters().getSupportedPreviewSizes();
            if (sizes == null) throw new NullPointerException();
            double targetRatio = w * 1.0 / h;
            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;
            double aspectTolerance = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                double ratio = size.width * 1.0 / size.height;
                if (Math.abs(ratio - targetRatio) > aspectTolerance) continue;
                if (Math.abs(size.height - h) <= minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                    aspectTolerance = Math.abs(ratio - targetRatio);
                }
            }
            previewSize = new int[]{optimalSize.width, optimalSize.height};
        } catch (Exception e) {
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int width = dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;
            int height = dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels;
            previewSize = w * 1.0f / h > 1.0f ? new int[]{width, height} : new int[]{height, width};
        }
    }
}
