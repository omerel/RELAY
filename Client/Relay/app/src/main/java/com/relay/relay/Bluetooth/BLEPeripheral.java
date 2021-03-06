package com.relay.relay.Bluetooth;

/**
 * Created by omer on 20/12/2016.
 * Class for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 * this class play the server side of ble service
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.MacAddressFinder;
import com.relay.relay.viewsAndViewAdapters.StatusBar;


public class BLEPeripheral implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BLEPeripheral.class.getSimpleName();

    private BluetoothGattService mBluetoothGattService;
    private RelayConnectivityManager mRelayConnectivityManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEService mBLEService;
    private BluetoothGattServer mGattServer;
    private Messenger mMessenger;
    private  int mConnectionCounter;
    private BluetoothGattServerCallback mGattServerCallback;



    /**
     * BLEPeripheral constructor
     * @param bluetoothAdapter to check if bluetooth enable
     * @param messenger to send result to bluetooth manager
     * @param relayConnectivityManager needed to create bluetooth gatt
     */
    public BLEPeripheral(BluetoothAdapter bluetoothAdapter, Messenger messenger, RelayConnectivityManager relayConnectivityManager){

        this.mRelayConnectivityManager = relayConnectivityManager;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBLEService = new BLEService(MacAddressFinder.getBluetoothMacAddress());
        this.mBluetoothGattService = mBLEService.getBluetoothGattService();
        this.mMessenger =messenger;
        this.mBluetoothManager = (BluetoothManager) mRelayConnectivityManager.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mConnectionCounter = 0;

    }

    /**
     * close GattServer
     */
    public void close() {
        if (mBluetoothAdapter.isEnabled()) {
            if (mGattServer != null) {
                disconnectFromDevices();
                mGattServer.clearServices();
                mGattServer.close();
            }
        }
    }

    /**
     * Start Peripheral - start advertising
     */
    public void startPeripheral(){
        Log.e(TAG, "startPeripheral");

        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        if (mGattServer!= null)
            mGattServer.close();
        /**
         * BluetoothGattServerCallback
         */
        mGattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
                super.onConnectionStateChange(device, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        //mGattServer.connect(device,false);
                        mConnectionCounter++;
                        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Device Connected to my bluetooth GATT");
                        Log.e(TAG, "Device Connected to my bluetooth GATT: " + device.getAddress());

                        sendResultToManager(GET_BLUETOOTH_SERVER_READY);

                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        Log.d(TAG, " device Disconnected from my bluetooth GATT ");
                        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": device Disconnected from my bluetooth GATT ");
                    }
                } else {
                    Log.e(TAG, "Error when connecting: " + status);
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_ERROR,TAG+": Error when connecting: "+status);

                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
                Log.e(TAG, "onServiceAdded");
            }

            @Override
            public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId,final  int offset,
                                                    final BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
                if (mBluetoothAdapter.isEnabled()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                                    offset, characteristic.getValue());
                        }
                    });
                }
                Log.e(TAG, "Connection counter: "+ mConnectionCounter +"!!!");
            }

        };


        // open GattServer
        mGattServer = mBluetoothManager.openGattServer(mRelayConnectivityManager, mGattServerCallback);

        if (mGattServer == null) {
            Log.e(TAG, "ERROR - didn't open gattServer. returns null");
            // reset mBluetoothManager and start Peripheral in the next interval
            close();
            sendResultToManager(BLE_GATT_SERVER_ERROR);
            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_ERROR,TAG+": ERROR - didn't open gattServer. returns null");
            return;
        }

        // add services
        Log.e(TAG, "addService() - service: " + mGattServer.addService(mBluetoothGattService));
        Log.e(TAG, "mGattServer.getServices().size(): " + mGattServer.getServices().size());

    }


    /**
     * Disconnect From Devices
     */
    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        if (mBluetoothAdapter.enable()) {
            for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
                    BluetoothGattServer.GATT)) {
                Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
                mGattServer.cancelConnection(device);
            }
        }
    }

    /**
     * Send message to bluetooth manager
     * @param m message
     */
    private void sendResultToManager(int m)  {

        // Send data
        Bundle bundle = new Bundle();
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error -  sendResultToManager ");
        }
    }

}
