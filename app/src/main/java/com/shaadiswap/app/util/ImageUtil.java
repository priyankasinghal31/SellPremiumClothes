package com.shaadiswap.app.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageUtil {
    private static final int MAX_SIDE = 1280;

    private ImageUtil() {
    }

    /** Decodes, fixes rotation, shrinks to 1280px and returns JPEG bytes. Call off the main thread. */
    public static byte[] compress(ContentResolver cr, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (bounds.outWidth / sample > 2 * MAX_SIDE || bounds.outHeight / sample > 2 * MAX_SIDE) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("Could not decode image");

        int rotation = 0;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in != null) {
                int o = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (IOException ignored) {
            // No EXIF data: keep as is.
        }

        Matrix m = new Matrix();
        if (rotation != 0) m.postRotate(rotation);
        int longest = Math.max(bmp.getWidth(), bmp.getHeight());
        if (longest > MAX_SIDE) {
            float s = (float) MAX_SIDE / longest;
            m.postScale(s, s);
        }
        if (rotation != 0 || longest > MAX_SIDE) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return out.toByteArray();
    }
}
