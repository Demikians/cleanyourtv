package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

/** Displays and rotates a theme's local wallpaper images while Home is visible. */
final class WallpaperPlayer {
    private final FrameLayout root;
    private final ImageView image;
    private final List<File> files;
    private final long interval;
    private final String order;
    private final SharedPreferences preferences;
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final int width;
    /** config wallpaper_true_color: ARGB_8888 instead of RGB_565. */
    private final Bitmap.Config bitmapConfig;
    private final int height;
    /** wallpaper_dim as an ARGB black, painted into each picture once instead of a full-screen shade view. */
    private final int dimColor;
    /** The picture on screen, released once the next one has been drawn. */
    private Bitmap current;
    private boolean active;
    private boolean closed;
    private boolean loading;
    private String displayed = "";
    /** wallpaper_captions: the shown picture's name from wallpapers/captions.txt; null when off or there are none. */
    private final TextView caption;
    private final Map<String, String> names;
    private final IntConsumer paletteListener;

    WallpaperPlayer(Context context, FrameLayout root, Config config, IntConsumer paletteListener) {
        this.paletteListener = paletteListener;
        this.root = root;
        files = new ArrayList<>(config.wallpapers);
        // The theme's photo collection is optional. A clean install still gets a set of locally
        // generated, low-memory landscape wallpapers instead of a blank black screen.
        if (files.isEmpty() && config.wallpaperEnabled) files.addAll(createFallbackWallpapers(context));
        interval = config.wallpaperRotationMinutes * 60_000L;
        bitmapConfig = config.wallpaperTrueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        // The dim is painted into the picture (decode), so no full-screen shade layer is drawn on every frame.
        dimColor = config.wallpaperDim > 0 ? (config.wallpaperDim * 255 / 100) << 24 : 0;
        order = config.wallpaperOrder;
        preferences = context.getSharedPreferences("wallpaper_rotation", Context.MODE_PRIVATE);
        // Playback state belongs to one collection. Changing the collection starts from its first
        // picture instead of resuming a filename that belonged to the previous one.
        String collection = collectionKey(files);
        if (!collection.equals(preferences.getString("collection", ""))) {
            preferences.edit().putString("collection", collection)
                    .remove("image").remove("changed").apply();
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        names = config.wallpaperCaptions && config.wallpaperEnabled && !files.isEmpty()
                ? WallpaperCaptions.read(files.get(0).getParentFile()) : Collections.emptyMap();
        caption = names.isEmpty() ? null : makeCaption(context, root);
        if (config.wallpaperEnabled && !files.isEmpty()) {
            root.addView(image, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            showFirst();
        }
    }

    /**
     * The picture Home opens with is decoded right here, on the main thread, as the single wallpaper always was:
     * Home's first frame already has it instead of drawing the plain colour and the picture popping in later.
     * Only the later changes of a rotation decode in the background.
     */
    private void showFirst() {
        String previous = preferences.getString("image", "");
        File first = WallpaperRotation.find(files, previous);
        if (first == null) first = WallpaperRotation.pick(files, previous, order, random);
        while (first != null) {
            Bitmap bitmap = decode(first);
            if (bitmap != null) {
                show(bitmap);
                // The picture covers the whole screen: the root's plain colour under it is overdraw.
                root.setBackground(null);
                displayed = first.getName();
                showCaption(false);
                if (!displayed.equals(previous)) {
                    preferences.edit().putString("image", displayed)
                            .putLong("changed", System.currentTimeMillis()).apply();
                }
                return;
            }
            files.remove(first);   // unreadable or over 4K: try another, or keep the plain colour
            first = WallpaperRotation.pick(files, previous, order, random);
        }
    }

    void start() {
        active = true;
        update();
    }

    void stop() {
        active = false;
    }

    void close() {
        active = false;
        closed = true;
        decoder.shutdownNow();
    }

    void update() {
        if (!active || closed || loading || files.isEmpty()) return;
        String previous = preferences.getString("image", "");
        File selected = WallpaperRotation.find(files, previous);
        long changed = preferences.getLong("changed", 0);
        if (selected == null || WallpaperRotation.due(changed, System.currentTimeMillis(), interval)) {
            selected = WallpaperRotation.pick(files, previous, order, random);
        }
        if (selected == null || selected.getName().equals(displayed)) return;
        File next = selected;
        loading = true;
        decoder.execute(() -> {
            Bitmap bitmap = decode(next);
            root.post(() -> {
                loading = false;
                if (closed || !active) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                if (bitmap == null) {
                    files.remove(next);
                    Log.w("TvHome.Wallpaper", "Skipping unreadable image: " + next.getName());
                    update();
                    return;
                }
                show(bitmap);
                displayed = next.getName();
                showCaption(true);
                if (!displayed.equals(previous) || changed == 0) {
                    preferences.edit().putString("image", displayed)
                            .putLong("changed", System.currentTimeMillis()).apply();
                }
            });
        });
    }

    /**
     * Small light text under the top-right buttons, above everything else and never focusable. It sits where the
     * top bar ends in every layout, so it does not cover the clock, the stats line or the tiles.
     */
    private static TextView makeCaption(Context context, FrameLayout root) {
        float dp = context.getResources().getDisplayMetrics().density;
        TextView text = new TextView(context);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setTextColor(0xD9FFFFFF);
        text.setShadowLayer(6 * dp, 0, dp, 0xCC000000);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setMaxWidth(Math.round(520 * dp));
        text.setFocusable(false);
        text.setAlpha(0f);
        try {
            text.setTypeface(UiFont.get(context, R.string.font_regular));
        } catch (Exception ignored) {
            // the system font is fine
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        params.topMargin = Math.round(110 * dp);
        params.setMarginEnd(Math.round(62 * dp));
        root.addView(text, params);
        return text;
    }

    /** The displayed picture's name, faded across a change; a picture without a name hides the caption. */
    private void showCaption(boolean fade) {
        if (caption == null) return;
        String name = names.get(displayed);
        caption.animate().cancel();
        if (name == null) {
            caption.setAlpha(0f);
            return;
        }
        caption.setContentDescription(name);
        if (!fade || caption.getAlpha() == 0f) {
            caption.setText(name);
            caption.setAlpha(1f);
            return;
        }
        caption.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            caption.setText(name);
            caption.animate().alpha(1f).setDuration(350).start();
        }).start();
    }

    /** Names plus sizes: enough to notice a different collection without hashing every image. */
    private static String collectionKey(List<File> images) {
        StringBuilder key = new StringBuilder();
        for (File file : images) key.append(file.getName()).append(':').append(file.length()).append(';');
        return key.toString();
    }

    /** Puts a picture on screen and releases the previous one after a frame has drawn the new one. */
    private void show(Bitmap bitmap) {
        Bitmap old = current;
        current = bitmap;
        image.setImageBitmap(bitmap);
        if (old != null && old != bitmap) image.postDelayed(old::recycle, 500);
    }

    /**
     * Twelve small, locally generated landscape scenes. They are written once to cache, so no image
     * assets inflate the APK and the 1 GB TV does not keep twelve decoded bitmaps in memory.
     */
    private static List<File> createFallbackWallpapers(Context context) {
        final int[][] palette = {
                {0xFF101B35, 0xFF526B8C, 0xFFF0B779, 0xFF1D3342, 0xFF263F4B},
                {0xFF10283A, 0xFF4E8795, 0xFFFFD39A, 0xFF24535A, 0xFF12343C},
                {0xFF080F28, 0xFF293F66, 0xFFB5C8D8, 0xFF1D2A45, 0xFF111D31},
                {0xFF061B2B, 0xFF235D6C, 0xFF65D7B4, 0xFF123B4C, 0xFF0A2938},
                {0xFF2B1838, 0xFF9C5D70, 0xFFFFC58A, 0xFF53364B, 0xFF2C3142},
                {0xFF14263F, 0xFF7D9DAF, 0xFFE6E0CB, 0xFF344B61, 0xFF1A3444},
                {0xFF091D2C, 0xFF4F7C82, 0xFFF1CDA0, 0xFF24545A, 0xFF12363F},
                {0xFF0A2B35, 0xFF568B79, 0xFFE8C68C, 0xFF285847, 0xFF173E38},
                {0xFF201B35, 0xFF78627C, 0xFFFFC18A, 0xFF43364D, 0xFF252B3B},
                {0xFF091B32, 0xFF2B5378, 0xFF91D0D3, 0xFF17415B, 0xFF0D2B43},
                {0xFF13263B, 0xFF65869A, 0xFFF4D6A5, 0xFF345364, 0xFF1C3849},
                {0xFF101C2A, 0xFF4B6B70, 0xFFD9B58B, 0xFF28484B, 0xFF152F37}
        };
        final String[] names = {"01-dusk-peaks", "02-golden-coast", "03-moon-lake", "04-aurora",
                "05-dunes", "06-above-the-clouds", "07-mirror-lake", "08-tea-hills",
                "09-alpenglow", "10-deep-blue", "11-morning-ridge", "12-monsoon"};
        final float[][] ridges = {
                {190, 90, 150, 72, 180}, {165, 130, 68, 150, 115}, {130, 70, 155, 95, 170},
                {180, 115, 60, 135, 85}, {145, 65, 125, 175, 100}, {175, 85, 135, 72, 155},
                {155, 105, 170, 65, 140}, {185, 145, 70, 120, 95}, {155, 78, 130, 90, 165},
                {180, 110, 65, 150, 95}, {150, 80, 155, 70, 145}, {170, 95, 145, 75, 160}
        };
        File dir = new File(context.getCacheDir(), "demi-landscape-wallpapers");
        if (!dir.exists() && !dir.mkdirs()) return Collections.emptyList();
        List<File> result = new ArrayList<>();
        for (int i = 0; i < palette.length; i++) {
            File file = new File(dir, names[i] + ".jpg");
            if (!file.isFile() || file.length() == 0) {
                Bitmap bitmap = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setShader(new LinearGradient(0, 0, 0, 270,
                        new int[]{palette[i][0], palette[i][1], palette[i][2]},
                        new float[]{0f, 0.68f, 1f}, android.graphics.Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, 480, 270, paint);
                paint.setShader(null);
                paint.setColor(palette[i][2]);
                paint.setAlpha(165);
                float sunX = 105 + (i * 67 % 280);
                float sunY = 66 + (i * 17 % 50);
                canvas.drawCircle(sunX, sunY, i % 3 == 0 ? 25 : 17, paint);
                paint.setAlpha(36);
                canvas.drawCircle(sunX, sunY, 38, paint);
                drawRidge(canvas, palette[i][3], ridges[i]);
                float[] front = ridges[i].clone();
                for (int j = 0; j < front.length; j++) front[j] = Math.min(235, front[j] + 45 + (j % 2) * 18);
                drawRidge(canvas, palette[i][4], front);
                // A dark foreground band gives the translucent app dock enough contrast.
                paint.setColor(0x55101A20);
                paint.setAlpha(95);
                canvas.drawRect(0, 242, 480, 270, paint);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out);
                    out.flush();
                } catch (Exception failure) {
                    file.delete();
                } finally {
                    bitmap.recycle();
                }
            }
            if (file.isFile() && file.length() > 0) result.add(file);
        }
        return result;
    }

    private static void drawRidge(Canvas canvas, int color, float[] peaks) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(0, 270);
        path.lineTo(0, peaks[0]);
        for (int i = 1; i < peaks.length; i++) path.lineTo(i * 120, peaks[i]);
        path.lineTo(480, 270);
        path.close();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            int sample = WallpaperSize.sampleSize(width, height, this.width, this.height);
            if (!WallpaperSize.accepted(width / sample, height / sample)) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(width / sample, height / sample, this.width, this.height);
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            options.inPreferredConfig = bitmapConfig;
            options.inMutable = dimColor != 0;
            Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), options);
            if (decoded == null) return null;
            if (paletteListener != null) { int accent = AdaptivePalette.accent(decoded); root.post(() -> paletteListener.accept(accent)); }
            if (dimColor != 0) new Canvas(decoded).drawColor(dimColor);
            // One copy in GPU memory instead of a software bitmap plus its texture; the decode stays off the UI
            // thread for rotations, and Home's return no longer uploads 4-8 MB to the GPU.
            Bitmap gpu = decoded.copy(Bitmap.Config.HARDWARE, false);
            if (gpu == null) return decoded;
            decoded.recycle();
            return gpu;
        } catch (Exception | OutOfMemoryError failure) {
            return null;
        }
    }
}
