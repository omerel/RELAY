package com.relay.relay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

/**
 * Created by omer on 10/12/2016.
 * BluetoothConnect , a thread which connect to other device.
 */

public class BluetoothConnect extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+BluetoothConnect.class.getSimpleName();
    private final BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private Messenger mMessenger;
    private final BluetoothSocket mBluetoothSocket;


    public BluetoothConnect(BluetoothAdapter bluetoothAdapter, Messenger messenger,
                            BluetoothDevice bluetoothDevice) {


        // Use messanger to update bluetooth manger
        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBluetoothDevice = bluetoothDevice;

        // Use a temporary object that is later assigned to mBluetoothSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Problem with creating createRfcommSocketToServiceRecord");
        }

        mBluetoothSocket = tmp;

        Log.d(TAG, "Class created");
    }

    public void run() {

        Log.d(TAG, "Start Thread");
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mBluetoothSocket.connect();
        } catch (IOException connectException) {
            Log.e(TAG, "Problem with mBluetoothSocket.connect() ");
            // Unable to connect; close the socket and get out

            // close the socket
            cancel();

            // Send message back to the bluetooth manager
            sendMessageToManager(FAILED_CONNECTING_TO_DEVICE);

            return;
        }

        // Send message back to the bluetooth manager
        sendMessageToManager(SUCCEED_CONNECTING_TO_DEVICE);

        Log.d(TAG, "SUCCEED_CONNECTING_TO_DEVICE");
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

}
