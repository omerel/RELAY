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
import android.net.wifi.WifiManager;
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
import com.relay.relay.MainActivity;
import com.relay.relay.R;

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
    private static final int NULL_MODE = -1;

    // General BroadcastReceiver
    private BroadcastReceiver mBroadcastReceiver;
    // BroadcastReceiver to specific mode
    private BroadcastReceiver mModeBroadcastReceiver;
    // Power manager to keep service wake when phone locked
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private IntentFilter mFilter;
    // define if mobile data is available to use
    private boolean mSwitchMobileDataUses;
    // define if wifi available to use
    private boolean mSwitchWifiConnection;
    // define if bluetooth available to use
    private boolean mSwitchBluetootConnection;
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
        mCurrentMode = NULL_MODE;
        startConnectivityByPriority();
        createGeneralBroadcastReceiver();
        setWakeLock();
        return  START_NOT_STICKY;
    }

    // setConnectivityValues from sharedPreferences
    private void setConnectivityValues(){
        mSwitchWifiConnection = sharedPreferences.getBoolean(getString(R.string.key_enable_wifi),false);
        mSwitchMobileDataUses = sharedPreferences.getBoolean(getString(R.string.key_enable_data),false);
        mSwitchBluetootConnection = sharedPreferences.getBoolean(getString(R.string.key_enable_bluetooth),false);
    }

    private void startConnectivityByPriority() {
        if (mSwitchWifiConnection && isWifiAvailable() && isWifiConnected()){
            // start Wifi mode
            //TODO initialWifiMode();
            createWifiBroadcastReceiver();
            // TODO startWIFIhMode();
            mCurrentMode = WIFI_MODE;
            Log.e(TAG,"Starting WIFI_MODE");
        }
        else if (mSwitchBluetootConnection && BluetoothAdapter.getDefaultAdapter().isEnabled()){
            // start bluetooth mode
            initialBluetoothMode();
            createBluetoothBroadcastReceiver();
            // Update bluetooth manager
            mBluetoothManager.powerConnectionDetected(isConnectedToPower());
            startBluetoothMode();
            mCurrentMode = BLUETOOTH_MODE;
            Log.e(TAG,"Starting BLUETOOTH_MODE");
        }
        else{
            // if not of them make sure broadcastreceiver is null
            mModeBroadcastReceiver = null;
            mCurrentMode = NULL_MODE;
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
        Intent updateActivity = new Intent(MainActivity.MESSAGE_RECEIVED);
        updateActivity.putExtra("relayMessage", message);
        sendBroadcast(updateActivity);
    }

    private void initialBluetoothMode(){
        this.mBluetoothManager = new BLManager(mMessenger,this);
    }

    private void startBluetoothMode(){mBluetoothManager.start();}

    private void stopBluetoothMode(){mBluetoothManager.cancel();}

    private boolean isWifiAvailable() {
        return ((WifiManager)getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
    }

    private boolean isWifiConnected() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        boolean isWifiConnection =  activeNetworkInfo != null && activeNetworkInfo.isConnected() &&
                activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        Log.e(TAG, "Wifi connected: " + isWifiConnection);
        return isWifiConnection;
    }


    /**
     *  General BroadcastReceiver
     */
    private  void createGeneralBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(KILL_SERVICE);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(CHANGE_PRIORITY_B);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case KILL_SERVICE:
                        killService();
                        break;

                    case CHANGE_PRIORITY_B:
                        if (mCurrentMode == BLUETOOTH_MODE){
                            stopBluetoothMode();
                            unregisterReceiver(mModeBroadcastReceiver);
                            Log.e(TAG, " Bluetooth switch turned off ");
                        }else if (mCurrentMode == WIFI_MODE){
                            // TODO stop wifi mode
                            unregisterReceiver(mModeBroadcastReceiver);
                            Log.e(TAG, " Wifi turned off ");
                        }else if (mCurrentMode == DATA_MODE){

                        }
                        setConnectivityValues();
                        startConnectivityByPriority();
                        break;
                    // When the device in wifi mode and bluetooth is on
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_OFF){
                            // saving state into sharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(getString(R.string.key_enable_bluetooth),false);
                            editor.commit();

                            // refresh fragment
                            Intent updateActivity = new Intent(MainActivity.FRESH_FRAGMENT);
                            sendBroadcast(updateActivity);
                        }
                        break;
                    // When the device in bluetooth and wifi switch in and connected to internet
                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        // Check if wifi connected and available
                        if (mSwitchWifiConnection && isWifiAvailable() && isWifiConnected()){

                            if (mCurrentMode == BLUETOOTH_MODE || mCurrentMode== NULL_MODE){
                                stopBluetoothMode();
                                // clear mode;
                                mCurrentMode = NULL_MODE;
                                // unregister broadcast
                                unregisterReceiver(mModeBroadcastReceiver);
                                // start startConnectivityByPriority
                                startConnectivityByPriority();
                                Log.e(TAG, " detect Wifi connected to internet ");
                            }
                        }
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }



    private void killService(){
        // stop mode
        if (mCurrentMode == BLUETOOTH_MODE)
            stopBluetoothMode();
        if (mCurrentMode == WIFI_MODE);
        //TODO stop wifi
        mCurrentMode = NULL_MODE;

        // unregisterReceiver
        if (mBroadcastReceiver != null)
            unregisterReceiver(mBroadcastReceiver);
        if (mModeBroadcastReceiver != null)
            unregisterReceiver(mModeBroadcastReceiver);

        stopSelf();
    }
    /**
     * BroadcastReceiver for Bluetooth
     */
    private  void createBluetoothBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(MANUAL_SYNC);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        // TODO enter receiver when fail while connecting

        mModeBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case MANUAL_SYNC:
                        mBluetoothManager.startManualSync();
                        break;

                    // When bluetooth state changed
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch(state) {

                            case BluetoothAdapter.STATE_OFF:
                                // saving state into sharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(getString(R.string.key_enable_bluetooth),false);
                                editor.commit();
                                // refresh fragment
                                Intent updateActivity = new Intent(MainActivity.FRESH_FRAGMENT);
                                sendBroadcast(updateActivity);
                                unregisterReceiver(mModeBroadcastReceiver);
                                // startConnectivityByPriority
                                startConnectivityByPriority();
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                // stop bluetooth mode
                                stopBluetoothMode();
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
        registerReceiver(mModeBroadcastReceiver, mFilter);
    }


    /**
     * BroadcastReceiver for Wifi
     */
    private  void createWifiBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        mModeBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action) {
                    // When incoming message received

                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        // Check if wifi connected and available
                        if (!isWifiAvailable()){

                            // saving state into sharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(getString(R.string.key_enable_wifi),false);
                            editor.commit();

                            // stop Wifi mode
                            // TODO stopWifiMode();

                            // clear mode;
                            mCurrentMode = NULL_MODE;
                            // unregister broadcast
                            unregisterReceiver(mModeBroadcastReceiver);
                            // start startConnectivityByPriority
                            startConnectivityByPriority();
                            Log.e(TAG, "Wifi turned off ");
                            // refresh fragment
                            Intent updateActivity = new Intent(MainActivity.FRESH_FRAGMENT);
                            sendBroadcast(updateActivity);

                        }else if (!isWifiConnected()){
                            // stop Wifi mode
                            // TODO stopWifiMode();

                            // clear mode;
                            mCurrentMode = NULL_MODE;
                            // unregister broadcast
                            unregisterReceiver(mModeBroadcastReceiver);
                            // start startConnectivityByPriority
                            startConnectivityByPriority();
                            Log.e(TAG, " Wifi not connected ");
                        }

//                        if (mCurrentMode == BLUETOOTH_MODE && isWifiConnected()){
//
//                            // stop Wifi mode
//                            // TODO stopWifiMode();
//
//                            // clear mode;
//                            mCurrentMode = NULL_MODE;
//                            // unregister broadcast
//                            unregisterReceiver(mModeBroadcastReceiver);
//                            // start startConnectivityByPriority
//                            startConnectivityByPriority();
//                            Log.e(TAG, " Wifi not connected ");
//                        }

                        break;
                }
            }
        };
        registerReceiver(mModeBroadcastReceiver, mFilter);
    }


    // send mode status to inboxFragment
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