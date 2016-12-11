package com.relay.relay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by omer on 10/12/2016.
 * The bluetooth scan uses BLE abilities to search for other bluetooth device advertisements.
 * When finding a bluetooth device or finishing search, the class will send notice to
 * bluetooth manager
 */

public class BLEScan implements BLConstants{

    private final String TAG = "RELAY_DEBUG: "+ BLEScan.class.getSimpleName();

    private Messenger mMessenger;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private List<String> mLastConnectedDevices;


    public BLEScan(BluetoothAdapter bluetoothAdapter, Messenger messenger,
                   List<String> lastConnectedDevices ) {
        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mLastConnectedDevices = lastConnectedDevices;
        this.mScanCallback = null;

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        Log.d(TAG, "Class created");
    }


    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {

            // Kick off a new scan.
            mScanCallback = new CustomScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            Log.d(TAG, "Start Scanning for BLE Advertisements");
        } else {
            Log.e(TAG, "Problem - Called Scan while already scanning");
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        Log.d(TAG, "Stopping Scanning");
    }


    /**
     * Return a List of objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(new ParcelUuid(APP_UUID));
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return an object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        // TODO only works in api 23 an above. I guess that its default in the current api
        //builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        return builder.build();
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class CustomScanCallback extends ScanCallback {

        // TODO not using the batch result for now. only onScanResult.
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String address = result.getDevice().getAddress();

            if (!mLastConnectedDevices.contains(address))
            {
                // found new device that not been sync
                // send result to bluetooth manager
                sendResultToManager(result);
            }
            Log.d(TAG, "onScanResult - found new result");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error: "+ errorCode);
        }
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
     * Send result value to bluetooth manager
     */
    private void sendResultToManager(ScanResult result) {

        // Send data as a String
        Bundle bundle = new Bundle();
        bundle.putParcelable("result", result);
        Message msg = Message.obtain(null, FOUND_NEW_DEVICE);
        msg.setData(bundle);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
