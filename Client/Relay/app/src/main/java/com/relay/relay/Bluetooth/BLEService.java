package com.relay.relay.Bluetooth;
/**
 * BLEService
 * create the service that given to ble client
 * the service share the Mac address of the bluetooth
 */


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public class BLEService implements BLConstants  {

    private BluetoothGattService mAddressService;
    private BluetoothGattCharacteristic mAddressCharacteristic;

    /**
     * BLEService constructor
     * @param address Mac address of the device
     */
    public BLEService(String address) {

        // create Characteristic
        mAddressCharacteristic =
                new BluetoothGattCharacteristic(MAC_ADDRESS_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        mAddressCharacteristic.setValue(address);

        // create Characteristic service
        mAddressService = new BluetoothGattService(RELAY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mAddressService.addCharacteristic(mAddressCharacteristic);
    }

    /**
     * BluetoothGattService getter
     * @return BluetoothGattService
     */
    public BluetoothGattService getBluetoothGattService() {
        return mAddressService;
    }

    public BluetoothGattCharacteristic getAddressCharacteristic() {
        return mAddressCharacteristic;
    }


}
