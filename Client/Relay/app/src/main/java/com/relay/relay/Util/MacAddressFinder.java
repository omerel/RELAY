package com.relay.relay.Util;

import android.bluetooth.BluetoothAdapter;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Created by omer on 16/05/2017.
 */

public class MacAddressFinder {

    public static String getBluetoothMacAddress() {

        String address = BluetoothAdapter.getDefaultAdapter().getAddress();
        if (address.equals("02:00:00:00:00:00")) {
            try {
                List<NetworkInterface> NetworkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface networkInterface : NetworkInterfaceList) {

                    if (!networkInterface.getName().equalsIgnoreCase("wlan0")) continue;

                    byte[] macBytes = networkInterface.getHardwareAddress();

                    if (macBytes == null) {
                        return "02:00:00:00:00:00";
                    } else {

                        StringBuilder macResult = new StringBuilder();
                        for (byte macByte : macBytes) {
                            macResult.append(String.format("%02X:", macByte));
                        }
                        if (macResult.length() > 0) {
                            macResult.deleteCharAt(macResult.length() - 1);
                        }
                        return macResult.toString();
                    }
                }
            } catch (Exception ex) {}
        }
        return address;
    }
}
