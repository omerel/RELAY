package com.relay.relay.Bluetooth;

import android.os.ParcelUuid;

import java.util.UUID;

/**
 * Created by omer on 10/12/2016.
 * This interface fills all the constants that are used in the bluetooth connectivity
 */

public interface BLConstants {

    // inner interface
    int SECOND = 1000;
    int MINUTE = 1000*60;

    // Unique UUID for this application
    UUID APP_UUID = UUID.fromString("ca87c0d0-afac-11de-8a39-0800200c9a66");

    ParcelUuid SERVICE_UUID = ParcelUuid.fromString("ca87c0d0-afac-11de-8a39-0800200c9a66");

    UUID RELAY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    UUID MAC_ADDRESS_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    // Message from BluetoothServer
    int DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER = 10;


    // Message from BluetoothClient
    int FAILED_CONNECTING_TO_DEVICE = 20;
    int SUCCEED_CONNECTING_TO_DEVICE = 21;

    // Message from BLEAdvertising

    // Constants used in BLEAdvertising
    int TIMEOUT_ADVERTISING_IN_MINUTES = 5;

    // Message from BLEScan
    int FOUND_NEW_DEVICE = 40;

    // Constants used in BlManager
    int TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS = 7*SECOND;
    int MAX_TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS = 32*MINUTE;
    int TIME_RELAY_SCAN_IN_SECONDS = 5*SECOND;
    int TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_SECOND = 1*MINUTE;
    int MAX_SEARCHS_WITHOUT_CHANGE_BEFORE_CHANGES = 20;
    int SCAN_FINISHED_WITHOUT_CHANGES = 50;

    // Message from BluetoothConnected
    int READ_PACKET = 60;

    // Message from HandShake
    int FINISHED_HANDSHAKE = 70;
    int  NEW_RELAY_MESSAGE = 71;

    // Message from BLECentral
    int FOUND_MAC_ADDRESS = 80;

    int TEST = 1231;



}
