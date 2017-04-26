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
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.HandShake;
import com.relay.relay.Util.DataTransferred;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    // Messenger from RelayConnectivityManager
    private Messenger mConnectivityMessenger;
    private Handler mHandler;
    private Handler mAdvertiserHandler;
    private HandShake mHandShake;
    public int mStatus;
    private RelayConnectivityManager mRelayConnectivityManager;
    // who connect(initiate) to who
    private boolean mInitiator;
    private DataManager mDataManager;
    private DataTransferred mDataTransferred;
    private DataTransferred.Metadata metadata; // calculate in BLManager saves time in handshake


     public BLManager(Messenger connectivityMessenger, RelayConnectivityManager relayConnectivityManager){

        this.mLastConnectedDevices = new ArrayList<>();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to initialize BLManager.");
            exit(1);
        }
         this.mRelayConnectivityManager = relayConnectivityManager;
         this.mBLECentral = new BLECentral(mBluetoothAdapter,mMessenger,mLastConnectedDevices, relayConnectivityManager);
         this.mBlePeripheral = new BLEPeripheral(mBluetoothAdapter,mMessenger, relayConnectivityManager);
         this.mBluetoothScan = new BluetoothScan(mBluetoothAdapter,mMessenger, relayConnectivityManager);
         this.mBluetoothClient =  null;
         this.mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
         this.mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL;
         this.mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST;
         this.mSearchWithoutChangeCounter = 0;
         this.mConnectivityMessenger = connectivityMessenger;
         this.mHandler = new Handler();
         this.mAdvertiserHandler = new Handler();
         this.mHandShake = null;
         this.mStatus = DISCONNECTED;
         this.mDataManager = new DataManager(relayConnectivityManager);
         this.mDataTransferred = new DataTransferred(mDataManager.getGraphRelations(),
                 mDataManager.getNodesDB(),mDataManager.getMessagesDB());
         this.metadata = mDataTransferred.createMetaData();
         Log.d(TAG, "Class created");
         Log.d(TAG, "I am :" +mBluetoothAdapter.getName()+", MAC : "+ mBluetoothAdapter.getAddress());
    }

    // Start thread
    @Override
    public void run() {

        // Open server socket
//        mBluetoothServer.start();
//        checkSupport();

        // Start advertising
        mBlePeripheral.startPeripheral();
        checkAdvertiser(60000);

        startSearchImmediately();
        Log.d(TAG, "Start thread");
    }

    private void checkSupport() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()){
            //Device does not support Bluetooth LE
        }

    }

    private void checkAdvertiser(int time){
        final int newTime = time;
        mAdvertiserHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBlePeripheral.startPeripheral();
                checkAdvertiser(newTime);
                Log.d(TAG, "Restart Advertisement if needed");
            }
        }, time);

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
        mAdvertiserHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Cancel thread");
    }

    /**
     * When service receive Power connection it will update the bluetooth manger in this method
     * @param isConnected boolean connect to power
     */
    public void powerConnectionDetected(boolean isConnected){
        if (isConnected){
            mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL_POWER_MODE;
            mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_POWER_MODE;
            Log.d(TAG, "changed interval setting to connected to power mode");
        }
        else{
            mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL;
            mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST;
            Log.d(TAG, "changed interval setting to connected to default");
        }
    }

    /**
     * Start search with timer
     */
    public void startSearchImmediately(){
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
        Log.d(TAG, "StopSearch()");

    }

    /**
     * Start new search with the interval time delayed
     */
    private void intervalSearch(){
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
        mSearchWithoutChangeCounter = 0;
        Log.d(TAG, "Reset mIntervalSearchTime and mSearchWithoutChangeCounter ");

    }

    /**
     * Send relay message to the RelayConnectivityManager class
     * @param m message
     * @param relayMessage
     */
    private void sendMessageToConnectivityManager(int m, String relayMessage)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("relayMessage", relayMessage);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mConnectivityMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToConnectivityManager ");
        }
    }

    public void startManualSync() {
        stopSearch(RESET_SEARCH_COUNTER);
        mBluetoothScan.startScan();
    }

    //  mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

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
                    mHandShake = new HandShake(bluetoothSocket,mMessenger,mInitiator,
                            mRelayConnectivityManager,mDataManager,metadata);
                    break;

                case FAILED_CONNECTING_TO_DEVICE:
                    Log.e(TAG, "FAILED_CONNECTING_TO_DEVICE");
                    // update status
                    mStatus = DISCONNECTED;
//                    // Open server socket
//                    mBluetoothServer.cancel();
//                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
//                    mBluetoothServer.start();
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
                    mHandShake = new HandShake(bluetoothSocket,mMessenger,mInitiator,
                            mRelayConnectivityManager,mDataManager,metadata);

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
                    mStatus = DISCONNECTED;
                    stopSearch(SCAN_FINISHED_WITHOUT_CHANGES);
                    intervalSearch();
                    break;

                case FINISHED_HANDSHAKE:
                    Log.e(TAG, "FINISHED_HANDSHAKE");
                    // update status
                    mStatus = DISCONNECTED;

                    // update service finish handshake
                    sendMessageToConnectivityManager(FINISHED_HANDSHAKE,null);

                    address = msg.getData().getString("address");
                    UUID deviceUUID = UUID.fromString(msg.getData().getString("deviceUUID"));
                    addToLastConnectedDevicesList(address);

                    // close handShake connection
                    mHandShake.closeConnection();
                    // update metadata
                    metadata = mDataTransferred.createMetaData();
                    // Open server socket
                    mBluetoothServer.cancel();
//                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
                    if (mInitiator)
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // TODO sometimes thread is alive. prevent error
                              //  if (!mBluetoothServer.isAlive())
                                 //   mBluetoothServer.start();
                                intervalSearch();
                            }
                        }, DELAY_AFTER_HANDSHAKE);
                    else{
                      //  mBluetoothServer.start();
                        intervalSearch();
                    }
                    break;

                case READ_PACKET:
                    Log.e(TAG, "READ_PACKET");
                    // Test
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
                    sendMessageToConnectivityManager(NEW_RELAY_MESSAGE,relayMessage);
                    break;

                case BLE_ADVERTISE_ERROR:
                    Log.e(TAG, "BLE_ADVERTISE_ERROR");
                    mStatus = DISCONNECTED;
                    sendMessageToConnectivityManager(BLE_ERROR,null);
                    break;

                case BLE_SCAN_ERROR:
                    Log.e(TAG, "BLE_SCAN_ERROR");
                    mStatus = DISCONNECTED;
                    sendMessageToConnectivityManager(BLE_ERROR,null);
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
                case GET_BLUETOOTH_SERVER_READY:
                    // Open server socket
                    mBluetoothServer.cancel();
                    mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger);
                    mBluetoothServer.start();
                    break;
                case FAILED_DURING_HAND_SHAKE:
                    Log.e(TAG, "FAILED_DURING_HAND_SHAKE");
                    // update status
                    mStatus = DISCONNECTED;
                    startSearchImmediately();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
// TODO delete all the  mBluetoothServer that in comment
