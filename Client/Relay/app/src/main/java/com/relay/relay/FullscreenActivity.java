package com.relay.relay;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.TouchImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide status bar
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        Intent intent = getIntent();
        Bitmap pic = ImageConverter.convertBytesToBitmap(intent.getByteArrayExtra("image"));

        setContentView(R.layout.activity_fullscreen);
        TouchImageView imageView = (TouchImageView) findViewById(R.id.fullscreen_content);

        if (pic != null)
            imageView.setImageBitmap(pic);
            imageView.setMaxZoom(4f);

    }
}
