package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.relay.relay.ConnectivityManager;

/**
 * Created by omer on 03/01/2017.
 * Bluetooth scan allows user scan manually near device when the device doesn't support ble
 * advertisement
 */

public class BluetoothScan implements BLConstants{

    private final String TAG = "RELAY_DEBUG: "+BluetoothScan.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectivityManager mConnectivityManager;
    private Messenger mMessenger;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;
    private boolean isDeviceFound;
    private String mBluetoothName;


    public BluetoothScan(BluetoothAdapter bluetoothAdapter,Messenger messenger,
                         ConnectivityManager connectivityManager) {
        // Use messenger to update bluetooth manger
        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mConnectivityManager = connectivityManager;
        this.isDeviceFound = false;
        createBroadcastReceiver();
    }

    public void startScan(){
        mConnectivityManager.registerReceiver(mBroadcastReceiver, mFilter);

        if (mBluetoothAdapter != null) {
            mBluetoothName = mBluetoothAdapter.getName();
            mBluetoothAdapter.setName("relay_"+mBluetoothName);
            mBluetoothAdapter.startDiscovery();
            Log.e(TAG, "start Discovery ");
            beDiscoverable();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
                if(!isDeviceFound)
                    sendResultToBluetoothManager(NOT_FOUND_ADDRESS_FROM_BLSCAN,null);
            }
        }, SCAN_TIME);
    }

    private void stopScan(){
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.setName(mBluetoothName);
//        mConnectivityManager.unregisterReceiver(mBroadcastReceiver);
        Log.e(TAG, "cancel Discovery ");
    }

    /**
     * BroadcastReceiver of bluetooth scan
     */
    private  void createBroadcastReceiver() {

        mFilter = new IntentFilter();
        // Add all all the actions to filter
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action) {
                    // When discovery finds a device
                    case BluetoothDevice.ACTION_FOUND:

                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.e(TAG, "ACTION_FOUND "+device.getName());

                        String[] split = null;
                        if (device.getName()!= null)
                            split = device.getName().split("_");
                        if (split!= null && split.length>0)
                            if (split[0].equals("relay")) {
                                isDeviceFound = true;
                                stopScan();
                                sendResultToBluetoothManager(FOUND_MAC_ADDRESS_FROM_BLSCAN, device.getAddress());
                            }


                        break;
                }
            }
        };
    }

    /**
     *  Make the device be discoverable
     */
    private void beDiscoverable(){

        if (mBluetoothAdapter != null){
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_TIME);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mConnectivityManager.startActivity(discoverableIntent);
        }
    }

    /**
     * Send scan device to bluetooth manager
     */
    private void sendResultToBluetoothManager(int m, String address)  {

        // Send data
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendResultToBluetoothManager ");
        }
    }



}
