package com.example.brakesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = TempDataProfile.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager mbluetoothManager = null;
    private BluetoothAdapter mbluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner =
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

    private BluetoothGatt deviceGatt;
    private BluetoothDevice device = null;

    private boolean mScanning = false;
    private Handler handler = new Handler();
    private UUID myServiceUUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private UUID myCharacteristicUUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb");
    private ParcelUuid pServiceUUID = new ParcelUuid(myServiceUUID);
    private ParcelUuid pCharacteristicUUID = new ParcelUuid(myCharacteristicUUID);
    public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int[] incomingDataInt = new int[3];
    private int[] lfData = new int[3];
    private int[] rfData = new int[3];
    private int[] lrData = new int[3];
    private int[] rrData = new int[3];
    private int[] lcData = new int[3];
    private int[] rcData = new int[3];


    //////////////////////////////////////////////////////////////////
    //                                                              //
    //                          CALLBACKS                           //
    //                                                              //
    //////////////////////////////////////////////////////////////////

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            gatt.discoverServices();

            Log.i(TAG, "connection state changed called");

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            super.onServicesDiscovered(gatt, status);
            BluetoothGattCharacteristic myCharacteristic = gatt.getService(myServiceUUID).getCharacteristic(myCharacteristicUUID);
            gatt.setCharacteristicNotification(myCharacteristic, true);

            BluetoothGattDescriptor desc = myCharacteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //Log.i(TAG, Arrays.toString(characteristic.getValue()));
            String incomingData = new String(characteristic.getValue(), StandardCharsets.UTF_8);
            //Log.i(TAG, incomingData);
            parseNewData(incomingData);
            printTempInLog();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i(TAG, String.valueOf(rssi));
        }


    };


    //prints the result of the scan
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, "device found");
            if (device == null){
                device = result.getDevice();
                bluetoothLeScanner.stopScan(scanCallback);
                bluetoothLeScanner.flushPendingScanResults(scanCallback);
                Log.i(TAG, device.toString());

                Log.i(TAG, "device created");
                deviceGatt = device.connectGatt(null, true, gattCallback);
            }
        }
    };

    //////////////////////////////////////////////////////////////////
    //                                                              //
    //                       CLASS OVERRIDES                        //
    //                                                              //
    //////////////////////////////////////////////////////////////////

    //on create method.  program starts here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mbluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mbluetoothAdapter = mbluetoothManager.getAdapter();
        //GattServerActivity server = new GattServerActivity();
        //server.onCreate(savedInstanceState);
        Log.i(TAG, "yeet");

        if (mbluetoothAdapter == null || !mbluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

       scanLeDevice();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

            if (deviceGatt == null) {
                return;
            }
            deviceGatt.close();
            deviceGatt = null;


    }

    ///////////////////////////////////////////////////////////////
    //                                                           //
    //                     CUSTOM METHODS                        //
    //                                                           //
    ///////////////////////////////////////////////////////////////

    private void scanLeDevice(){
        Log.i(TAG, "attempting to scan");
        final long SCAN_PERIOD = 5000;

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(pServiceUUID)

                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        filters.add(filter);

        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);       //need to create scan filter and settings
        Log.i(TAG, "scan started");
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);



    }

    //parses incoming string into arrays of ints and divides them into individual positions
    private void parseNewData(String newData){
        String[] parts = newData.split("i");            //index, temp, batt
        Log.i(TAG, parts[0]);              //debugging
        for (int i = 0; i < 3; i++) {
            incomingDataInt[i] = Integer.parseInt(parts[i]);
        }
        switch (incomingDataInt[0]){
            case 1:
                lfData = incomingDataInt;
                break;
            case 2:
                rfData = incomingDataInt;
                break;
            case 3:
                lrData = incomingDataInt;
                break;
            case 4:
                rrData = incomingDataInt;
                break;
            case 5:
                lcData = incomingDataInt;
                break;
            case 6:
                rcData = incomingDataInt;
                break;
        }
    }

    private void printTempInLog(){
        Log.i(TAG, "left front:" + lfData[0]);
        Log.i(TAG, "right front:" + rfData.toString());
        Log.i(TAG, "left center:" + lcData.toString());
        Log.i(TAG, "right center:" + rcData.toString());
        Log.i(TAG, "left rear:" + lrData.toString());
        Log.i(TAG, "right rear:" + rrData.toString());

    }



}