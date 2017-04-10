package com.relay.relay.Util;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.relay.relay.R;

/**
 * Created by omer on 08/04/2017.
 */

public class ShowDialogWithPicture {

    public ShowDialogWithPicture(Bitmap pic , Activity activity){

        // Get the layout inflater
        LayoutInflater inflater = activity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_show_picture, null);

        TouchImageView imageView = (TouchImageView) view.findViewById(R.id.dialog_image);

        if (pic != null)
            imageView.setImageBitmap(pic);
            imageView.setMaxZoom(4f);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view);
        // Add action buttons
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                // sign in the user ...
//            }
//        });

        builder.create().show();
    }
}
