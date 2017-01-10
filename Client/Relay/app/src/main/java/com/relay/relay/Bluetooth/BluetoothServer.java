package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;

/**
 * Created by omer on 10/12/2016.
 * BluetoothServer , a thread which waits for other device to ask for connection. when it happen, BluetoothServer
 * creates BluetoothSocket.
 */

public class BluetoothServer extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+BluetoothServer.class.getSimpleName();

    private final BluetoothServerSocket mmServerSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private Messenger mMessenger;
    private BluetoothDevice mConnectedDevice;
    private BluetoothSocket mBluetoothSocket;


    /**
     * BluetoothServer constructor
     * @param bluetoothAdapter to create socket
     * @param messenger to bluetooth manager
     */
    public BluetoothServer(BluetoothAdapter bluetoothAdapter, Messenger messenger) {

        // Use messenger to update bluetooth manger
        this.mMessenger = messenger;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBluetoothSocket = null;
        this.mConnectedDevice = null;

        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;

        try {
            // APP_UUID is the app's UUID string, also used by the client code
            //tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("RELAY", APP_UUID);
            // TODO check if the listening should be insecure
             tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("RELAY", APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Problem with creating listenUsingRfcommWithServiceRecord");
        }
        mmServerSocket = tmp;
        Log.d(TAG, "Class created");
    }

    // Start thread
    public void run() {
        Log.d(TAG, "Start Thread");

        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                Log.d(TAG, "Waiting to mmServerSocket.accept() ");
                socket = mmServerSocket.accept();
                Log.d(TAG, "SUCCESSFULLY CONNECTED");
            } catch (IOException e) {
                Log.e(TAG, "Problem with mmServerSocket.accept() ");
                break;
            }catch (NullPointerException e){
                Log.e(TAG, "Problem with mmServerSocket.accept() [null] ");
                break;
            }
            // If a connection was accepted
            if (socket != null) {

                // update socket
                mBluetoothSocket = socket;
                // get connceted device
                mConnectedDevice = mBluetoothSocket.getRemoteDevice();
                // Send message back to the bluetooth manager
                sendMessageToManager(DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER);
                Log.d(TAG, "DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER");
            }

        }
    }

    /**
     * Close thread
     */
    public void cancel() {
        try {
            if (mmServerSocket != null)
                mmServerSocket.close();
                Log.d(TAG, "Thread was closed");
        } catch (IOException e) {
            Log.e(TAG, "Problem with mmServerSocket.close() ");
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
     * @param msg message type
     */
    private void sendMessageToManager(int msg)  {
        try {
            mMessenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }
}
