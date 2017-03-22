package com.relay.relay.SubSystem;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.*;
import com.relay.relay.R;
import com.relay.relay.RelayMainActivity;

/**
 * Created by omer on 13/12/2016.
 * Service which run on the background an sense if using wifi/cellular data or bluetooth mode
 * the service starts the manager that controls the operations
 */

public class RelayConnectivityManager extends Service implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ RelayConnectivityManager.class.getSimpleName();

    public static final String KILL_SERVICE = "relay.BroadcastReceiver.KILL_SERVICE";
    public static final String MANUAL_SYNC = "relay.BroadcastReceiver.MANUAL_SYNC";
    public static final String CHANGE_PRIORITY_B = "relay.BroadcastReceiver.CHANGE_PRIORITY_B";

    private static final int BLUETOOTH_MODE = 1;
    private static final int WIFI_MODE = 2;
    private static final int DATA_MODE = 3;

    private BroadcastReceiver mBroadcastReceiver;
    // Power manager to keep service wake when phone locked
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private IntentFilter mFilter;
    // define if mobile data is available to use
    private boolean mMobileDataUses;
    // define if wifi available to use
    private boolean mWifiConnection;
    // define if bluetooth available to use
    private boolean mBluetootConnection;
    // saves current mode
    private int mCurrentMode;


    // Handler for all incoming messages from Bluetooth Manager
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private BLManager mBluetoothManager;
    private DataManager mDataManager;
    private SharedPreferences sharedPreferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        // get values from sharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setConnectivityValues();
        mCurrentMode = 0; // null
        startConnectivityByPriority();
        setWakeLock();
        return  START_NOT_STICKY;
    }

    // setConnectivityValues from sharedPreferences
    private void setConnectivityValues(){
        mWifiConnection = sharedPreferences.getBoolean(getString(R.string.key_enable_wifi),false);
        mMobileDataUses = sharedPreferences.getBoolean(getString(R.string.key_enable_data),false);
        mBluetootConnection = sharedPreferences.getBoolean(getString(R.string.key_enable_bluetooth),false);
    }

    private void startConnectivityByPriority() {
        if (mWifiConnection && isWifiAvailable()){

            // start Wifi mode
            //TODO initialWifiMode();
            createWifiBroadcastReceiver();
            // TODO startWIFIhMode();
            mCurrentMode = WIFI_MODE;
            Log.e(TAG,"Starting WIFI_MODE");
        }
        else if (mBluetootConnection && BluetoothAdapter.getDefaultAdapter().isEnabled()){

            // start bluetooth mode
            initialBluetoothMode();
            createBluetoothBroadcastReceiver();
            // Update bluetooth manager
            mBluetoothManager.powerConnectionDetected(isConnectedToPower());
            startBluetoothMode();
            mCurrentMode = BLUETOOTH_MODE;
            Log.e(TAG,"Starting BLUETOOTH_MODE");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCurrentMode == BLUETOOTH_MODE)
            stopBluetoothMode();
        if (mCurrentMode == WIFI_MODE);
        unregisterReceiver(mBroadcastReceiver);
        mWakeLock.release();
        Log.d(TAG, "Service destroyed");
    }

    private void setWakeLock() {
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        mWakeLock.acquire();
    }
    private boolean isConnectedToPower() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isConnected = status == BatteryManager.BATTERY_STATUS_CHARGING;
        return isConnected;
    }

    public void updateActivityNewMessage(String message) {

    //  BroadCast relay message to activity
        Intent updateActivity = new Intent(RelayMainActivity.MESSAGE_RECEIVED);
        updateActivity.putExtra("relayMessage", message);
        sendBroadcast(updateActivity);
    }

    private void initialBluetoothMode(){
        this.mBluetoothManager = new BLManager(mMessenger,this);
    }

    private void startBluetoothMode(){mBluetoothManager.start();}

    private void stopBluetoothMode(){mBluetoothManager.cancel();}

    private boolean isWifiAvailable() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        boolean isWifiConnection =  activeNetworkInfo != null && activeNetworkInfo.isConnected() &&
                activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        Log.e(TAG, "Wifi connected: " + isWifiConnection);
        return isWifiConnection;
    }

    /**
     * BroadcastReceiver for Bluetooth
     */
    private  void createBluetoothBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(KILL_SERVICE);
        mFilter.addAction(MANUAL_SYNC);
        mFilter.addAction(CHANGE_PRIORITY_B);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        // TODO enter receiver when fail while connecting

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case KILL_SERVICE:
                        stopSelf();
                        break;

                    case CHANGE_PRIORITY_B:
                        stopBluetoothMode();
                        unregisterReceiver(mBroadcastReceiver);
                        setConnectivityValues();
                        startConnectivityByPriority();
                        break;

                    case MANUAL_SYNC:
                        mBluetoothManager.startManualSync();
                        break;

                    // When bluetooth state changed
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch(state) {

                            case BluetoothAdapter.STATE_OFF:
                                // startConnectivityByPriority
                                startConnectivityByPriority();
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                // stop bluetooth mode
                                stopBluetoothMode();
                                unregisterReceiver(mBroadcastReceiver);
                                break;
                            case BluetoothAdapter.STATE_ON:
                                break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                break;
                        }
                        break;
                    case Intent.ACTION_POWER_DISCONNECTED:
                        // do the next case
                    case Intent.ACTION_POWER_CONNECTED:
                        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
                        // update bluetooth manager
                        mBluetoothManager.powerConnectionDetected(usbCharge || acCharge);
                        // show detection
                        if (usbCharge || acCharge)
                            Log.e(TAG,"Relay detects power connection");
                        else
                            Log.e(TAG,"Relay detects power disconnection");
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }


    /**
     * BroadcastReceiver for Wifi
     */
    private  void createWifiBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(KILL_SERVICE);
        mFilter.addAction(CHANGE_PRIORITY_B);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // TODO add KILL service like bluetooth receiver or put it in handler

                String action = intent.getAction();

                switch (action) {

                    // When incoming message received
                    case KILL_SERVICE:
                        stopSelf();
                        break;
                    case CHANGE_PRIORITY_B:
                        stopBluetoothMode();
                        unregisterReceiver(mBroadcastReceiver);
                        setConnectivityValues();
                        startConnectivityByPriority();
                        break;
                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        // Check if wifi connected and available
                        if (!isWifiAvailable()){
                            // stop Wifi mode
                            // TODO stopWifiMode();
                            unregisterReceiver(mBroadcastReceiver);
                            // start startConnectivityByPriority
                            startConnectivityByPriority();
                            Log.e(TAG, " Wifi turn off ");
                        }
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }

    /**
     * Handler of incoming messages from Bluetooth Manager
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case NEW_RELAY_MESSAGE:
                    String relayMessage = msg.getData().getString("relayMessage");
                    updateActivityNewMessage(relayMessage);
                    Log.e(TAG, "update activity with new message");
                    break;

                case BLE_ERROR:
                    mBluetoothManager.cancel();
                    // make sure bluetooth enable
                    BluetoothAdapter.getDefaultAdapter().disable();
                    unregisterReceiver(mBroadcastReceiver);
                    Log.e(TAG,"Received BLE error. stop bluetooth mode");
                    setConnectivityValues();
                    startConnectivityByPriority();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }
}
