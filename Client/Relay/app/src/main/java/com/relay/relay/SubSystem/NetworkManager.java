package com.relay.relay.SubSystem;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.Network.NetworkConstants;
import com.relay.relay.Network.SyncWithServer;
import com.relay.relay.viewsAndViewAdapters.StatusBar;

/**
 * Created by omer on 05/06/2017.
 */

public class NetworkManager extends Thread implements NetworkConstants {

    private final String TAG = "RELAY_DEBUG: "+ NetworkManager.class.getSimpleName();

    // Messenger from RelayConnectivityManager
    private Messenger mConnectivityMessenger;
    private Handler mIntervalHandler;
    private Runnable mIntervalRunnable;
   // private Server mServer;
    private RelayConnectivityManager mRelayConnectivityManager;
    private DataManager mDataManager;
    private SyncWithServer mSyncWithServer;
    // Handler for all incoming messages from SyncWithServer
    private final Messenger mMessenger = new Messenger(new IncomingHandler());


    public NetworkManager(Messenger connectivityMessenger, RelayConnectivityManager relayConnectivityManager) {

        this.mRelayConnectivityManager = relayConnectivityManager;
        this.mConnectivityMessenger = connectivityMessenger;
        this.mIntervalHandler = new Handler();
        this.mDataManager = new DataManager(relayConnectivityManager);
        this.mIntervalRunnable = new Runnable() {
            @Override
            public void run() {
                // if syncing with server don't interrupt
                if (mSyncWithServer.getStatus() == NOT_SYNC_WITH_SERVER)
                    mSyncWithServer = new SyncWithServer(mMessenger,mRelayConnectivityManager,mDataManager);
                intervalSync();
            }
        };


    }

    // Start thread
    @Override
    public void run() {
        Log.e(TAG, "Start thread");
        mDataManager.closeAllDataBase();
        mDataManager.openAllDataBase();
        mSyncWithServer = new SyncWithServer(mMessenger,mRelayConnectivityManager,mDataManager);
        intervalSync();
        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_WIFI_ON,TAG+": Wifi mode turned on");
    }


    // Close thread
    public void cancel() {
        mIntervalHandler.removeCallbacks(mIntervalRunnable);
    }

    private void intervalSync(){
        mIntervalHandler.postDelayed(mIntervalRunnable, INTERVAL_SYNC_WITH_SERVER);
        Log.e(TAG, "Start intervalSearch()");
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

    /**
     * Handler of incoming messages from one of the SyncWithSever clases
     */
    class IncomingHandler extends Handler {
        String message;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case NEW_RELAY_MESSAGE:
                    Log.e(TAG, "NEW_RELAY_MESSAGE");
                    // When the message is for this device
                    Log.d(TAG, "Received  new relay message from Handshake");
                    String relayMessage = msg.getData().getString("relayMessage");
                    sendMessageToConnectivityManager(NEW_RELAY_MESSAGE,relayMessage);
                    break;
                case  START_SYNC:
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CONNECTING,TAG+": Start sync with server");
                    break;
                case  FINISH_SYNC:
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CLOSE_CONNECTION,TAG+": Finish sync with server");
                    break;
                case ERROR_WHILE_SYNC:
                    message = msg.getData().getString("message");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_ERROR,TAG+": "+message);
                    break;
                case PROGRESS:
                    message = msg.getData().getString("message");
                    mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_WIFI_ON,TAG+": "+message);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
