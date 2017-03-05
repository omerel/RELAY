package com.relay.relay.system;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Created by omer on 05/03/2017.
 */

public class BitmapConvertor {

    final static int QUALITY = 70;

    /**
     * Convert Bitmap to bytes
     * @param bitmap
     * @return
     */
    public static byte[] ConvertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,QUALITY,stream);

        return stream.toByteArray();
    }

    /**
     * Convert bytes To Bitmap
     * @param byteArray
     * @return
     */
    public static Bitmap convertBytesToBitmap(byte[] byteArray){
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return bitmap;
    }


}
