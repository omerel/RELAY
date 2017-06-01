
package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
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

import java.util.List;

/**
 * Class for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 * this class play the client side of ble service
 */
public class BLECentral implements BLConstants {
    private final String TAG = "RELAY_DEBUG: "+ BLECentral.class.getSimpleName();

    private RelayConnectivityManager mRelayConnectivityManager;
    private List<String> mLastConnectedDevices;
    private Messenger mManagerMessenger;
    private BLEScan mBleScan;
    // Handler for incoming messages from BleScan
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt mBluetoothGatt;
    List<BluetoothGattService>  mBluetoothGattService;
    List<BluetoothGattCharacteristic>  mBluetoothGattCharacteristic;
    private BluetoothGattCallback mGattCallback;


    /**
     * BLECentral constructor
     * @param bluetoothAdapter to check if bluetooth enable
     * @param messenger to send result to bluetooth manager
     * @param lastConnectedDevices list contains all the devices that sync with this device
     * @param relayConnectivityManager needed to create bluetooth gatt
     */
    BLECentral(BluetoothAdapter bluetoothAdapter, Messenger messenger,
               List<String> lastConnectedDevices,RelayConnectivityManager relayConnectivityManager){
        this.mRelayConnectivityManager = relayConnectivityManager;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mManagerMessenger = messenger;
        this.mLastConnectedDevices = lastConnectedDevices;
        this.mBleScan = new BLEScan(mBluetoothAdapter,mMessenger, relayConnectivityManager);
        initialBluetoothGattCallback();
        Log.d(TAG, "Class created");
    }


    public void initialBluetoothGattCallback(){
        /**
         * Implements callback methods for GATT events that needed dor connection between devices.
         */
        this.mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e(TAG, "Connected to GATT server.( I'm the client side)");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Connected to GATT server.( I'm the client side)");
                    // Attempts to discover services after successful connection.
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Ask GATT server to discover services:" + mBluetoothGatt.discoverServices());
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "Disconnected from GATT server!");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Disconnected from GATT server");
                    //close();

                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "GATT server services Discovered");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": GATT server services Discovered ");

                    // get services
                    mBluetoothGattService =  gatt.getServices();
                    // Find RELAY_SERVICE_UUID and get Characteristics
                    Log.e(TAG, "mBluetoothGattService.size()= "+mBluetoothGattService.size());
                    for (int i = 0; i < mBluetoothGattService.size(); i++ ){
                        Log.e(TAG, "index "+i+ "equal? "+ mBluetoothGattService.get(i).getUuid().equals(RELAY_SERVICE_UUID));
                        if (mBluetoothGattService.get(i).getUuid().equals(RELAY_SERVICE_UUID)) {
                            mBluetoothGattCharacteristic = mBluetoothGattService.get(i).getCharacteristics();
                            Log.e(TAG, "Found RELAY_SERVICE_UUID in index: "+i+". Get MAC_ADDRESS_UUID Characteristic");
                            // read Characteristic
                            // relay BlePeripheral has only one Characteristic ==> index 0
                            Log.e(TAG, "mBluetoothGattCharacteristic.size()= "+mBluetoothGattCharacteristic.size());
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (gatt.getServices().size() != 0) {
                                        gatt.readCharacteristic(mBluetoothGattCharacteristic.get(0));
                                        Log.e(TAG, "Ask from GATT server MAC_ADDRESS_UUID Characteristic");
                                        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE, TAG + ":Ask from GATT server MAC_ADDRESS_UUID Characteristic ");
                                    }
                                }
                            });
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
                    Log.e(TAG, "On Characteristic Read ");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+":Read MAC_ADDRESS_UUID Characteristic from GATT server");
                    if (MAC_ADDRESS_UUID.equals(characteristic.getUuid())) {

                        // get address value
                        String address = new String(characteristic.getValue());

                        // Check if the device is new and we did'nt sync with it
                        if (!mLastConnectedDevices.contains(address)) {
                            sendResultToManager(FOUND_MAC_ADDRESS, address);
                            Log.e(TAG, "Found new device that not in connected list :" + address);
                            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,
                                    TAG+": Found new device that not in connected list :" + address);
                        }
                        else{
                            Log.e(TAG, "The device that is in the connected list :" + address);
                            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,
                                    TAG+": The device that is in the connected list :" + address);
                            // try the next device in results
                            mBleScan.checkResults();
                        }
                    }
                }
            }
        };
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param bluetoothDevice the device that found on ble scan
     * @return true
     */
    public boolean connect(final BluetoothDevice bluetoothDevice) {
        Log.e(TAG, "Connect");
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothGatt = null;
            // disable connection automatically
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt = bluetoothDevice.connectGatt(mRelayConnectivityManager, false, mGattCallback);
                    Log.e(TAG, "Connecting to gatt server ");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,
                            TAG+": Connecting to gatt server in "+ bluetoothDevice.getAddress());
                }
            });
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
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothGatt.close();
            Log.e(TAG, " mBluetoothGatt.close()");
        }
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
                    BluetoothDevice device = msg.getData().getParcelable("result");
                    // connect to gat server device
                    connect(device);
                    break;
                case BLE_SCAN_ERROR:
                    sendResultToManager(BLE_SCAN_ERROR,null);
                    Log.e(TAG, "Error -  BLE_SCAN_ERROR ");
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

}
