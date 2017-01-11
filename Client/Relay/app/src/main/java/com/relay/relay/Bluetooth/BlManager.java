package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
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
 * The bluetooth manager controls the ‘when’ and for ‘how long’ relay device is
 * searching for other device in it’s close area, advertising(being discoverable) itself
 * and waiting to accept other devices request for handshake(sync).
 */

public class BLManager extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BLManager.class.getSimpleName();

    private BLEPeripheral mBlePeripheral;
    private BLECentral mBLECentral;
    private BluetoothScan mBluetoothScan;
    private BluetoothClient mBluetoothClient;
    private BluetoothServer mBluetoothServer;
    private BluetoothAdapter mBluetoothAdapter;
    // List that contains all the devices that were sync in the last
    private List<String>  mLastConnectedDevices;
    // The interval time to search for connection with other device.
    private int mIntervalSearchTime;
    // Time relay keeps found devices that synced in waiting list
    private int mWaitingList;
    // Counter the number of times that search result returns 0 new device;
    private int mSearchWithoutChangeCounter;
    // Handler for all incoming messages from BL classes
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    // Messenger from ConnectivityManager
    private Messenger mConnectivityMessenger;
    private Handler mHandler;
    private HandShake mHandShake;


    private String mDeviceUUID;
    private int mStatus;
    private ConnectivityManager mConnectivityManager;
    private boolean mInitiator;


     public BLManager(String deviceUUID, Messenger connectivityMessenger, ConnectivityManager connectivityManager){

        this.mDeviceUUID = deviceUUID;
        this.mLastConnectedDevices = new ArrayList<>();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to initialize BLManager.");
            exit(1);
        }
         this.mConnectivityManager = connectivityManager;
         this.mBLECentral = new BLECentral(mBluetoothAdapter,mMessenger,mLastConnectedDevices,connectivityManager);
         this.mBlePeripheral = new BLEPeripheral(mBluetoothAdapter,mMessenger,connectivityManager);
         this.mBluetoothScan = new BluetoothScan(mBluetoothAdapter,mMessenger,connectivityManager);
         this.mBluetoothClient =  null;
         this.mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
         this.mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL;
         this.mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST;
         this.mSearchWithoutChangeCounter = 0;
         this.mConnectivityMessenger = connectivityMessenger;
         this.mHandler = new Handler();
         this.mHandShake = null;
         this.mStatus = DISCONNECTED;
         Log.d(TAG, "Class created");
         Log.d(TAG, "I am :" +mBluetoothAdapter.getName()+", MAC : "+ mBluetoothAdapter.getAddress());

    }


    //
    // Start thread
    @Override
    public void run() {

        // Open server socket
        mBluetoothServer.start();
        checkSupport();
        startSearchImmediately();
        Log.d(TAG, "Start thread");
    }

    private void checkSupport() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()){
            //Device does not support Bluetooth LE
        }

    }


    // Close thread
    public void cancel() {
        stopSearch(FOUND_NEW_DEVICE); // the parameter used to init the counter
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
     * @param isConnected boolean connect to power
     */
    public void powerConnectionDetected(boolean isConnected){
        if (isConnected){
            mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_POWER_MODE;
            mSearchWithoutChangeCounter = -1000;
            mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_POWER_MODE;
            // clear connect devices list
//            mLastConnectedDevices.clear();
//            if (mStatus == DISCONNECTED) {
//                stopSearch(RESET_SEARCH_COUNTER);
//                resetSearch();
//                intervalSearch();
//            }
            Log.d(TAG, "changed interval setting to connected to power mode");
        }
        else{
            mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL;
            mSearchWithoutChangeCounter = 0;
            mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST;
            // clear connect devices list
//            mLastConnectedDevices.clear();
//            if (mStatus == DISCONNECTED) {
//                stopSearch(RESET_SEARCH_COUNTER);
//                resetSearch();
//                intervalSearch();
//            }
            Log.d(TAG, "changed interval setting to connected to default");
        }
    }

    /**
     * Start search with timer
     */
    private void startSearchImmediately(){

        // Start advertising
        mBlePeripheral.startPeripheral();

        mBLECentral.getBleScan().startScanning();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessageToManager(SCAN_FINISHED_WITHOUT_CHANGES);
                Log.d(TAG, "SCAN_FINISHED_WITHOUT_CHANGES");
            }
        }, TIME_RELAY_SCAN);
        Log.d(TAG, "Start startSearchImmediately");
    }



    /**
     * Stop search
     * @param stopReason parameter deals with the  search counter
     */
    private void stopSearch( int stopReason ){

        // remove handler callback
        mHandler.removeCallbacksAndMessages(null);
        if (stopReason == SCAN_FINISHED_WITHOUT_CHANGES)
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
        if (mSearchWithoutChangeCounter > MAX_SEARCH_WITHOUT_CHANGE_COUNTER)
            // set limit to interval search time
            if (mIntervalSearchTime < MAX_TIME_RELAY_SEARCH_INTERVAL) {
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
        }, mIntervalSearchTime);

        Log.d(TAG, "Start intervalSearch()");
    }

    /**
     *
     * @param msg to bluetooth manager IncomingHandler
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
     * mWaitingList
     */
    private void addToLastConnectedDevicesList(String address){
        mLastConnectedDevices.add(address);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // remove the oldest device in the list
                mLastConnectedDevices.remove(mLastConnectedDevices.get(0));
                Log.e(TAG, " Device removed from connected devices list ");
            }
        }, mWaitingList);

        Log.d(TAG, "device added to connected devices list");

    }


    /**
     *  Reset interval search time and counter
     */
    private void resetSearch()  {
        // TODO need to check if it in power or not
        mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_POWER_MODE;
        mSearchWithoutChangeCounter = 0;

        Log.d(TAG, "Reset mIntervalSearchTime and mSearchWithoutChangeCounter ");

    }

    /**
     * Send relay message to the ConnectivityManager class
     * @param m message
     * @param relayMessage
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

    public void startManualSync() {
        if(mStatus == DISCONNECTED) {
            stopSearch(RESET_SEARCH_COUNTER);
            mBluetoothScan.startScan();
        }
    }


    /**
     * Handler of incoming messages from one of the BL classes
     */
    class IncomingHandler extends Handler {

        BluetoothSocket bluetoothSocket;
        String address;
        BluetoothDevice bl;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER:
                    Log.e(TAG, "DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER");
                    // this device is the  server. not the Initiator
                    mInitiator = false;
                    // update status
                    mStatus = CONNECTED;
                    // cancel socket
                    mBluetoothServer.cancel();
                    bluetoothSocket = mBluetoothServer.getBluetoothSocket();
                    stopSearch(FOUND_NEW_DEVICE);
                    // reset interval search time and counter
                    resetSearch();
                    // Start handshake
                    mHandShake = new HandShake(mDeviceUUID,bluetoothSocket,mMessenger,mInitiator);
                    break;

                case FAILED_CONNECTING_TO_DEVICE:
                    Log.e(TAG, "FAILED_CONNECTING_TO_DEVICE");
                    // update status
                    mStatus = DISCONNECTED;
                    // Open server socket
                    mBluetoothServer.cancel();
                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
                    mBluetoothServer.start();
                    startSearchImmediately();
                    break;

                case SUCCEED_CONNECTING_TO_DEVICE:
                    Log.e(TAG, "SUCCEED_CONNECTING_TO_DEVICE");
                    // this device is the  Initiator
                    mInitiator = true;
                    // update status
                    mStatus = CONNECTED;
                    bluetoothSocket = mBluetoothClient.getBluetoothSocket();
                    // cancel socket if working
                    mBluetoothServer.cancel();
                    // Start handshake
                    mHandShake = new HandShake(mDeviceUUID,bluetoothSocket,mMessenger,mInitiator);

                    break;

                case FOUND_MAC_ADDRESS:
                    Log.e(TAG, "FOUND_MAC_ADDRESS");
                    // update status
                    mStatus = CONNECTING;
                    stopSearch(FOUND_NEW_DEVICE);
                    // reset interval search time and counter
                    resetSearch();
                    address = msg.getData().getString("address");
                    Log.d(TAG, "Found MAC device : "+ address);

                    // create bluetooth device with the mac address of the founded device
                    bl  = mBluetoothAdapter.getRemoteDevice(address);
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    mBluetoothClient =  new BluetoothClient(mMessenger, bl);

                    Log.d(TAG, "Connect to device : "+ address);
                    mBluetoothClient.start();
                    break;

                case SCAN_FINISHED_WITHOUT_CHANGES:
                    Log.e(TAG, "SCAN_FINISHED_WITHOUT_CHANGES");
                    stopSearch(SCAN_FINISHED_WITHOUT_CHANGES);
                    intervalSearch();
                    break;

                case FINISHED_HANDSHAKE:
                    Log.e(TAG, "FINISHED_HANDSHAKE");
                    // update status
                    mStatus = DISCONNECTED;
                    address = msg.getData().getString("address");
                    addToLastConnectedDevicesList(address);
                    // close handShake connection
                    mHandShake.closeConnection();
                    // Open server socket
                    mBluetoothServer.cancel();
                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
                    //
                    if (!mBluetoothServer.isAlive())
                        mBluetoothServer.start();

                    intervalSearch();


//                    if (mInitiator)
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                // TODO sometimes thread  is alive. prevent error
//                                if (!mBluetoothServer.isAlive())
//                                    mBluetoothServer.start();
//                                startSearchImmediately();
//                            }
//                        }, DELAY_AFTER_HANDSHAKE);
//                    else{
//                        if (!mBluetoothServer.isAlive())
//                            mBluetoothServer.start();
//                        startSearchImmediately();
//                    }
                    break;

                case READ_PACKET:
                    Log.e(TAG, "READ_PACKET");

                    // test
                    Log.d(TAG, "Read Packet in BLManager");
                    String packet = msg.getData().getString("packet");
                    // update handshake with the new packet
                    mHandShake.getPacket(packet);
                    break;

               case NEW_RELAY_MESSAGE:
                   Log.e(TAG, "NEW_RELAY_MESSAGE");
                   // When the message is for this device
                    Log.d(TAG, "Received  new relay message from Handshake");
                    String relayMessage = msg.getData().getString("relayMessage");
                    sendRelayMessageToConnectivityManager(NEW_RELAY_MESSAGE,relayMessage);
                    break;

                case BLE_ADVERTISE_ERROR:
                    Log.e(TAG, "BLE_ADVERTISE_ERROR");
                    sendRelayMessageToConnectivityManager(BLE_ERROR,null);
                    break;
                case BLE_SCAN_ERROR:
                    Log.e(TAG, "BLE_SCAN_ERROR");
                    sendRelayMessageToConnectivityManager(BLE_ERROR,null);
                    break;

                case FOUND_MAC_ADDRESS_FROM_BLSCAN:

                    Log.e(TAG, "FOUND_MAC_ADDRESS FROM BL SCAN");
                    // update status
                    mStatus = CONNECTING;

                    address = msg.getData().getString("address");
                    Log.d(TAG, "Found MAC device : "+ address);

                    // create bluetooth device with the mac address of the founded device
                    bl  = mBluetoothAdapter.getRemoteDevice(address);
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    mBluetoothClient =  new BluetoothClient(mMessenger, bl);

                    Log.d(TAG, "Connect to device : "+ address);
                    mBluetoothClient.start();
                    break;

                case NOT_FOUND_ADDRESS_FROM_BLSCAN:
                    Log.d(TAG, "NOT_FOUND_ADDRESS_FROM_BLSCAN");
                    mStatus = DISCONNECTED;
                    intervalSearch();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }
}
