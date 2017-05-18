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
import com.relay.relay.Util.MacAddressFinder;
import com.relay.relay.viewsAndViewAdapters.StatusBar;

import java.io.IOException;
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
    // Messenger from RelayConnectivityManager
    private Messenger mConnectivityMessenger;
    private Handler mHandler;
    private Handler mScanHandler;
    private Handler mAdvertiserHandler;
    private HandShake mHandShake;
    public int mStatus;
    private RelayConnectivityManager mRelayConnectivityManager;
    // who connect(initiate) to who
    private boolean mInitiator;
    private DataManager mDataManager;
    private DataTransferred mDataTransferred;
    private DataTransferred.Metadata metadata; // calculate in BLManager saves time

    // in handshake


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
         this.mBluetoothServer = new BluetoothServer(mBluetoothAdapter,mMessenger,mRelayConnectivityManager);
         this.mIntervalSearchTime = TIME_RELAY_SEARCH_INTERVAL;
         this.mWaitingList = TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST;
         this.mSearchWithoutChangeCounter = 0;
         this.mConnectivityMessenger = connectivityMessenger;
         this.mHandler = new Handler();
         this.mScanHandler = new Handler();
         this.mAdvertiserHandler = new Handler();
         this.mHandShake = null;
         this.mStatus = DISCONNECTED;
         this.mDataManager = new DataManager(relayConnectivityManager);
         this.mDataTransferred = new DataTransferred(mDataManager.getGraphRelations(),
                 mDataManager.getNodesDB(),mDataManager.getMessagesDB());
         this.metadata = mDataTransferred.createMetaData();

         Log.d(TAG, "Class created");
         Log.d(TAG, "I am :" +mBluetoothAdapter.getName()+", MAC : "+ MacAddressFinder.getBluetoothMacAddress());
    }

    // Start thread
    @Override
    public void run() {
        // Start advertising
         startPeripheral();
         //start interval search
         intervalSearch();
        Log.d(TAG, "Start thread");
    }


    private void startPeripheral(){

        if (BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()){
            if (mStatus != CONNECTED || mStatus != CONNECTING){
                mBlePeripheral.startPeripheral();
            }
        }
        mAdvertiserHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startPeripheral();
                Log.d(TAG, "Restart Advertisement if needed");
            }
        }, 60000); // 1 min

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
     * Start new search with the interval time delayed
     */
    private void intervalSearch(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mStatus != CONNECTED || mStatus != CONNECTING)
                    startSearchImmediately();
                intervalSearch();
            }
        }, mIntervalSearchTime);
        Log.e(TAG, "Start intervalSearch()");
    }

    /**
     * Start search with timer
     */
    public void startSearchImmediately(){
        mBLECentral.getBleScan().startScanning();
        mScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mBLECentral.getBleScan().isResultsInQueue())
                    mBLECentral.getBleScan().checkResults();
                else
                    sendMessageToManager(SCAN_FINISHED_WITHOUT_CHANGES);
            }
        }, TIME_RELAY_SCAN);
        Log.e(TAG, "Start startSearchImmediately");
    }


    /**
     * Stop search
     * @param stopReason parameter deals with the  search counter
     */
    private void stopSearch( int stopReason ){
        // remove handler callback
        mScanHandler.removeCallbacksAndMessages(null);
        if (stopReason == SCAN_FINISHED_WITHOUT_CHANGES)
            // add to search counter
            mSearchWithoutChangeCounter++;
        Log.d(TAG, "COUNTER SEARCH WITHOUT CHANGES: " + mSearchWithoutChangeCounter );
        // stop scan
        mBLECentral.getBleScan().stopScanning();
        Log.d(TAG, "StopSearch()");

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

        // close advertiser if advertising
        mBlePeripheral.close();

        // stop ble search
        stopSearch(0);

        // create server socket
        openBluetoothServerSocketConnection();

        mBluetoothScan.startScan();
        Log.e(TAG, "Start Bluetooth scan ");
        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_SEARCH,TAG+": Start manual bluetooth search");
    }

    public void openBluetoothServerSocketConnection(){
        // Open server socket if not already connected
        if (mStatus != CONNECTED) {
            mBluetoothServer.cancel();
            mBluetoothServer = new BluetoothServer(mBluetoothAdapter, mMessenger,mRelayConnectivityManager);
            mBluetoothServer.start();
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
                    bluetoothSocket = mBluetoothServer.getBluetoothSocket();

                    // cancel server
                    mBluetoothServer.cancel();

                    stopSearch(FOUND_NEW_DEVICE);

                    // reset interval search time and counter
                    resetSearch();

                    // Start handshake
                    mHandShake = new HandShake(bluetoothSocket,mMessenger,mInitiator,
                            mRelayConnectivityManager,mDataManager,metadata);
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_HANDSHAKE,TAG+": Start hand shake with device "+
                            bluetoothSocket.getRemoteDevice().getAddress());
                    break;

                case FAILED_CONNECTING_TO_DEVICE:
                    Log.e(TAG, "FAILED_CONNECTING_TO_DEVICE");
                    //update status
                    mStatus = DISCONNECTED;
                    //make sure not scanning
                    stopSearch(0);
                    break;

                case DEVICE_FAILED_CONNECTING_ME:
                    Log.e(TAG, "DEVICE_FAILED_CONNECTING_ME");
                    //update status
                    mStatus = DISCONNECTED;
                    break;

                case SUCCEED_CONNECTING_TO_DEVICE:
                    Log.e(TAG, "SUCCEED_CONNECTING_TO_DEVICE");

                    // this device is the  Initiator
                    mInitiator = true;
                    // update status
                    mStatus = CONNECTED;

                    bluetoothSocket = mBluetoothClient.getBluetoothSocket();

                    // cancel server if working
                    mBluetoothServer.cancel();

                    stopSearch(FOUND_NEW_DEVICE);


                    // Start handshake
                    mHandShake = new HandShake(bluetoothSocket,mMessenger,mInitiator,
                            mRelayConnectivityManager,mDataManager,metadata);
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_HANDSHAKE,TAG+": Start hand shake with "+
                            bluetoothSocket.getRemoteDevice().getAddress());
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

                    mBLECentral.close();

                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CONNECTING,TAG+": Try connecting to device "+
                            address);

                    // create bluetooth device with the mac address of the founded device
                    bl  = mBluetoothAdapter.getRemoteDevice(address);
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    mBluetoothClient =  new BluetoothClient(mMessenger, bl,mRelayConnectivityManager);

                    Log.d(TAG, "Connect to device : "+ address);
                    mBluetoothClient.start();
                    break;

                case SCAN_FINISHED_WITHOUT_CHANGES:
                    Log.e(TAG, "SCAN_FINISHED_WITHOUT_CHANGES");
                    mStatus = DISCONNECTED;
                    stopSearch(SCAN_FINISHED_WITHOUT_CHANGES);
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Scan finished without changes");
                    break;

                case FINISHED_HANDSHAKE:
                    Log.e(TAG, "FINISHED_HANDSHAKE");
                    // update status
                    mStatus = DISCONNECTED;

                    // update service finish handshake
                    sendMessageToConnectivityManager(FINISHED_HANDSHAKE,null);

                    // add device mac to waiting list
                    address = msg.getData().getString("address");
                    addToLastConnectedDevicesList(address);

                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CLOSE_CONNECTION,TAG+": Finish hand shake with device : "+
                            address );

                    // close handShake connection
                    if (mHandShake != null)
                        mHandShake.closeConnection();

                    // startPeripheral
                    startPeripheral();
                    break;

                case READ_PACKET:
                    Log.e(TAG, "READ_PACKET");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_HANDSHAKE,TAG+": Read data");
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
                    // try to turn on  advertiser
                    startPeripheral();
                    break;

                case BLE_SCAN_ERROR:
                    Log.e(TAG, "BLE_SCAN_ERROR");
                    mStatus = DISCONNECTED;
                    break;

                case FOUND_MAC_ADDRESS_FROM_BLSCAN:
                    Log.e(TAG, "FOUND_MAC_ADDRESS FROM BL SCAN");
                    // update status
                    mStatus = CONNECTING;
                    address = msg.getData().getString("address");
                    Log.d(TAG, "Found MAC device : "+ address);

                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CONNECTING,TAG+": Try Connecting to device: "+address);

                    // create bluetooth device with the mac address of the founded device
                    bl  = mBluetoothAdapter.getRemoteDevice(address);
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    mBluetoothClient =  new BluetoothClient(mMessenger, bl,mRelayConnectivityManager);

                    Log.e(TAG, "Connect to device : "+ address);
                    mBluetoothClient.start();
                    break;

                case NOT_FOUND_ADDRESS_FROM_BLSCAN:
                    Log.e(TAG, "NOT_FOUND_ADDRESS_FROM_BLSCAN");
                    mStatus = DISCONNECTED;
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Found non in bluetooth scan");
                    break;

                case GET_BLUETOOTH_SERVER_READY:
                    openBluetoothServerSocketConnection();
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+": Open server bluetooth socket");
                    break;

                case FAILED_DURING_HAND_SHAKE:
                    Log.e(TAG, "FAILED_DURING_HAND_SHAKE");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_ERROR,TAG+": Failed transfer during hand shake");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CLOSE_CONNECTION,TAG+": Close connection");

                    // todo test
                    // close handShake connection
                    if (mHandShake != null)
                        mHandShake.closeConnection();

                    mBluetoothServer.cancel();
                    // close broken BluetoothClient connection
                    if (mBluetoothClient != null)
                        mBluetoothClient.cancel();
                    // update status
                    mStatus = DISCONNECTED;
                  //  intervalSearch();

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
