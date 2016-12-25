
package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.ConnectivityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BLECentral implements BLConstants {
    private final String TAG = "RELAY_DEBUG: "+ BLECentral.class.getSimpleName();
    private ConnectivityManager mConnectivityManager;
    private List<String> mLastConnectedDevices;
    private Messenger mManagerMessenger;
    private BLEScan mBleScan;
    // Handler for incoming messages from BleScan
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    List<BluetoothGattService>  mBluetoothGattService;
    List<BluetoothGattCharacteristic>  mBluetoothGattCharacteristic;
    BluetoothGattCharacteristic  mAddressCharacteristic;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
   // private int mConnectionState = STATE_DISCONNECTED;


    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

//    private static final int STATE_DISCONNECTED = 0;
//    private static final int STATE_CONNECTING = 1;
//    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

//    public final static UUID UUID_MAC_SERVICE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
//    public final static UUID UUID_MAC_ADDRESS = UUID.fromString("00002a33-0000-2000-8000-00805f9b34fb");

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {

             //   mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                close();
              //  mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services Discovered");
                // get services
                mBluetoothGattService =  gatt.getServices();
                // get Characteristic
                // relay BlePeripheral has only one service ==> index 0 // TODO why index 3??? what is 0,1,2???
                mBluetoothGattCharacteristic = mBluetoothGattService.get(3).getCharacteristics();
                Log.e(TAG, "get Characteristic "+mBluetoothGattService.size()+mBluetoothGattCharacteristic.size());
                // read Characteristic
                // relay BlePeripheral has only one Characteristic ==> index 0
                //gatt.setCharacteristicNotification(mBluetoothGattCharacteristic.get(0), true);
                gatt.readCharacteristic(mBluetoothGattCharacteristic.get(0));
                Log.e(TAG, "read Characteristic");

            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "On Characteristic Read ");
                if (MAC_ADDRESS_UUID.equals(characteristic.getUuid())) {

                    // get address value
                    String address = new String(characteristic.getValue());

                    Log.e(TAG, "THERE IS :" + mLastConnectedDevices.size() +" DEVICES IN LIST");
                    Log.e(TAG, "THE NEW ADDRESS ARE IN THE LIST? :" + mLastConnectedDevices.contains(address));
                    if (!mLastConnectedDevices.contains(address)) {
                        sendResultToManager(FOUND_MAC_ADDRESS, address);
                        Log.d(TAG, "Connect BL, Found new device that not in connected list :" + address);
                    }
                    else{
                        Log.d(TAG, "DO nothing, found device that is in connected list :" + address);
                    }

                }

                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,
//                                            BluetoothGattCharacteristic characteristic) {
//            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//        }
    };

    BLECentral(BluetoothAdapter bluetoothAdapter, Messenger messenger,
               List<String> lastConnectedDevices,ConnectivityManager connectivityManager){
        this.mConnectivityManager = connectivityManager;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mManagerMessenger = messenger;
        this.mLastConnectedDevices = lastConnectedDevices;
        this.mBleScan = new BLEScan(mBluetoothAdapter,mMessenger);
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     */
    public boolean connect(BluetoothDevice bluetoothDevice) {
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = bluetoothDevice.connectGatt(mConnectivityManager, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
       // mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if ( mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBleScan.stopScanning();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

//    /**
//     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
//     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
//     * callback.
//     *
//     * @param characteristic The characteristic to read from.
//     */
//    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.readCharacteristic(characteristic);
//    }

//    /**
//     * Enables or disables notification on a give characteristic.
//     *
//     * @param characteristic Characteristic to act on.
//     * @param enabled If true, enable notification.  False otherwise.
//     */
//    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
//                                              boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//
//    }

//    /**
//     * Retrieves a list of supported GATT services on the connected device. This should be
//     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
//     *
//     * @return A {@code List} of supported services.
//     */
//    public List<BluetoothGattService> getSupportedGattServices() {
//        if (mBluetoothGatt == null) return null;
//        return mBluetoothGatt.getServices();
//    }

    /**
     * Send MAC address value to bluetooth manager
     */
    private void sendResultToManager(int m,String address)  {

        // Send data
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mManagerMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendResultToManager ");
        }
    }

    // BLEScan getter
    public BLEScan getBleScan(){
        return mBleScan;
    }

    /**
     * Handler of incoming messages from BleScan
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case FOUND_NEW_DEVICE:
                    ScanResult result = msg.getData().getParcelable("result");
                    // connect to device
                    connect(result.getDevice());
                    //sendResultToManager(FOUND_MAC_ADDRESS, result.getDevice().getName());
                    // TODO delete
                    Log.d(TAG, "Try to connect to device and get bl MAC address");
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

}
