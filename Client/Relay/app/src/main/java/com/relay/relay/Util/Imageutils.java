package com.relay.relay.Util;

/**
 * Created by omer on 25/04/2017.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Imageutils {

    public static final int CAMERA_REQUEST = 150;
    public static final int GALLERY_REQUEST = 250;

    Context context;
    Activity current_activity;


    public Imageutils(Activity activity) {
        this.context = activity;
        this.current_activity = activity;
    }

    /**
     * Check Camera Availability
     */

    public boolean isDeviceSupportCamera() {
        if (this.context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * Launch Camera
     */

    public void launchCamera() {
        if (Build.VERSION.SDK_INT >= 23) {
            permission_check(CAMERA_REQUEST);
        } else {
            camera_call();
        }
    }

    /**
     * Launch Gallery
     */

    public void launchGallery() {
        if (Build.VERSION.SDK_INT >= 23) {
            permission_check(GALLERY_REQUEST);
        }
        else {
            galley_call();
        }
    }


    /**
     * Check permission
     */

    public void permission_check(final int code)
    {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(current_activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED)
        {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(current_activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                showMessageOKCancel("For adding images , You need to provide permission to access your files",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                ActivityCompat.requestPermissions(current_activity,
                                        new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        code);
                            }
                        });
                return;
            }

            ActivityCompat.requestPermissions(current_activity,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    code);
            return;
        }

        if(code == CAMERA_REQUEST)
            camera_call();
        else if(code == GALLERY_REQUEST)
            galley_call();
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(current_activity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    /**
     * Capture image from camera
     */

    public void camera_call() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        current_activity.startActivityForResult(intent, CAMERA_REQUEST);
    }

    /**
     * pick image from Gallery
     */
    public void galley_call() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        current_activity.startActivityForResult(intent, GALLERY_REQUEST);
    }


    /**
     * Activity PermissionResult
     */
    public void request_permission_result(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera_call();
                } else {
                    Toast.makeText(current_activity, "Permission denied",Toast.LENGTH_LONG).show();
                }
                break;

            case GALLERY_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    galley_call();
                } else {
                    Toast.makeText(current_activity, "Permission denied",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }



}
