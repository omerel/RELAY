package com.relay.relay.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.couchbase.lite.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

/**
 * Created by omer on 05/03/2017.
 */

public class ImageConverter {

    final static int QUALITY = 30;

    /**
     * Convert Bitmap to bytes
     * @param bitmap
     * @return
     */
    public static byte[] ConvertBitmapToBytes(Bitmap bitmap) {
        if (bitmap != null){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,QUALITY,stream);

            return stream.toByteArray();
        }
        return null;
    }

    /**
     * Convert bytes To Bitmap
     * @param byteArray
     * @return
     */
    public static Bitmap convertBytesToBitmap(byte[] byteArray){
        if (byteArray != null){
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            return bitmap;
        }
        return null;
    }


    public static InputStream convertBitmapToInputStream(Bitmap bitmap){

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
        return  bs;
    }

    public static Drawable convertInputStreamToDrawable(InputStream inputStream){

        Drawable drawable = Drawable.createFromStream(inputStream, "picture");
        return drawable;

    }


    public static byte[] convertInputStreamToByteArray(InputStream inputStream){
        byte[] bytes;
        try {
           bytes = IOUtils.toByteArray(inputStream);
            return  bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

}
