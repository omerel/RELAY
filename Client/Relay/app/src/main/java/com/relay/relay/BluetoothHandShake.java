package com.relay.relay;

import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

/**
 * Created by omer on 11/12/2016.
 */

public class BluetoothHandShake extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BluetoothHandShake.class.getSimpleName();

    BluetoothSocket mBluetoothSocket;
    private Messenger mMessenger;

    BluetoothHandShake(BluetoothSocket bluetoothSocket,Messenger messenger){
        this.mBluetoothSocket = bluetoothSocket;
        this.mMessenger = messenger;

    }


    public void run() {

        Log.d(TAG, "Start Thread");

        //TODO

        // Send message back to the bluetooth manager
        sendMessageToManager(FINISHED_HANDSHAKE);

        Log.d(TAG, "FINISHED_HANDSHAKE");
    }

    /**
     * Send message to the bluetooth manager
     */
    private void sendMessageToManager(int msg)  {
        try {
            mMessenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }

    // Close thread
    public void cancel() {
        try {
            mBluetoothSocket.close();
            Log.d(TAG, "Thread was closed");
        } catch (IOException e) {
            Log.e(TAG, "Problem with mBluetoothSocket.close() ");
        }
    }
}
