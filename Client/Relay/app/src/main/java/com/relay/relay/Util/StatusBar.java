package com.relay.relay.Util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;

import com.relay.relay.R;


/**
 * Created by omer on 04/05/2017.
 */

public class StatusBar {

    public static final String STATUS_BAR_RELAY = "relay.BroadcastReceiver.STATUS_BAR_RELAY";
    public static final int FLAG_ADVERTISEMENT = 11;
    public static final int STOP_ADVERTISEMENT = 113;
    public static final int FLAG_STOP_SCAN = 111;
    public static final int FLAG_CLOSE_CONNECTION = 112;
    public static final int FLAG_SEARCH = 12;
    public static final int FLAG_CONNECTING = 13;
    public static final int FLAG_HANDSHAKE = 14;
    public static final int FLAG_ERROR = 15;


    private Activity mActivity;
    private ImageView mAdvertisementFlag;
    private ImageView mSearchFlag;
    private ImageView mConnectingFlag;
    private ImageView mHandShakeFlag;
    private ImageView mErrorFlag;

    // Listener when flag has been changed
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;


    public StatusBar(Activity activity){
        this.mActivity = activity;

        this.mAdvertisementFlag = (ImageView) mActivity.findViewById(R.id.flag_advertise);
        mAdvertisementFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
        this.mSearchFlag = (ImageView) mActivity.findViewById(R.id.flag_search);
        mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
        this.mConnectingFlag = (ImageView) mActivity.findViewById(R.id.flag_connecting);
        mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
        this.mHandShakeFlag = (ImageView) mActivity.findViewById(R.id.flag_handshake);
        mHandShakeFlag.setVisibility(View.INVISIBLE);
        this.mErrorFlag = (ImageView) mActivity.findViewById(R.id.flag_error);
        mErrorFlag.setVisibility(View.INVISIBLE);
        createBroadcastReceiver();
    }


    /**
     * BroadcastReceiver
     */
    private void createBroadcastReceiver() {
        mFilter = new IntentFilter();
        mFilter.addAction(STATUS_BAR_RELAY);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                switch (action){

                    case STATUS_BAR_RELAY:
                        int flag = intent.getIntExtra("flag",0);
                        turnOnFlag(flag);
                        break;
                }
            }
        };
        mActivity.registerReceiver(mBroadcastReceiver, mFilter);
    }


    public void close(){
        mActivity.unregisterReceiver(mBroadcastReceiver);
    }
    private void turnOnFlag(int flag) {

        switch (flag){
            case FLAG_STOP_SCAN:
                mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                break;
            case FLAG_CLOSE_CONNECTION:
                mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                break;
            case FLAG_ADVERTISEMENT:
                mAdvertisementFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_advertise));
                break;
            case STOP_ADVERTISEMENT:
                mAdvertisementFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                break;
            case FLAG_SEARCH :
                mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_search));
                break;
            case FLAG_CONNECTING :
                mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_connecting));
                break;
            case FLAG_HANDSHAKE :
                blinkFlag(FLAG_HANDSHAKE);
                break;
            case FLAG_ERROR :
                blinkFlag(FLAG_ERROR);
                break;
            case 0 :
                mAdvertisementFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
                break;
        }
    }


    private void blinkFlag(int flag) {


        if (flag == FLAG_ERROR){
            new CountDownTimer(3000, 1000) {
                public void onTick(long millisUntilFinished) {
                    mErrorFlag.setVisibility(View.VISIBLE);
                }
                public void onFinish() {
                    mErrorFlag.setVisibility(View.INVISIBLE);
                }
            }.start();
        }

        if (flag == FLAG_HANDSHAKE){
            // flash hand shake
            new CountDownTimer(1000, 1000) {
                public void onTick(long millisUntilFinished) {
                    mHandShakeFlag.setVisibility(View.VISIBLE);
                }
                public void onFinish() {
                    mHandShakeFlag.setVisibility(View.INVISIBLE);
                }
            }.start();
        }
    }

    public void clear() {
        mAdvertisementFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
        mSearchFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
        mConnectingFlag.setImageDrawable(mActivity.getDrawable(R.drawable.ic_flag_non));
    }
}
