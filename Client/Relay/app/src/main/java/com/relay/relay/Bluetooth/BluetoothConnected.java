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

    /**
     * BluetoothConnected constructor
     * @param bluetoothSocket for transmit data between devices
     * @param messenger to bluetooth manager
     */
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

    // Start thread
    public void run() {
        Log.d(TAG, "Start Thread");
        int bytes;
        int counter;
        int bufferSize  = 1024;
        byte[] initialBuffer = new byte[1000];

        // Keep listening to the InputStream while connected
        while (mBluetoothSocket.isConnected()) {
            try {

                // read the first sent data of packet that includes the header
                bytes = mmInStream.read(initialBuffer);
                Log.d(TAG, "receiving  first package of the Packet that include the header");

                // convert to string
                String firstDeliver = new String(initialBuffer,0,bytes);

                // take the header and convert to integer
                int packetSize = Integer.valueOf((firstDeliver.split(DELIMITER))[0]);

                // create packet with the original size + buffer
                byte[] packetBuffer = new byte[packetSize + bufferSize];

                // if the first delivery is all of the packet
                if (bytes >= packetSize){
                    sendPacketToBluetoothManager(READ_PACKET, (firstDeliver.split(DELIMITER))[1]);
                }
                else {
                    counter = bytes;
                    while (packetSize >= counter) {
                        counter += mmInStream.read(packetBuffer, counter, bufferSize);
                    }
                    // convert  packet buffer to string
                    String tempString = new String(packetBuffer);
                    // add the header to the packet buffer
                    String stringPacketWithHeader = firstDeliver + tempString.substring(firstDeliver.length());
                    // the sent packet without header
                    String stringPacket = (stringPacketWithHeader.split(DELIMITER))[1];
                    stringPacket = stringPacket.substring(0,packetSize);
                    sendPacketToBluetoothManager(READ_PACKET, stringPacket);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error - got out from read packet loop ");
                break;
            }
            catch (Exception e) {
                // TODO testing it
                Log.e(TAG, "Error - Problem with reading data");
                sendPacketMessageBluetoothManager(FAILED_DURING_HAND_SHAKE,null);
            }
        }
    }

    /**
     * Write to the connected OutStream.
     * @param packetString
     */
    public void writePacket(String packetString) {
        try {
            // get how many bytes
            int sumBytes = packetString.getBytes().length;
            //
            String sumBytesInString = String.valueOf(sumBytes);
            //
            String packetStringWithHeader  = sumBytesInString +DELIMITER+ packetString;

            mmOutStream.write(packetStringWithHeader.getBytes());
            Log.d(TAG, "Packet delivered");

        }
        catch (IOException e) {
            Log.e(TAG, "Error with writePacket ");
        }
    }


    /**
     * Send packet to the bluetooth manager
     * @param m message to bluetooth manager
     * @param packet received packet
     */
    private void sendPacketToBluetoothManager(int m,String packet)  {

        // Send packet as a byte array
        Bundle bundle = new Bundle();
        bundle.putString("packet", packet);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);
        Log.d(TAG, "Packet sent to BLManager ");
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendPacketToBluetoothManager ");
        }
    }


    private void sendPacketMessageBluetoothManager(int m,String message)  {

        // Send packet as a byte array
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);
        Log.d(TAG, "message sent to BLManager ");
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error with sendMessageToBluetoothManager ");
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
