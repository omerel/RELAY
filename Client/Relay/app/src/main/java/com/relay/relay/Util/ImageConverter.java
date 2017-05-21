package com.relay.relay.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

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
        Bitmap newBitmap;
        try {
            float ratio = Math.min(
                    (float) maxImageSize / realImage.getWidth(),
                    (float) maxImageSize / realImage.getHeight());
            int width = Math.round((float) ratio * realImage.getWidth());
            int height = Math.round((float) ratio * realImage.getHeight());

            int size = Math.max(height, width);


            newBitmap = Bitmap.createScaledBitmap(realImage, size,
                    size, filter);
        }
        catch (Exception e){
            Log.e(TAG,"Error in scaleDown");
            return realImage;
        }
        return newBitmap;
    }

    public static  Bitmap scaleImageToImageViewSize(ImageView imageView,String imagePath){

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);

        return bitmap;

    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage,null, null);
        return Uri.parse(path);
    }

}
