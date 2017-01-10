package com.relay.relay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.BluetoothConnected;

/**
 * Created by omer on 12/12/2016.
 *
 */

public class HandShake implements BLConstants {


    private final String TAG = "RELAY_DEBUG: "+ HandShake.class.getSimpleName();
    private Messenger mMessenger;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothConnected mBluetoothConnected;
    private final String mDeviceUUID;
    private boolean mInitiator;


    // TEST
    private boolean testReceieved = false;
    //

    public HandShake(String deviceUUID, BluetoothSocket bluetoothSocket, Messenger messenger
            ,boolean initiator){

        this.mDeviceUUID = deviceUUID;
        this.mMessenger = messenger;
        this.mBluetoothSocket = bluetoothSocket;
        this.mInitiator = initiator;
        this.mBluetoothConnected = new BluetoothConnected(mBluetoothSocket,messenger);
        this.mBluetoothConnected.start();


        startHandshake();
    }

    /**
     * Start handshake process
     */
    private void startHandshake() {

        String testout = BluetoothAdapter.getDefaultAdapter().getAddress();
        mBluetoothConnected.writePacket(testout);

    }

    /**
     * Finish handshake process
     */
    private void finishHandshake() {
        // Send message back to the bluetooth manager
        sendMessageToManager(FINISHED_HANDSHAKE,mBluetoothSocket.getRemoteDevice().getAddress());
        Log.d(TAG, "FINISHED_HANDSHAKE");
    }


    /**
     * Send message to the bluetoothManager class
     */
    private void sendMessageToManager(int m,String address)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }


    /**
     * Send  relay message to the bluetoothManager class
     */
    private void sendRelayMessageToManager(int m,String relayMessage)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("relayMessage", relayMessage);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendRelayMessageToManager ");
        }

        //  TODO DELETE
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finishHandshake();
            }
        }, 2000);

    }

    /**
     * Getter of incoming messages from one of the BluetoothConnected
     */
    public void getPacket(String packet){

        // convert the packet to relay message
         String relayMessage = convertPacketToRelayMessage(packet);

        // TODO change string to relay message
        // if the message meant for the user update bluetooth manager
         if (isMessageForMe(relayMessage)){
             sendRelayMessageToManager(NEW_RELAY_MESSAGE,relayMessage);
             Log.e(TAG, "Sending new relay message to manager");
         }

    }


    /**
     * Convert packet to relay message
     */
    // TODO change string to relay message
    private String convertPacketToRelayMessage(String packet) {

        //String relayMessage  = new String(packet, 0, packet.length);
        testReceieved = true;
        return packet;
    }

    /**
     * Convert packet to relay message
     */
    private boolean isMessageForMe(String relayMessage) {
        testReceieved = true;
        return true;
    }


    public void closeConnection(){
        mBluetoothConnected.cancel();
    }
}
