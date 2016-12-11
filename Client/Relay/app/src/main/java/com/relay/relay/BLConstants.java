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
    int FAILED_ADVERTISING = 30;

    // Constants used in BLEAdvertising
    int TIMEOUT_ADVERTISING = 5;

    // Message from BLEScan
    int FOUND_NEW_DEVICE = 40;

    // Constants used in BLEScan
    int SCAN_PERIOD = 5000;
}
