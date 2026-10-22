package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Extracts a readable accent from a wallpaper once when it is decoded. */
final class AdaptivePalette {
    private AdaptivePalette() {}
    static int accent(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return 0xFF4BDCA5;
        long r = 0, g = 0, b = 0, count = 0;
        int sx = Math.max(1, bitmap.getWidth() / 32), sy = Math.max(1, bitmap.getHeight() / 24);
        float[] hsv = new float[3];
        for (int y = sy / 2; y < bitmap.getHeight(); y += sy) for (int x = sx / 2; x < bitmap.getWidth(); x += sx) {
            int p = bitmap.getPixel(x, y);
            if (Color.alpha(p) < 160) continue;
            Color.colorToHSV(p, hsv);
            if (hsv[2] < 0.18f || hsv[2] > 0.98f) continue;
            int w = 1 + Math.round(hsv[1] * 3f);
            r += Color.red(p) * w; g += Color.green(p) * w; b += Color.blue(p) * w; count += w;
        }
        if (count == 0) return 0xFF4BDCA5;
        Color.RGBToHSV((int)(r/count), (int)(g/count), (int)(b/count), hsv);
        hsv[1] = Math.max(0.42f, Math.min(0.78f, hsv[1] * 1.12f));
        hsv[2] = Math.max(0.72f, Math.min(0.92f, hsv[2]));
        return 0xFF000000 | (Color.HSVToColor(hsv) & 0x00FFFFFF);
    }
}
