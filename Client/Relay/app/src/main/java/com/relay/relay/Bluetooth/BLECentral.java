
package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.ConnectivityManager;

import java.util.List;

/**
 * Class for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 * this class play the client side of ble service
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
    private BluetoothGatt mBluetoothGatt;
    List<BluetoothGattService>  mBluetoothGattService;
    List<BluetoothGattCharacteristic>  mBluetoothGattCharacteristic;


    /**
     * Implements callback methods for GATT events that needed dor connection between devices.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "Connected to GATT server.(CLIENT SIDE)");
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server!");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services Discovered");
                // get services
                mBluetoothGattService =  gatt.getServices();
                // Find RELAY_SERVICE_UUID and get Characteristics
                for (int i = 0; i < mBluetoothGattService.size(); i++ ){
                    if (mBluetoothGattService.get(i).getUuid().equals(RELAY_SERVICE_UUID))
                        mBluetoothGattCharacteristic = mBluetoothGattService.get(i).getCharacteristics();
                }
                Log.d(TAG, "Get Characteristic ");
                // read Characteristic
                // relay BlePeripheral has only one Characteristic ==> index 0
                for (int i = 0; i < mBluetoothGattCharacteristic.size(); i++ ){
                    if (mBluetoothGattCharacteristic.get(i).getUuid().equals(MAC_ADDRESS_UUID)) {
                        gatt.readCharacteristic(mBluetoothGattCharacteristic.get(i));
                        Log.d(TAG, "Read Characteristic");
                    }
                }

            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "On Characteristic Read ");
                if (MAC_ADDRESS_UUID.equals(characteristic.getUuid())) {

                    // get address value
                    String address = new String(characteristic.getValue());

                    // Check if the device is new and we did'nt sync with it
                    if (!mLastConnectedDevices.contains(address)) {
                        sendResultToManager(FOUND_MAC_ADDRESS, address);
                        Log.d(TAG, "Found new device that not in connected list :" + address);
                    }
                    else{
                        Log.e(TAG, "Found device that is in the connected list :" + address+
                                "\n restart scan.");
                        // Wait to the next interval with scanning and let others find you
                    }
                }
            }
        }
    };

    /**
     * BLECentral constructor
     * @param bluetoothAdapter to check if bluetooth enable
     * @param messenger to send result to bluetooth manager
     * @param lastConnectedDevices list contains all the devices that sync with this device
     * @param connectivityManager needed to create bluetooth gatt
     */
    BLECentral(BluetoothAdapter bluetoothAdapter, Messenger messenger,
               List<String> lastConnectedDevices,ConnectivityManager connectivityManager){
        this.mConnectivityManager = connectivityManager;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mManagerMessenger = messenger;
        this.mLastConnectedDevices = lastConnectedDevices;
        this.mBleScan = new BLEScan(mBluetoothAdapter,mMessenger);
        Log.d(TAG, "Class created");
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param bluetoothDevice the device that found on ble scan
     * @return true
     */
    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (mBluetoothAdapter.isEnabled()) {
            // disable connection automatically
            mBluetoothGatt = bluetoothDevice.connectGatt(mConnectivityManager, false, mGattCallback);
            Log.d(TAG, "Connecting to gatt server ");
        }
        return true;
    }


    /**
     * close (include stop scanning)
     */
    public void close() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBleScan.stopScanning();
        if (mBluetoothAdapter.isEnabled())
            mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Send MAC address value to bluetooth manager
     * @param m message
     * @param address  Mac address of the new device
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
            Log.e(TAG, "Error -  sendResultToManager ");
        }
    }

    /**
     * BlESCan Getter
     * @return BLEScan
     */
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
                    // connect to gat server device
                    connect(result.getDevice());
                    break;
                case BLE_SCAN_ERROR:
                    mBleScan.stopScanning();
                    sendResultToManager(BLE_SCAN_ERROR,null);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

}
