package com.relay.relay.Bluetooth;

/**
 * Created by omer on 20/12/2016.
 */

/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.relay.relay.ConnectivityManager;

import java.util.Arrays;
import java.util.HashSet;



public class BLEPeripheral implements BLConstants {


    private final String TAG = "RELAY_DEBUG: "+ BLEPeripheral.class.getSimpleName();

    private BluetoothGattService mBluetoothGattService;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private ConnectivityManager mConnectivityManager;
    private BluetoothManager mBluetoothManager;
    private Messenger mMessenger;
    private BluetoothAdapter mBluetoothAdapter;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser mAdvertiser;
    private BLEService mBLEService;
    private BLEAdvertising mBleAdvertising;
    private BluetoothGattServer mGattServer;

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    mGattServer.connect(device,false);
                    // TODO Stop or not . need to check with few devices
                    mBleAdvertising.stopAdvertising();
                    Log.e(TAG, "Connected to device: " + device.getAddress());

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    Log.v(TAG, "Disconnected from device");
                }
            } else {
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.e(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }
//
//        @Override
//        public void onNotificationSent(BluetoothDevice device, int status) {
//            super.onNotificationSent(device, status);
//            Log.v(TAG, "Notification sent. Status: " + status);
//        }
//
//        @Override
//        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
//                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
//                                                 int offset, byte[] value) {
//            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
//                    responseNeeded, offset, value);
//            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
//            int status = 0;//mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value);
//            if (responseNeeded) {
//                mGattServer.sendResponse(device, requestId, status,
//            /* No need to respond with an offset */ 0,
//            /* No need to respond with a value */ null);
//            }
//        }
//
//        @Override
//        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
//                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
//                                             int offset,
//                                             byte[] value) {
//            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
//            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
//                    offset, value);
//            if(responseNeeded) {
//                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
//            /* No need to respond with offset */ 0,
//            /* No need to respond with a value */ null);
//            }
//        }
    };

    BLEPeripheral(BluetoothAdapter bluetoothAdapter, Messenger messenger,ConnectivityManager connectivityManager){

        this.mConnectivityManager = connectivityManager;
        this.mBluetoothDevices = new HashSet<>();
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBLEService = new BLEService(mBluetoothAdapter.getAddress());
        this.mBluetoothGattService = mBLEService.getBluetoothGattService();
        this.mBleAdvertising = new BLEAdvertising(mBluetoothAdapter,mBLEService);

        this.mBluetoothManager = (BluetoothManager) mConnectivityManager.getSystemService(Context.BLUETOOTH_SERVICE);//  bluetoothManager;

        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        this.mGattServer = mBluetoothManager.openGattServer( mConnectivityManager , mGattServerCallback);

        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer.addService(mBluetoothGattService);
    }


    public void close(){

        if (mGattServer != null) {
            disconnectFromDevices();
            mGattServer.close();
            mGattServer = null;
        }
        mBleAdvertising.stopAdvertising();
    }

    public void stopPeripheral(){

        if (mGattServer != null) {
            disconnectFromDevices();
        }
        mBleAdvertising.stopAdvertising();
    }

    public void startPeripheral(){

        mBleAdvertising.startAdvertising();
    }


    public BLEAdvertising getBleAdvertising(){
        return mBleAdvertising;
    }

//    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothDevices.isEmpty()) {
//            Log.d(TAG, "No bluetooth devices connected");
//        } else {
//            boolean indicate = (characteristic.getProperties()
//                    & BluetoothGattCharacteristic.PROPERTY_INDICATE)
//                    == BluetoothGattCharacteristic.PROPERTY_INDICATE;
//            for (BluetoothDevice device : mBluetoothDevices) {
//                // true for indication (acknowledge) and false for notification (unacknowledge).
//                mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
//            }
//        }
//    }

    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
            mGattServer.cancelConnection(device);
        }
    }


//
//
//    /**
//     * Send message to the bluetooth manager
//     */
//    private void sendMessageToManager(int msg)  {
//        try {
//            mMessenger.send(Message.obtain(null, msg));
//        } catch (RemoteException e) {
//            Log.e(TAG, "Error with sendMessageToManager ");
//        }
//    }
}
