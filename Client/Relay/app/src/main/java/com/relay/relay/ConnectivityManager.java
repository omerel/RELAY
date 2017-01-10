package com.relay.relay;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;
import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.*;


/**
 * Created by omer on 13/12/2016.
 * Service which run on the background an sense if using wifi/cellular data or bluetooth mode
 * the service starts the manager that controls the operations
 */

public class ConnectivityManager extends Service implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ ConnectivityManager.class.getSimpleName();

    public static final String KILL_SERVICE = "relay.BroadcastReceiver.KILL_SERVICE";
    public static final String MANUAL_SYNC = "relay.BroadcastReceiver.MANUAL_SYNC";
    private BroadcastReceiver mBroadcastReceiver;
    // Power manager to keep service wake when phone locked
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private IntentFilter mFilter;
    // define if mobile data is available to use
    private boolean mMobileDataUses;
    // define if to start again bluetooth connection
    private boolean mRestart;
    // define if there is an internet connection
    private boolean mWifiConnection;
    // Handler for all incoming messages from Bluetooth Manager
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private BLManager mBluetoothManager;
    // TODO change device uuid TEMP
    private  final String mDeviceUUID = "0002280F-0000-1000-8000-00805f9234f1";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        // set off data mobile uses
        mMobileDataUses = false;
        mWifiConnection = false;
        startConnectivityByPriority();
        setWakeLock();
        mRestart = false;
        return  START_NOT_STICKY;
    }

    private void startConnectivityByPriority() {

        if (mWifiConnection){
            // start wifi connection
        }
        else{
            if(mMobileDataUses){
                // start connection using mobile data
            }
            else{

                // make sure bluetooth enable
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));

                // start bluetooth mode
                initialBluetoothMode();
                createBroadcastReceiver();
                checkPowerConnection();
                startBluetoothMode();

            }
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
        stopBluetoothMode();
        unregisterReceiver(this.mBroadcastReceiver);
        mWakeLock.release();
        Log.d(TAG, "Service destroyed");
    }


    private void setWakeLock() {
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        mWakeLock.acquire();
    }

    private void checkPowerConnection() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        // Update bluetooth manager
        mBluetoothManager.powerConnectionDetected(isCharging);
    }

    public void updateActivityNewMessage(String message) {

    //  BroadCast relay message to activity
        Intent updateActivity = new Intent(MainActivity.MESSAGE_RECEIVED);
        updateActivity.putExtra("relayMessage", message);
        sendBroadcast(updateActivity);
    }

    private void initialBluetoothMode(){
        this.mBluetoothManager = new BLManager(mDeviceUUID,mMessenger,this);
    }

    private void startBluetoothMode(){
        mBluetoothManager.start();
    }

    private void stopBluetoothMode(){
        mBluetoothManager.cancel();
    }


    private void enableBluetooth(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bluetooth");
        builder.setMessage("to ensure RELAY working, please open the bluetooth");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Intent blEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                blEnable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(blEnable);
            }
        });
        builder.show();
    }

    /**
     * BroadcastReceiver of incoming messages from all activities
     */
    private  void createBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(KILL_SERVICE);
        mFilter.addAction(MANUAL_SYNC);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case KILL_SERVICE:
                        stopSelf();
                        break;

                    case MANUAL_SYNC:
                        mBluetoothManager.startManualSync();
                        break;

                    // When bluetooth state changed
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch(state) {
                            case BluetoothAdapter.STATE_OFF:
                                if (mRestart){
                                    Intent blEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    blEnable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(blEnable);
                                }
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                break;
                            case BluetoothAdapter.STATE_ON:
                                if(mRestart){
                                    initialBluetoothMode();
                                    startBluetoothMode();
                                    mRestart = false;
                                }
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
                        if (usbCharge || acCharge)
                            Toast.makeText(getApplicationContext(), "Relay detects power connection",
                                    Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getApplicationContext(), "Relay detects power disconnection",
                                    Toast.LENGTH_SHORT).show();
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
                    // TODO change string to relay message
                    String relayMessage = msg.getData().getString("relayMessage");
                    updateActivityNewMessage(relayMessage);
                    Log.e(TAG, "update activity with new message");
                    break;

                case BLE_ERROR:
                    mBluetoothManager.cancel();
                    BluetoothAdapter.getDefaultAdapter().disable();
                    mRestart = true;
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }



}
