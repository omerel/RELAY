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
import android.os.Messenger;
import android.util.Log;
import com.relay.relay.ConnectivityManager;


public class BLEPeripheral implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BLEPeripheral.class.getSimpleName();

    private BluetoothGattService mBluetoothGattService;
    private ConnectivityManager mConnectivityManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEService mBLEService;
    private BLEAdvertising mBleAdvertising;
    private BluetoothGattServer mGattServer;

    /**
     * BluetoothGattServerCallback
     */
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mGattServer.connect(device,false);
                    // Stop advertising
                    mBleAdvertising.stopAdvertising();
                    Log.d(TAG, "Connected to device: " + device.getAddress());

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from device");
                    // Continue advertising to others device
                    mBleAdvertising.startAdvertising();
                }
            } else {
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            if (mBluetoothAdapter.isEnabled())
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }
    };

    /**
     * BLEPeripheral constructor
     * @param bluetoothAdapter to check if bluetooth enable
     * @param messenger to send result to bluetooth manager
     * @param connectivityManager needed to create bluetooth gatt
     */
    BLEPeripheral(BluetoothAdapter bluetoothAdapter, Messenger messenger,ConnectivityManager connectivityManager){

        this.mConnectivityManager = connectivityManager;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBLEService = new BLEService(mBluetoothAdapter.getAddress());
        this.mBluetoothGattService = mBLEService.getBluetoothGattService();
        this.mBleAdvertising = new BLEAdvertising(mBluetoothAdapter);

        this.mBluetoothManager = (BluetoothManager) mConnectivityManager.getSystemService(Context.BLUETOOTH_SERVICE);

    }

    /**
     * close GattServer
     */
    public void close(){

        if (mGattServer != null) {
            disconnectFromDevices();
            mGattServer.close();
        }
        mBleAdvertising.stopAdvertising();
    }

    /**
     * Stop Peripheral - disconnect from all devices and stop advertising
     */
    public void stopPeripheral(){

        if (mGattServer != null) {
            disconnectFromDevices();
            mGattServer.close();
        }
        mBleAdvertising.stopAdvertising();
    }

    /**
     * Start Peripheral - start advertising
     */
    public void startPeripheral(){
        mBleAdvertising.startAdvertising();
        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        if (mGattServer!= null)
            mGattServer.close();
        mGattServer = mBluetoothManager.openGattServer( mConnectivityManager , mGattServerCallback);

        // TODO why does I get null sometimes?
        if(mGattServer == null){
            Log.e(TAG, "ERROR - didn't open gattServer . return null");
        }
        // Add BLEService
        mGattServer.addService(mBluetoothGattService);

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

}
