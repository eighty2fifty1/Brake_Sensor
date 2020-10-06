package com.example.brakesensor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.Calendar;
import java.util.UUID;

/**
 * Implementation of the Bluetooth GATT Time Profile.
 * https://www.bluetooth.com/specifications/adopted-specifications
 */
public class TempDataProfile {
    private static final String TAG = TempDataProfile.class.getSimpleName();
    public static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    public static UUID DATA_SERVICE = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    public static UUID DATA_CHAR = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb");


    // Adjustment Flags
    public static final byte ADJUST_NONE = 0x0;
    public static final byte ADJUST_MANUAL = 0x1;
    public static final byte ADJUST_EXTERNAL = 0x2;
    public static final byte ADJUST_TIMEZONE = 0x4;
    public static final byte ADJUST_DST = 0x8;



    public static BluetoothGattService createDataService() {
        BluetoothGattService service = new BluetoothGattService(DATA_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic tempData = new BluetoothGattCharacteristic(DATA_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        tempData.addDescriptor(configDescriptor);

        // Local Time Information characteristic


        service.addCharacteristic(tempData);


        return service;
    }
}

