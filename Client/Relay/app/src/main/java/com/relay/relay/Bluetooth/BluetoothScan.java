package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.viewsAndViewAdapters.StatusBar;

/**
 * Created by omer on 03/01/2017.
 * Bluetooth scan allows user scan manually near device when the device doesn't support ble
 * advertisement
 */

public class BluetoothScan implements BLConstants{

    private final String TAG = "RELAY_DEBUG: "+BluetoothScan.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private RelayConnectivityManager mRelayConnectivityManager;
    private Messenger mMessenger;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;
    private boolean isDeviceFound;
    private String mBluetoothName;
    private Handler stopScanHandler;


    public BluetoothScan(BluetoothAdapter bluetoothAdapter,Messenger messenger,
                         RelayConnectivityManager relayConnectivityManager) {
        // Use messenger to update bluetooth manger
        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mRelayConnectivityManager = relayConnectivityManager;
        this.isDeviceFound = false;
       // createBroadcastReceiver();
    }

    public void startScan(){

        createBroadcastReceiver();

        if (mBluetoothAdapter != null) {
            mBluetoothName = mBluetoothAdapter.getName();
            mBluetoothAdapter.setName("relay_"+mBluetoothName);
            sendMessageToBluetoothManager(GET_BLUETOOTH_SERVER_READY,null);
            mBluetoothAdapter.startDiscovery();
            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_SEARCH,TAG+": start Manual Discovery " );
            Log.e(TAG, "start Discovery ");
            beDiscoverable();
        }
        stopScanHandler = new Handler();
        stopScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
                // if not found or if the device was the server
                if(!isDeviceFound) {
                    try {
                        mRelayConnectivityManager.unregisterReceiver(mBroadcastReceiver);
                        Log.e(TAG, "stopScanHandler is alarmed");
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    sendMessageToBluetoothManager(NOT_FOUND_ADDRESS_FROM_BLSCAN, null);
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Found non in bl scan" );
                }
            }
        }, SCAN_TIME);
    }

    private void stopScan(){
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.setName(mBluetoothName);
        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Stop Bluetooth scan" );
        Log.e(TAG, "finish Discovery ");
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
                            String[] split = null;
                            if (device.getName() != null)
                                split = device.getName().split("_");
                            if (split != null && split.length > 0) {
                                if (split[0].equals("relay")) {
                                    isDeviceFound = true;
                                    Log.e(TAG, "ACTION_FOUND " + device.getName());
                                    stopScan();
                                    try {
                                        mRelayConnectivityManager.unregisterReceiver(mBroadcastReceiver);
                                        sendMessageToBluetoothManager(FOUND_MAC_ADDRESS_FROM_BLSCAN, device.getAddress());
                                        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Found MAC address from bl scan, device: "+ device.getAddress());
                                    }catch (Exception e){
                                        Log.e(TAG,e.getMessage());

                                    }
                                }
                            } else {
                                mBluetoothAdapter.startDiscovery();
                            }
                            break;
                    }
                }
            };
            mRelayConnectivityManager.registerReceiver(mBroadcastReceiver, mFilter);

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
            mRelayConnectivityManager.startActivity(discoverableIntent);
        }
    }

    /**
     * Send scan device to bluetooth manager
     */
    private void sendMessageToBluetoothManager(int m, String address)  {

        // Send data
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendMessageToBluetoothManager ");
        }
    }



}
