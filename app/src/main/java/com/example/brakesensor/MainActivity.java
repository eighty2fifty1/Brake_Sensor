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
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

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

    private TextView leftFrontTemp, rightFrontTemp, leftRearTemp, rightRearTemp, leftCenterTemp, rightCenterTemp;
    private ImageView lfBatt, rfBatt, lrBatt, rrBatt, lcBatt, rcBatt;


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
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    updateUIData();
                    // Stuff that updates the UI
                }
            });
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

        //enables UI components
        leftFrontTemp = (TextView) findViewById(R.id.leftFrontTemp);
        rightFrontTemp = (TextView) findViewById(R.id.rightFrontTemp);
        leftRearTemp = (TextView) findViewById(R.id.leftRearTemp);
        rightRearTemp = (TextView) findViewById(R.id.rightRearTemp);
        lfBatt = (ImageView) findViewById(R.id.lfBatt);
        rfBatt = (ImageView) findViewById(R.id.rfBatt);
        lrBatt = (ImageView) findViewById(R.id.lrBatt);
        rrBatt = (ImageView) findViewById(R.id.rrBatt);

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
        //Log.i(TAG, parts[0]);              //debugging
        for (int i = 0; i < 3; i++) {
            incomingDataInt[i] = Integer.parseInt(parts[i]);
        }
        Log.i(TAG, "incoming data: " + incomingDataInt[0]);
        switch (incomingDataInt[0]){
            case 1:
                lfData =  incomingDataInt.clone();
                Log.i(TAG, String.valueOf(lfData[1]));
                Log.i(TAG, String.valueOf(rfData[1]));
                Log.i(TAG, String.valueOf(lrData[1]));
                Log.i(TAG, String.valueOf(rrData[1]));
                break;
            case 2:
                rfData = incomingDataInt.clone();
                Log.i(TAG, String.valueOf(lfData[1]));
                Log.i(TAG, String.valueOf(rfData[1]));
                Log.i(TAG, String.valueOf(lrData[1]));
                Log.i(TAG, String.valueOf(rrData[1]));
                break;
            case 3:
                lrData = incomingDataInt.clone();
                Log.i(TAG, String.valueOf(lfData[1]));
                Log.i(TAG, String.valueOf(rfData[1]));
                Log.i(TAG, String.valueOf(lrData[1]));
                Log.i(TAG, String.valueOf(rrData[1]));
                break;
            case 4:
                rrData = incomingDataInt.clone();
                Log.i(TAG, String.valueOf(lfData[1]));
                Log.i(TAG, String.valueOf(rfData[1]));
                Log.i(TAG, String.valueOf(lrData[1]));
                Log.i(TAG, String.valueOf(rrData[1]));
                break;
            case 5:
                lcData = incomingDataInt.clone();
                break;
            case 6:
                rcData = incomingDataInt.clone();
                break;
        }
    }

    //updates all UI info
    private void updateUIData(){
        //updates temp
        leftFrontTemp.setText(String.valueOf(lfData[1]));
        rightFrontTemp.setText(String.valueOf(rfData[1]));
        leftRearTemp.setText(String.valueOf(lrData[1]));
        rightRearTemp.setText(String.valueOf(rrData[1]));

        //shows battery low icon if batt < 20%
        if (lfData[2] < 20){
            lfBatt.setVisibility(View.VISIBLE);
        }
        else lfBatt.setVisibility(View.INVISIBLE);

        if (rfData[2] < 20){
            rfBatt.setVisibility(View.VISIBLE);
        }
        else rfBatt.setVisibility(View.INVISIBLE);

        if (lrData[2] < 20){
            lrBatt.setVisibility(View.VISIBLE);
        }
        else lrBatt.setVisibility(View.INVISIBLE);

        if (rrData[2] < 20){
            rrBatt.setVisibility(View.VISIBLE);
        }
        else rrBatt.setVisibility(View.INVISIBLE);



    }



}