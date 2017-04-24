package com.relay.relay.Util;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.relay.relay.FullscreenActivity;
import com.relay.relay.R;

import static java.security.AccessController.getContext;

/**
 * Created by omer on 08/04/2017.
 */

public class ShowActivityFullImage {

    public ShowActivityFullImage(Bitmap pic , Activity activity){

        Intent intent = new Intent(activity, FullscreenActivity.class);
        intent.putExtra("image",ImageConverter.ConvertBitmapToBytes(pic));
        activity.startActivity(intent);

//        // Get the layout inflater
//        LayoutInflater inflater = activity.getLayoutInflater();
//
//        View view = inflater.inflate(R.layout.dialog_show_picture, null);
//
//        TouchImageView imageView = (TouchImageView) view.findViewById(R.id.dialog_image);
//
//        if (pic != null)
//            imageView.setImageBitmap(pic);
//            imageView.setMaxZoom(4f);
//
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        builder.setView(view);
//        // Add action buttons
//
//        builder.create().show();
    }

    private void goToConversationActivity( Activity activity) {


    }
}
