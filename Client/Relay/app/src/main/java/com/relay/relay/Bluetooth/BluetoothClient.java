package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.viewsAndViewAdapters.StatusBar;

import java.io.IOException;

/**
 * Created by omer on 10/12/2016.
 * Bluetooth Client is a thread that connect to other device in bluetooth radio
 */

public class BluetoothClient extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+BluetoothClient.class.getSimpleName();

    private final BluetoothDevice mBluetoothDevice;
    private Messenger mMessenger;
    private final BluetoothSocket mBluetoothSocket;
    private  RelayConnectivityManager mRelayConnectivityManager;

    /**
     * BluetoothClient constructor
     * @param messenger to bluetooth manager
     * @param bluetoothDevice the device that act as a service
     */
    public BluetoothClient(Messenger messenger, BluetoothDevice bluetoothDevice, RelayConnectivityManager relayConnectivityManager) {

        // Use messenger to update bluetooth manger
        this.mMessenger = messenger;
        this.mBluetoothDevice = bluetoothDevice;
        this.mRelayConnectivityManager = relayConnectivityManager;

        // Use a temporary object that is later assigned to mBluetoothSocket,
        // because mBluetoothSocket is final
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            //tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Problem with creating createRfcommSocketToServiceRecord");
            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_ERROR,TAG+
                    ": Failed create Rfcomm Socket To Service Record, "+e.getMessage());
        }
        mBluetoothSocket = tmp;
        Log.d(TAG, "Class created");
    }
    // Start thread
    public void run() {
        Log.d(TAG, "Start Thread");
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mBluetoothSocket.connect();
        } catch (IOException connectException) {
            Log.e(TAG, "Problem with mBluetoothSocket.connect() "+connectException.getMessage());
            // Unable to connect; close the socket and get out
            cancel();
            // Send message back to the bluetooth manager
            sendMessageToManager(FAILED_CONNECTING_TO_DEVICE);
            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_NO_CHANGE,TAG+
                    ": Failed connecting device socket, "+connectException.getMessage());
            return;
        }
        // Send message back to the bluetooth manager
        sendMessageToManager(SUCCEED_CONNECTING_TO_DEVICE);
        mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CONNECTING,TAG+": Succeed Connecting device"+
                mBluetoothSocket.getRemoteDevice().getAddress());
        Log.d(TAG, "SUCCEED_CONNECTING_TO_DEVICE");
    }

    // Close thread
    public void cancel() {
        try {
            mBluetoothSocket.close();
            Log.d(TAG, "Thread was closed");
            mRelayConnectivityManager.broadCastFlag(StatusBar.FLAG_CLOSE_CONNECTION,TAG+": Close bluetooth socket");
        } catch (IOException e) {
            Log.e(TAG, "Error with mBluetoothSocket.close() ");
        }
    }

    /**
     * BluetoothSocket getter
     * @return BluetoothSocket
     */
    public BluetoothSocket getBluetoothSocket(){
        return mBluetoothSocket;
    }


    /**
     * Send message to the bluetooth manager
     * @param msg
     */
    private void sendMessageToManager(int msg)  {
        try {
            mMessenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }

}
