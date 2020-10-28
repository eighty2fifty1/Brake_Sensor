package com.example.brakesensor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
/////////////////////////////////////////////////////////////////
//              Scan should return BLE device to connect to    //
//              Device or address??                            //
/////////////////////////////////////////////////////////////////

public class BLEScanService extends Service {
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
    private static final String TAG = BLEScanService.class.getSimpleName();
    private BluetoothLeScanner bluetoothLeScanner =
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler handler;
    private BluetoothDevice device = null;

    protected String deviceAddress;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public BLEScanService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        handler = new Handler();
        scanLeDevice();
        return START_NOT_STICKY;
    }

    //scan device.  item is returned in scan callback
    void scanLeDevice() {
        Log.i(TAG, "attempting to scan");

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(MyUUID.pServiceUUID)
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

    //prints the result of the scan
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, "device found");
            if (device == null) {
                device = result.getDevice();
                bluetoothLeScanner.stopScan(scanCallback);
                bluetoothLeScanner.flushPendingScanResults(scanCallback);
                Log.i(TAG, device.toString());
                deviceAddress = device.getAddress();
                if (deviceAddress == null){
                    Log.e(TAG, "unable to find device address");
                    return;
                }
                broadcastUpdate(ACTION_DATA_AVAILABLE, deviceAddress);
            }
        }
    };

    private void broadcastUpdate(final String action,
                                 final String stringToPass) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, stringToPass);
        Log.i(TAG, "sending" + stringToPass);
        sendBroadcast(intent);
    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
