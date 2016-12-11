package com.relay.relay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Created by omer on 10/12/2016.
 * The bluetooth advertising uses BLE abilities to be discoverable for other bluetooth devices.
 * the period time of advertising set by the caller class.
 */

public class BLEAdvertising implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BLEAdvertising.class.getSimpleName();

    private Messenger mMessenger;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;

    //Length of time to allow advertising before automatically shutting off. (TIMEOUT_ADVERTISING_IN_MINUTES)
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(TIMEOUT_ADVERTISING_IN_MINUTES, TimeUnit.MINUTES);
    private Handler mHandler;
    private Runnable timeoutRunnable;

    public BLEAdvertising(BluetoothAdapter bluetoothAdapter, Messenger messenger) {

        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mAdvertiseCallback = null;

        if (mBluetoothAdapter != null) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        } else {
            Log.e(TAG, "FAILED_ADVERTISING");
        }

        Log.d(TAG, "Class created");
    }

    /**
     * Start BLE Advertising
     */
    public void startAdvertising() {
        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            // setup device uuid in data
            AdvertiseData data = buildAdvertiseData();
            // set custom callback to get information about the connection status
            mAdvertiseCallback = new CustomAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
                // set timer to timeout
                setTimeout();
                Log.d(TAG, "Starting Advertising");
            }
        }
    }

    /**
     * Stops BLE Advertising
     */
    public void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
            // stop timeout
            mHandler.removeCallbacks(timeoutRunnable);
            Log.d(TAG, " System Stopping Advertising");
        }
    }


    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Returns an AdvertiseData object which includes Device uuid.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        // no need name
        dataBuilder.setIncludeDeviceName(false);
        // add service UUID
        dataBuilder.addServiceUuid(new ParcelUuid(APP_UUID));

        return dataBuilder.build();
    }


    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout(){
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AdvertiserService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                stopAdvertising();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    /**
     * Send message to the bluetooth manager
     */
    private void sendMessageToManager(int msg)  {
        try {
            mMessenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }

    /**
     * Custom callback after Advertising succeeds or fails to start.
     */
    private class CustomAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed. code: "+errorCode);
        }
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }
    }
}
