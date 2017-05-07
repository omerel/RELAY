package com.relay.relay.Bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.Util.Gzip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

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
        int bytes = 0;
        int counter;
        int bufferSize  = 1000;
        byte[] initialBuffer = new byte[1000];

        // Keep listening to the InputStream while connected
        while (mBluetoothSocket.isConnected()) {
            try {
                // read the first sent data of packet that includes the header
                bytes = mmInStream.read(initialBuffer);
                Log.e(TAG, "receiving  first package of the Packet that include the header. size: "+bytes);

                // the part of the size of packet
                byte[] packetSizeInByte = new byte[20];
                System.arraycopy(initialBuffer,0,packetSizeInByte,0,packetSizeInByte.length);
                // the part of the first delivery of packet
                byte[] firstDeliveryPacket = new byte[bytes - 20];
                System.arraycopy(initialBuffer,20,firstDeliveryPacket,0,firstDeliveryPacket.length);


                // convert bytes to integer
                int packetSize = java.nio.ByteBuffer.wrap(packetSizeInByte).getInt();
                Log.e(TAG,"packet size is "+packetSize);

                // create packet with the original size + buffer
                byte[] packetBuffer = new byte[packetSize + bufferSize];

                // if the first delivery is all of the packet
                if (bytes >= packetSize){
                    byte[] finalPacket = new byte[packetSize];
                    System.arraycopy(firstDeliveryPacket,0,finalPacket,0,finalPacket.length);
                    String stringPacket = Gzip.decompress(finalPacket);
                    sendPacketToBluetoothManager(READ_PACKET, stringPacket);
                }
                else {
                    counter = bytes-packetSizeInByte.length;
                    int sum;
                    byte[] buffer = new byte[1000];
                    byte[] finalPacket = new byte[packetSize];
                    System.arraycopy(firstDeliveryPacket,0,finalPacket,0,firstDeliveryPacket.length);
                    while (packetSize > counter ) {
                        sum = mmInStream.read(buffer,0,buffer.length);
                        System.arraycopy(buffer,0,finalPacket,counter,sum);
                        counter += sum;
                    }


                    String stringPacket = Gzip.decompress(finalPacket);
                    sendPacketToBluetoothManager(READ_PACKET, stringPacket);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error - got out from read packet loop\n"+e.getMessage());
                break;
            }
            catch (Exception e) {
                Log.e(TAG, "Error - Problem with reading data\n"+e.getMessage());
                cancel();
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

            // zip packetString
            byte[] zip = Gzip.compress(packetString);
            int sumBytes = zip.length;

            // allocate 20 bytes for packet size
            ByteBuffer byteBuffer = ByteBuffer.allocate(20);
            byteBuffer.putInt(sumBytes);
            byte[] packetSize = byteBuffer.array();
            Log.e(TAG,"packet size is sent "+sumBytes);

            byte[] packet = new byte[packetSize.length + zip.length];

            System.arraycopy(packetSize,0,packet,0,packetSize.length);
            System.arraycopy(zip,0,packet,packetSize.length,zip.length);

            mmOutStream.write(packet);
        }
        catch (IOException e) {
            Log.e(TAG, "Error with writePacket ");
            cancel();
            sendPacketMessageBluetoothManager(FAILED_DURING_HAND_SHAKE,null);
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
