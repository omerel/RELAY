package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by omer on 11/12/2016.
 * BluetoothConnected controls the transmit in and out of data between to devices
 */

public class BluetoothConnected extends Thread implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ BluetoothConnected.class.getSimpleName();

    private final BluetoothSocket mBluetoothSocket;
    private Messenger mMessenger;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;


    public BluetoothConnected(BluetoothSocket bluetoothSocket, Messenger messenger){
        this.mBluetoothSocket = bluetoothSocket;
        this.mMessenger = messenger;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = bluetoothSocket.getInputStream();
            tmpOut = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error in creating InputStream and OutputStream");
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        Log.d(TAG, "Class created");
    }

    public void run() {
        Log.d(TAG, "Start Thread");
        int bytes;
        byte[] buffer = new byte[1000];
       // byte[] sizePacket = new byte[20]; // the maximum size of it

        // Keep listening to the InputStream while connected
        while (mBluetoothSocket.isConnected()) {
            try {

                // Read from the InputStream how many bytes in the the packetSize
                bytes = mmInStream.read(buffer);
                Log.d(TAG, "receiving Packet");

                sendPacketToBluetoothManager(READ_PACKET,buffer);
                Log.d(TAG, "Packet received , size: "+buffer+ "bytes");
                Log.e(TAG, " THE Packet received is :"+new String(buffer, 0, bytes));


//                // Read from the InputStream how many bytes in the the packetSize
//                bytes = mmInStream.read(sizePacket);
//                Log.d(TAG, "receiving Packet");
//                // Get the packetsize in string
//                String packetSizeString = (new String(sizePacket, 0, bytes));
//
//                // convert packet to int
//                int packetSize =  Integer.valueOf(packetSizeString);
//
//                byte[] packetBuffer = new byte[packetSize+1024];
//                // use byteCounter as a counter
//                int counter = 0 ;
//                int bufferSize  = 1024;
//                while(packetSize != counter){
//                    counter+= mmInStream.read(packetBuffer,counter,bufferSize);
//                }
//                // when finish read all packet. broadcast the packet
//                sendPacketToBluetoothManager(READ_PACKET,packetBuffer);
//                Log.d(TAG, "Packet received , size: "+packetSize+ "bytes");
//                Log.e(TAG, " THE Packet received is :"+packetBuffer.toString());

            } catch (IOException e) {
                Log.e(TAG, "Error with readPacket ");
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void writePacket(byte[] buffer) {
        try {

            // get how many bytes
//            int bytes = buffer.length;
//
//            if (bytes > 100){
//                // send header (size of the packet)
//                mmOutStream.write((String.valueOf(bytes)).getBytes());
//                mmOutStream.flush();
//            }

            // send the original packet
            mmOutStream.write(buffer);
            Log.d(TAG, "Packet delivered");

        } catch (IOException e) {
            Log.e(TAG, "Error with writePacket ");
        }
    }


    /**
     * Send packet to the bluetooth manager
     */
    private void sendPacketToBluetoothManager(int m,byte[] packet)  {

        // Send packet as a byte array
        Bundle bundle = new Bundle();
        bundle.putByteArray("packet", packet);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);
        Log.e(TAG, "Packet sent to BlManager ");
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendPacketToBluetoothManager ");
        }
    }


    // Close thread
    public void cancel() {
        try {
            mBluetoothSocket.close();
            Log.d(TAG, "Thread was closed");
        } catch (IOException e) {
            Log.e(TAG, "Error with mBluetoothSocket.close() ");
        }
    }
}
