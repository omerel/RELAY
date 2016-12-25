package com.relay.relay;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.format.Time;
import android.util.Log;

import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.BlManager;

import static java.lang.System.exit;


/**
 * Created by omer on 13/12/2016.
 * Service which run on the background an sense if using wifi/cellular data or bluetooth mode
 * the service starts the manager that controls the operations
 */

public class ConnectivityManager extends Service implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ ConnectivityManager.class.getSimpleName();

    public static final String KILL_SERVICE = "relay.BroadcastReceiver.KILL_SERVICE";
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    private boolean mMobileDataUses;

    // Handler for all incoming messages from Bluetooth Manager
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private BlManager mBluetoothManager;
    // TODO change device uuid
    private  final String mDeviceUUID = Time.getCurrentTimezone().toString();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service started");

        // set off data mobile uses
        this.mMobileDataUses = false;
        this.mBluetoothManager = new BlManager(mDeviceUUID,mMessenger,this);
        mBluetoothManager.start();

        createBroadcastReceiver();
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
        mBluetoothManager.cancel();
        unregisterReceiver(this.mBroadcastReceiver);
        Log.d(TAG, "Service destroyed");
    }


public void updateActivityNewMessage(String message) {

    //  BroadCast relay message to activity
        Intent updateActivity = new Intent(MainActivity.MESSAGE_RECEIVED);
        updateActivity.putExtra("relayMessage", message);
        sendBroadcast(updateActivity);
    }

    /**
     * BroadcastReceiver of incoming messages from all activities
     */
    private  void createBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(KILL_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case KILL_SERVICE:
                       // mBluetoothManager.cancel();
                        //stopSelf();
                        // TODO  the commands above doesnt working from some reason????
                        exit(0);
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

                default:
                    super.handleMessage(msg);
            }
        }
    }
}
