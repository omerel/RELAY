package com.relay.relay;

import java.util.UUID;

/**
 * Created by omer on 10/12/2016.
 * This interface fills all the constants that are used in the bluetooth connectivity
 */

public interface BLConstants {

    // Unique UUID for this application
    UUID APP_UUID = UUID.fromString("ca87g0d0-afac-11de-8a39-0800200c9a66");


    // Message from BluetoothServer
    int DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER = 10;


    // Message from BluetoothConnect
    int FAILED_CONNECTING_TO_DEVICE = 20;
    int SUCCEED_CONNECTING_TO_DEVICE = 21;

    // Message from BLEAdvertising

    // Constants used in BLEAdvertising
    int TIMEOUT_ADVERTISING_IN_MINUTES = 5;

    // Message from BLEScan
    int FOUND_NEW_DEVICE = 40;

    // Constants used in BluetoothManager
    int TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS = 30*1000;
    int MAX_TIME_RELAY_SEARCH_INTERVAL_IN_SECONDS = 1920*1000;
    int TIME_RELAY_SCAN_IN_SECONDS = 5000;
    int TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_IN_MINUTES = 30;
    int MAX_SEARCHS_WITHOUT_CHANGE = 5;
    int SCAN_FINISHED_WITHOUT_CHANGES = 50;

    // Message from BluetoothHandShake
    int FINISHED_HANDSHAKE = 60;


}
