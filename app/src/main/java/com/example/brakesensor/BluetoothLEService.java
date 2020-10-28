package com.example.brakesensor;

import android.app.Service;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothLEService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String TEMP_DATA = "temp_data";
    public final static String STATUS_DATA = "status_data";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    private static final String TAG = BluetoothLEService.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    private String mBluetoothDeviceAddress;
    private String action;

    protected static BluetoothGatt deviceGatt;
    private BluetoothDevice device = null;

    private boolean mScanning = false;
    private boolean connected = false;
    private Handler handler = new Handler();
    private BLEScanService scanner;
    private Intent scannerIntent;

    private int mConnectionState = STATE_DISCONNECTED;

    //class constructor.  may not be necessary
    public BluetoothLEService() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onCreate();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        scanner = new BLEScanService();
        Log.i(TAG, "yeet");


        scannerIntent = new Intent(this, BLEScanService.class);
        // TODO: make scanner and connecting operate with switch on UI
        registerReceiver(scanResultReceiver, makeScanResultIntentFilter());

        startService(scannerIntent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroying BLE service");
        if (deviceGatt == null) {
            return;
        }
        deviceGatt.close();
        deviceGatt = null;
    }




    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (gatt.discoverServices()){
                    Log.i(TAG, "stopping scan service");
                    stopService(scannerIntent);                 //stops service once gatt is connected.  doing it to clean up unnecessary services.  not sure if needed
            }}

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "disconnected.  scanning...");
                connected = false;
                scanner.scanLeDevice();

            }

            Log.i(TAG, "connection state changed called");

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            try {
                BluetoothGattCharacteristic myCharacteristic = gatt.getService(MyUUID.myServiceUUID).getCharacteristic(MyUUID.myCharacteristicUUID);
                BluetoothGattCharacteristic msgCharacteristic = gatt.getService(MyUUID.msgServiceUUID).getCharacteristic(MyUUID.msgCharacteristicUUID);
                gatt.setCharacteristicNotification(msgCharacteristic, true);
                gatt.setCharacteristicNotification(myCharacteristic, true);

                BluetoothGattDescriptor notifyDesc = myCharacteristic.getDescriptor(MyUUID.CONFIG_DESCRIPTOR);
                notifyDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(notifyDesc);

                BluetoothGattDescriptor indicateDesc = msgCharacteristic.getDescriptor(MyUUID.CONFIG_DESCRIPTOR);
                indicateDesc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                gatt.writeDescriptor(indicateDesc);

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        //called when characteristic changed
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (MyUUID.myCharacteristicUUID.equals(characteristic.getUuid())){
                action = TEMP_DATA;
            }
            else if (MyUUID.msgCharacteristicUUID.equals(characteristic.getUuid())){
                action = STATUS_DATA;
            }
            if (action != null) {
                broadcastUpdate(action, characteristic);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i(TAG, String.valueOf(rssi));
        }


    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        String incomingData = new String(characteristic.getValue(), StandardCharsets.UTF_8);
        intent.putExtra(EXTRA_DATA, incomingData);
        sendBroadcast(intent);
    }

    // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
    private final BroadcastReceiver scanResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "scan result received " + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));

            if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action) && !connected) {
                Log.i(TAG, "scan result received " + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
                connected = connect(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
            }
        }
    };



    public class LocalBinder extends Binder {
        BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        close();
        Log.i(TAG,"unbinding BLE Service");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize(){
        if (mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null){
                Log.e(TAG, "Unable to initialize Bluetooth Manager");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null){
            Log.e(TAG, "Unable to obtain Bluetooth Adapter");
            return false;
        }

        return true;

    }

    public boolean connect(final String address){
        if (mBluetoothAdapter == null && address.equals(mBluetoothDeviceAddress) && deviceGatt != null){
            Log.d(TAG, "Trying existing gatt");
            if(deviceGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;
            }
            else return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null){
            Log.w(TAG, "Device not found");
            return false;
        }

        deviceGatt = device.connectGatt(this, true, gattCallback);
        Log.d(TAG, "trying to create new connection");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;

    }

    public void disconnect() {
        if (mBluetoothAdapter == null || deviceGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        deviceGatt.disconnect();
    }

    public void close(){
        if (deviceGatt == null){
            return;
        }
        deviceGatt.close();
        deviceGatt = null;
    }

    private static IntentFilter makeScanResultIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEScanService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEScanService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEScanService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEScanService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }

}