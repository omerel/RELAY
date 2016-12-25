package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.ConnectivityManager;
import com.relay.relay.HandShake;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

/**
 * Created by omer on 11/12/2016.
 * The bluetooth manager controls  the ‘when’ and for ‘how long’ relay device is
 * searching for other device in it’s close area, advertising(being discoverable) itself
 * and waiting to accept other devices request for handshake(sync).
 */

public class BlManager extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BlManager.class.getSimpleName();

    //private BLEAdvertising mBLEAdvertising;
    //private BLEScan mBLEScan;
    private BLEPeripheral mBlePeripheral;
    private BLECentral mBLECentral;
    private BluetoothClient mBluetoothClient;
    private BluetoothServer mBluetoothServer;
    private BluetoothAdapter mBluetoothAdapter;
    // List that contains all the devices that were sync in the last
    private List<String>  mLastConnectedDevices;
    // The interval time to search for connection with other device.
    private int mIntervalSearchTime;
    // Counter the number of times that search result returns 0 new device;
    private int mSearchWithoutChangeCounter;
    // Handler for all incoming messages from BL classes
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    // Messenger from ConnectivityManager
    private Messenger mConnectivityMessenger;
    private Handler mHandler;
    private HandShake mHandShake;
    private final String mDeviceUUID;
    private  ConnectivityManager mConnectivityManager;

    public BlManager(String deviceUUID, Messenger connectivityMessenger, ConnectivityManager connectivityManager){

        this.mDeviceUUID = deviceUUID;
        this.mLastConnectedDevices = new ArrayList<>();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to initialize BlManager.");
            exit(1);
        }
        this.mConnectivityManager = connectivityManager;
        this.mBLECentral = new BLECentral(mBluetoothAdapter,mMessenger,mLastConnectedDevices,connectivityManager);
        this.mBlePeripheral = new BLEPeripheral(mBluetoothAdapter,mMessenger,connectivityManager);
        //this.mBLEAdvertising = new BLEAdvertising(mBluetoothAdapter,mMessenger);
        //this.mBLEScan = new BLEScan(mBluetoothAdapter,mMessenger,mLastConnectedDevices);
        this.mBluetoothClient =  null;
        this.mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
        this.mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS;
        this.mSearchWithoutChangeCounter = 0;
        this.mConnectivityMessenger = connectivityMessenger;
        this.mHandler = new Handler();
        this.mHandShake = null;

        this.mBluetoothAdapter.setName(mBluetoothAdapter.getAddress());
        Log.d(TAG, "Class created");

        // TODO
        Log.e(TAG, "I am :" +mBluetoothAdapter.getName()+", MAC : "+ mBluetoothAdapter.getAddress());

    }

    /**
     * Start thread
     */
    @Override
    public void run() {

        // Open server socket
        mBluetoothServer.start();

        startSearchImmediately();

        // TODO delete
//
//         if (mBluetoothAdapter.getAddress().equals("74:23:44:75:27:1D")){
//             // Open server socket
//             if(mBluetoothServer != null )
//                 mBluetoothServer.cancel();
//             mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
//             mBluetoothServer.start();
//         }
//        else{
//             // create bluetooth device with the mac address of the founded device
//             BluetoothDevice bl  = mBluetoothAdapter.getRemoteDevice("74:23:44:75:27:1D");
//             mBluetoothClient =  new BluetoothClient(mMessenger, bl);
//
//             Log.d(TAG, "Connect to device : "+ "74:23:44:75:27:1D");
//
//             mBluetoothClient.start();
//         }


        Log.d(TAG, "Start thread");

    }


    /**
     * Close thread
     */
    public void cancel() {
        stopSearch(FOUND_NEW_DEVICE); // the code used to init the counter
        if (mBluetoothClient != null)
            mBluetoothClient.cancel();
        if (mBluetoothServer != null)
            mBluetoothServer.cancel();
        mBLECentral.close();
        mBlePeripheral.close();
        Log.d(TAG, "Cancel thread");
    }


    /**
     * When service receive Power connection it will update the bluetooth manger in this method
     */
    public void powerConncetionDedected(boolean isConnected){
        if (isConnected){
            mIntervalSearchTime = 6000;
            mSearchWithoutChangeCounter = -100;
            Log.d(TAG, "changed interval setting to connected to power mode");
        }
        else{
            mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS;
            mSearchWithoutChangeCounter = 0;
            Log.d(TAG, "changed interval setting to connected to default");
        }
    }

    /**
     * Start search with timer
     */
    private void startSearchImmediately(){

        // Start advertising
        mBlePeripheral.startPeripheral();

        // Start scan
        mBLECentral.getBleScan().startScanning();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessageToManager(SCAN_FINISHED_WITHOUT_CHANGES);
                Log.d(TAG, "SCAN_FINISHED_WITHOUT_CHANGES");
            }
        }, TIME_RELAY_SCAN_IN_SECONDS);
        Log.d(TAG, "Start startSearchImmediately");
    }


    /**
     * Stop search
     * the code is to deal with the counter
     */
    private void stopSearch( int code ){

        // remove handler callback
        mHandler.removeCallbacksAndMessages(null);

        if (code == SCAN_FINISHED_WITHOUT_CHANGES)
            // add to search counter
            mSearchWithoutChangeCounter++;
        Log.d(TAG, "COUNTER SEARCH WITHOUT CHANGES: "+mSearchWithoutChangeCounter );

        // stop scan
        mBLECentral.getBleScan().stopScanning();

        // stop advertising
        mBlePeripheral.stopPeripheral();


        Log.d(TAG, "StopSearch()");

    }

    /**
     * Start new search with the interval time delayed
     */
    private void intervalSearch(){

        // check search without changes in results counter
        if (mSearchWithoutChangeCounter > MAX_SEARCHS_WITHOUT_CHANGE_BEFORE_CHANGES)
            // set limit to interval search time
            if (mIntervalSearchTime < MAX_TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS) {
                // increase interval search time
                mIntervalSearchTime = mIntervalSearchTime * 2;
                // reset the counter
                mSearchWithoutChangeCounter = 0;
                Log.d(TAG, "mSearchWithoutChangeCounter reach to maximum , interval time changes");
            }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startSearchImmediately();
            }
        }, TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS);

        Log.d(TAG, "Start intervalSearch()");
    }

    /**
     * Send message to the IncomingHandler
     */
    private void sendMessageToManager(int msg)  {
        try {
            mMessenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }

    /**
     * Add Device to last connected list and remove it after
     * TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_SECOND
     */
    private void addToLastConnectedDevicesList(String address){
        mLastConnectedDevices.add(address);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // remove the oldest device in the list
                mLastConnectedDevices.remove(mLastConnectedDevices.get(0));
            }
        }, TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_SECOND);

        Log.d(TAG, "device added to connected devices list");

    }


    /**
     *  Reset interval search time and counter
     */
    private void resetSearch()  {
        mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS;
        mSearchWithoutChangeCounter = 0;

        Log.d(TAG, "reset mIntervalSearchTime and mSearchWithoutChangeCounter ");

    }


    /**
     * Send relay message to the ConnectivityManager class
     */
    private void sendRelayMessageToConnectivityManager(int m,String relayMessage)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("relayMessage", relayMessage);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mConnectivityMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendRelayMessageToConnectivityManager ");
        }
    }

    /**
     * Handler of incoming messages from one of the BL classes
     */
    class IncomingHandler extends Handler {

        BluetoothSocket bluetoothSocket;
        String address;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER:

                    // cancel socket
                    mBluetoothServer.cancel();

                    bluetoothSocket = mBluetoothServer.getBluetoothSocket();

                    stopSearch(FOUND_NEW_DEVICE);

                    // reset interval search time and counter
                    resetSearch();

                    // Start handshake
                    mHandShake = new HandShake(mDeviceUUID,bluetoothSocket,mMessenger);

                    break;

                case FAILED_CONNECTING_TO_DEVICE:

                    startSearchImmediately();

                    break;

                case SUCCEED_CONNECTING_TO_DEVICE:

                    bluetoothSocket = mBluetoothClient.getBluetoothSocket();

                    // cancel socket if working
                    mBluetoothServer.cancel();

                    // Start handshake
                    mHandShake = new HandShake(mDeviceUUID,bluetoothSocket,mMessenger);

                    break;

                case FOUND_MAC_ADDRESS:

                    stopSearch(FOUND_NEW_DEVICE);

                    // reset interval search time and counter
                    resetSearch();

                    address = msg.getData().getString("address");
                    Log.d(TAG, "Found MAC device : "+ address);

                    // create bluetooth device with the mac address of the founded device
                    BluetoothDevice bl  = mBluetoothAdapter.getRemoteDevice(address);
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    mBluetoothClient =  new BluetoothClient(mMessenger, bl);

                    Log.d(TAG, "Connect to device : "+ address);

                    mBluetoothClient.start();

                    break;

                case SCAN_FINISHED_WITHOUT_CHANGES:
                    stopSearch(SCAN_FINISHED_WITHOUT_CHANGES);
                    intervalSearch();
                    break;

                case FINISHED_HANDSHAKE:

                    address = msg.getData().getString("address");
                    addToLastConnectedDevicesList(address);

                    Log.e(TAG, "THERE IS :" + mLastConnectedDevices.size() +" DEVICES IN LIST");

                    // close handShake connection
                    mHandShake.closeConnection();

                    // Open server socket
                    mBluetoothServer.cancel();
                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
                    mBluetoothServer.start();

                    startSearchImmediately();

                    break;

                case READ_PACKET:
                    // test
                    Log.e(TAG, "Read Packet in BLManager");
                    byte[] packet = msg.getData().getByteArray("packet");
                    // update handshake with the new packet
                    mHandShake.getPacket(packet);
                    break;

                case NEW_RELAY_MESSAGE:

                    // TODO change string to relay message
                    Log.e(TAG, "Received  new relay message from Handshake");
                    String relayMessage = msg.getData().getString("relayMessage");

                    sendRelayMessageToConnectivityManager(NEW_RELAY_MESSAGE,relayMessage);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
