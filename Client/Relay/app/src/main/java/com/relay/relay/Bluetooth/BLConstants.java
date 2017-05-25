package com.relay.relay.Bluetooth;

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

    UUID RELAY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    UUID MAC_ADDRESS_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    // Delimiter in bluetoothConnected
    String DELIMITER = "<!-12341234@12341234-!>";

    // Message from BluetoothServer
    int DEVICE_CONNECTED_SUCCESSFULLY_TO_BLUETOOTH_SERVER = 10;

    // Message from BluetoothClient
    int FAILED_CONNECTING_TO_DEVICE = 20;
    int SUCCEED_CONNECTING_TO_DEVICE = 21;
    int DEVICE_FAILED_CONNECTING_ME = 22;

    // Message from BLEAdvertising
    int BLE_ADVERTISE_ERROR = 30;
    int BLE_GATT_SERVER_ERROR =33;
    int BLE_SCAN_ERROR = 31;

    // Message from BLEPERIPHERAL
    int GET_BLUETOOTH_SERVER_READY = 32;

    // Message from BLEScan
    int FOUND_NEW_DEVICE = 40;
    int SCAN_FAILED = 41;

    // Constants used in BLManager
    int TIME_RELAY_SEARCH_INTERVAL =40*SECOND;//60
    int TIME_RELAY_SEARCH_INTERVAL_POWER_MODE = 40*SECOND;//60
    int TIME_RELAY_SCAN = 5*SECOND;//
    int TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST = 120*SECOND;//20
    int TIME_RELAY_KEEPS_FOUND_DEVICE_IN_LIST_POWER_MODE = 120*SECOND;//20
    int DELAY_AFTER_HANDSHAKE = 10*SECOND;//30
    int SCAN_FINISHED_WITHOUT_CHANGES = 50;
    int RESET_SEARCH_COUNTER = 51;
    int BLE_ERROR = 32;


    // Constants from Connectivity manager
    int CHANGE_PRIORITY = 43;

    // status
    int CONNECTED = 51;
    int DISCONNECTED = 52;
    int CONNECTING = 53;

    // Message from BluetoothConnected
    int READ_PACKET = 60;

    // Message from HandShake
    int FINISHED_HANDSHAKE = 70;
    int NEW_RELAY_MESSAGE = 71;
    int FAILED_DURING_HAND_SHAKE = 72;
    int WATCHDOG_TIMER = 15*SECOND;

    // Message from BLECentral
    int FOUND_MAC_ADDRESS = 80;

    // Constants used in BluetoothScan
    int SCAN_TIME = 5*SECOND;
    int DISCOVERABLE_TIME = 5;
    int FOUND_MAC_ADDRESS_FROM_BLSCAN = 90;
    int NOT_FOUND_ADDRESS_FROM_BLSCAN = 91;

}
