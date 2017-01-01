package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * Created by omer on 10/12/2016.
 * The bluetooth advertising uses BLE abilities to be discoverable to other BLE devices.
 * the period time of advertising set by the bluetooth manager class.
 */

public class BLEAdvertising implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BLEAdvertising.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;

    /**
     * BLEAdvertising constructor
     * @param bluetoothAdapter helps to check if bluetooth enable or disable
     */
    public BLEAdvertising(BluetoothAdapter bluetoothAdapter) {

        this.mBluetoothAdapter = bluetoothAdapter;
        this.mAdvertiseCallback = null;

        if (mBluetoothAdapter != null) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        } else {
            Log.e(TAG, "Error - FAILED_ADVERTISING");
        }
        Log.d(TAG, "Class created");
    }

    /**
     * StartAdvertising
     */
    public void startAdvertising() {
        if (mAdvertiseCallback == null) {

            // setup advertising setting
            AdvertiseSettings settings = buildAdvertiseSettings();

            // setup device uuid in data
            AdvertiseData data = buildAdvertiseData();

            // setup device respond when found
            AdvertiseData dataRes = buildAdvertiseScanResponse();

            // set custom callback to get information about the connection status
            mAdvertiseCallback = new CustomAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null && mBluetoothAdapter.isEnabled()) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, dataRes, mAdvertiseCallback);
                Log.d(TAG, "Starting Advertising");
            }
        }
    }

    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null && mAdvertiseCallback!= null && mBluetoothAdapter.isEnabled()) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
            Log.d(TAG, "System Stopping Advertising");
        }
    }


    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);//check
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);//check
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(0); //disable time limit
        return settingsBuilder.build();
    }

    /**
     * Returns an AdvertiseData object which includes Device uuid.
     */
    private AdvertiseData buildAdvertiseData() {

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        // add service UUID
        dataBuilder.addServiceUuid(new ParcelUuid(RELAY_SERVICE_UUID));
        dataBuilder.setIncludeTxPowerLevel(true);
        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseScanResponse.
     */
    private AdvertiseData buildAdvertiseScanResponse() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start.
     */
    private class CustomAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Error - Not broadcasting, code: "+errorCode);
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "ADVERTISE_FAILED_ALREADY_STARTED");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG, "ADVERTISE_FAILED_DATA_TOO_LARGE");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "ADVERTISE_FAILED_INTERNAL_ERROR");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
                    break;
            }
        }
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }
    }
}
