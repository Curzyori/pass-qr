package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

public class ScannerUtils {
    public static String decodeCode(byte[] data, int width, int height) {
        try {
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();
            return reader.decode(bitmap).getText();
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int width, int height) {
        byte[] rotated = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rotated[x * height + (height - 1 - y)] = data[y * width + x];
            }
        }
        return rotated;
    }
}
