package com.relay.relay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by omer on 11/12/2016.
 * The bluetooth manager controls  the ‘when’ and for ‘how long’ relay device is
 * searching for other device in it’s close area, advertising(being discoverable) itself
 * and waiting to accept other devices request for handshake(sync).
 */

public class BluetoothManager extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BluetoothManager.class.getSimpleName();

    private BluetoothHandShake mBluetoothHandShake;
    private BLEAdvertising mBLEAdvertising;
    private BLEScan mBLEScan;
    private BluetoothConnect mBluetoothConnect;
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
    private Handler mHandler;

    BluetoothManager(){

        this.mLastConnectedDevices = new ArrayList<>();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mBLEAdvertising = new BLEAdvertising(mBluetoothAdapter,mMessenger);
        this.mBLEScan = new BLEScan(mBluetoothAdapter,mMessenger,mLastConnectedDevices);
        this.mBluetoothConnect =  null;
        this.mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
        this.mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS;
        this.mSearchWithoutChangeCounter = 0;
        this.mHandler = new Handler();

        Log.d(TAG, "Class created");
    }

    /**
     * Start thread
     */
    @Override
    public void run() {
        super.run();
        startSearchImmediately();
        Log.d(TAG, "Start thread");

    }

    /**
     * Close thread
     */
    public void cancel() {
        stopSearch(0);
        mBluetoothConnect.cancel();
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
        mBLEAdvertising.startAdvertising();

        // Open server socket
        mBluetoothServer.start();

        // Start scan
        mBLEScan.startScanning();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessageToManager(SCAN_FINISHED_WITHOUT_CHANGES);
            }
        }, TIME_RELAY_SCAN_IN_SECONDS);
        Log.d(TAG, "Start startSearchImmediately");
    }


    /**
     * Stop search
     * code == -1  : search finished without changes
     * code == 0 : search stopped
     */
    private void stopSearch( int code ){

        // remove handler callback
        mHandler.removeCallbacksAndMessages(null);

        if (code == -1)
            // add to search counter
            mSearchWithoutChangeCounter++;

        // stop scan
        mBLEScan.stopScanning();

        // stop advertising
        mBLEAdvertising.stopAdvertising();

        // cancel socket
        mBluetoothServer.cancel();

        Log.d(TAG, "Start stopSearch()");

    }

    /**
     * Start new search with the interval time delayed
     */
    private void intervalSearch(){

        // check search without changes in results counter
        if (mSearchWithoutChangeCounter > MAX_SEARCHS_WITHOUT_CHANGE)
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
     * TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_MINUTES
     */
    private void addToLastConnectedDevicesList(String address){
        mLastConnectedDevices.add(address);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // remove the oldest device in the list
                mLastConnectedDevices.remove(mLastConnectedDevices.get(0));
            }
        }, TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_MINUTES);

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
     * Handler of incoming messages from one of the BL classes
     */
    class IncomingHandler extends Handler {

        BluetoothSocket bluetoothSocket;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER:

                    stopSearch(0);

                    // reset interval search time and counter
                    resetSearch();

                    bluetoothSocket = mBluetoothServer.getBluetoothSocket();

                    // create bluetooth handshake and start thread
                    mBluetoothHandShake = new BluetoothHandShake(bluetoothSocket,mMessenger);
                    mBluetoothHandShake.start();
                    break;

                case FAILED_CONNECTING_TO_DEVICE:

                    startSearchImmediately();

                    break;

                case SUCCEED_CONNECTING_TO_DEVICE:

                    bluetoothSocket = mBluetoothConnect.getBluetoothSocket();

                    // create bluetooth handshake and start thread
                    mBluetoothHandShake = new BluetoothHandShake(bluetoothSocket,mMessenger);
                    mBluetoothHandShake.start();

                    break;

                case FOUND_NEW_DEVICE:

                    stopSearch(0);

                    // reset interval search time and counter
                    resetSearch();

                    ScanResult result = msg.getData().getParcelable("result");

                    mBluetoothConnect =  new BluetoothConnect(mBluetoothAdapter,mMessenger,
                            result.getDevice());

                    mBluetoothConnect.start();
                    break;

                case SCAN_FINISHED_WITHOUT_CHANGES:
                    stopSearch(-1);
                    intervalSearch();
                    break;

                case FINISHED_HANDSHAKE:
                    String address = msg.getData().getString("address");
                    addToLastConnectedDevicesList(address);

                    startSearchImmediately();

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }
}
