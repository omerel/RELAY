package com.relay.relay;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.BluetoothConnected;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by omer on 12/12/2016.
 *
 */

public class HandShake implements BLConstants {


    private final String TAG = "RELAY_DEBUG: "+ HandShake.class.getSimpleName();
    private Messenger mMessenger;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothConnected mBluetoothConnected;



    private String mDeviceUUID;
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
        // send th Map as json
        sendPacket();
    }


    private void sendPacket(){

        String jsonString = new Gson().toJson(MainActivity.db);
        mBluetoothConnected.writePacket(jsonString);
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
        Map<String, Object[]> tempMap = convertPacketToRelayMessage(packet);
        boolean newMsg = false;
        String msg;
        Object[] obj;
        Date d1;
        Date d2;
        String id = "A";

        // for each Id check if there is new content and if its for me
        for (int i = 0 ; i < 4; i++) {

            if (i == 0)
                id = "A";
            if (i == 1)
                id = "B";
            if (i == 2)
                id = "C";
            if (i == 3)
                id = "D";

            obj = tempMap.get(id);
            d1 = convertStringToDate( (String) obj[0] );
            d2 = convertStringToDate(  (String) MainActivity.db.get(id)[0] );

            // if the device has newer message
            if (d1.compareTo(d2) > 0) {
                MainActivity.db.put(id, obj);
                // if id is me?
                Log.e(TAG,mDeviceUUID+ " is "+ id + " ?");
                if (id.equals(mDeviceUUID)) {
                    newMsg = true;
                }
            }
        }

        if (newMsg){
            msg = (String) MainActivity.db.get(mDeviceUUID)[1];
            msg = msg +"\nSent: "+ (String) MainActivity.db.get(mDeviceUUID)[0];
            sendRelayMessageToManager(NEW_RELAY_MESSAGE,msg);
            Log.e(TAG, "Sending new relay message to manager");
        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishHandshake();
                }
            }, 2000);
        }
    }


    // convert string to date
    private Date convertStringToDate(String date){

        String dateString = date;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    /**
     * Convert packet to relay message
     */
    // TODO change string to relay message
    private Map<String, Object[]> convertPacketToRelayMessage(String packet) {

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object[]>>(){}.getType();
        return gson.fromJson(packet, type);

    }


    public void closeConnection(){
        mBluetoothConnected.cancel();
    }
}
