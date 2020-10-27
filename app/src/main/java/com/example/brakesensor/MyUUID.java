package com.example.brakesensor;

import android.os.ParcelUuid;

import java.util.UUID;
//holds UUID's for the app
public class MyUUID {
    public static UUID myServiceUUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    public static UUID myCharacteristicUUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb");
    public static UUID msgServiceUUID = UUID.fromString("0000181C-0000-1000-8000-00805f9b34fb");
    public static UUID msgCharacteristicUUID = UUID.fromString("00002B25-0000-1000-8000-00805f9b34fb");
    public static ParcelUuid pServiceUUID = new ParcelUuid(myServiceUUID);
    public static ParcelUuid pCharacteristicUUID = new ParcelUuid(myCharacteristicUUID);
    public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
