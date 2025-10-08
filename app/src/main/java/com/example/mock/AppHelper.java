package com.example.mock;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class AppHelper {
    public static byte[] getFileDataFromDrawable(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return new byte[0]; // Return empty byte array if bitmap is null
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Compress as JPEG, 80% quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}

