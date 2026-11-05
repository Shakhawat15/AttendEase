package com.example.attendease;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_SIZE = 300;   // longest side in pixels
    private static final int JPEG_QUALITY = 70;

    // Bitmap -> small JPEG -> Base64 text (to save in Firebase)
    public static String bitmapToBase64(Bitmap bitmap) {
        Bitmap scaled = scaleDown(bitmap);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    // Base64 text -> Bitmap (to show in an ImageView)
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Gallery Uri -> Bitmap, loaded at reduced size so big photos don't run out of memory
    public static Bitmap uriToBitmap(Context context, Uri uri) {
        try {
            // 1st pass: read only the image dimensions
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }

            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= MAX_SIZE
                    && bounds.outHeight / (sample * 2) >= MAX_SIZE) {
                sample *= 2;
            }

            // 2nd pass: load a smaller version
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static Bitmap scaleDown(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= MAX_SIZE && h <= MAX_SIZE) return src;
        float ratio = Math.min((float) MAX_SIZE / w, (float) MAX_SIZE / h);
        return Bitmap.createScaledBitmap(src, Math.round(w * ratio), Math.round(h * ratio), true);
    }
}