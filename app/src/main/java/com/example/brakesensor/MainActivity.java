package com.example.brakesensor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import in.unicodelabs.kdgaugeview.KdGaugeView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    //private Context context = this;
    public static SharedPreferences sharedPrefs;
    public static SharedPreferences.Editor editor;
    private boolean connected = false;
    public static BluetoothLEService mBluetoothLEService;
    public static Intent leServiceIntent;
    private int[] incomingDataInt = new int[3];
    private int[] lfData = new int[3];
    private int[] rfData = new int[3];
    private int[] lrData = new int[3];
    private int[] rrData = new int[3];
    private int[] lcData = new int[3];
    private int[] rcData = new int[3];

    private static String serverAddress;
    //ints for incoming message
    private int[] incomingMsgInt = new int[7];
    private int lfStatus, rfStatus, lrStatus, rrStatus, lcStatus, rcStatus, sensorsConnected;
    //ints for outgoing message
    private int serverReset, hardwareResetClient, softwareResetClient, macRequest, forceScan, sensorSleep;
    private int defaultSensors = 4;
    public static int sensorsExpected;

    private TextView leftFrontTemp, rightFrontTemp, leftRearTemp, rightRearTemp, leftCenterTemp, rightCenterTemp;
    private ProgressBar lfBatt, rfBatt, lrBatt, rrBatt, lcBatt, rcBatt;
    private KdGaugeView lfGauge, rfGauge, lrGauge, rrGauge, lcGauge, rcGauge;
    private Button[] statusLed = new Button[6];


    public static final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLEService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onservicedisconnected called ln 75");
            mBluetoothLEService = null;
        }
    };


    // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                //updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLEService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(BluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
                Log.i(TAG, "something received" + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
                serverAddress = intent.getStringExtra(BluetoothLEService.EXTRA_DATA);
            } else if (BluetoothLEService.TEMP_DATA.equals(action)) {
                //Log.i(TAG,"tempdata received: " + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
                parseNewData(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
            } else if (BluetoothLEService.STATUS_DATA.equals(action)) {
                Log.i(TAG, "status data received: " + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
                parseMessageData(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));

            }
            updateUIData();
        }
    };

    //////////////////////////////////////////////////////////////////
    //                                                              //
    //                          CALLBACKS                           //
    //                                                              //
    //////////////////////////////////////////////////////////////////


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
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
        sensorsExpected = sharedPrefs.getInt(String.valueOf(R.integer.sensors_expected), defaultSensors);

        //enables UI components
        lfBatt = (ProgressBar) findViewById(R.id.lfBatt);
        rfBatt = (ProgressBar) findViewById(R.id.rfBatt);
        lrBatt = (ProgressBar) findViewById(R.id.lrBatt);
        rrBatt = (ProgressBar) findViewById(R.id.rrBatt);
        lfGauge = (KdGaugeView) findViewById(R.id.lfGauge);
        rfGauge = (KdGaugeView) findViewById(R.id.rfGauge);
        lrGauge = (KdGaugeView) findViewById(R.id.lrGauge);
        rrGauge = (KdGaugeView) findViewById(R.id.rrGauge);
        statusLed[0] = (Button) findViewById(R.id.lfLed);
        statusLed[1] = (Button) findViewById(R.id.rfLed);
        statusLed[2] = (Button) findViewById(R.id.lrLed);
        statusLed[3] = (Button) findViewById(R.id.rrLed);

        leServiceIntent = new Intent(this, BluetoothLEService.class);
        //bindService(leServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(leServiceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        //bindService(leServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.control_panel:
                startActivity(new Intent(this, ControlPanelActivity.class));
                return true;
            case R.id.appearance:
                return true;
            case R.id.datalog:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //unbindService(mServiceConnection);
        unregisterReceiver(gattUpdateReceiver);
    }

    ///////////////////////////////////////////////////////////////
    //                                                           //
    //                     CUSTOM METHODS                        //
    //                                                           //
    ///////////////////////////////////////////////////////////////

    //parses incoming string into arrays of ints and divides them into individual positions
    private void parseNewData(String newData) {
        try {
            String[] parts = newData.split("i");            //index, temp, batt
            //Log.i(TAG, parts[0]);              //debugging
            for (int i = 0; i < parts.length; i++) {
                incomingDataInt[i] = Integer.parseInt(parts[i]);
            }
            //Log.i(TAG, "incoming data: " + incomingDataInt[0]);
            switch (incomingDataInt[0]) {
                case 1:
                    lfData = incomingDataInt.clone();
                    break;
                case 2:
                    rfData = incomingDataInt.clone();
                    break;
                case 3:
                    lrData = incomingDataInt.clone();
                    break;
                case 4:
                    rrData = incomingDataInt.clone();
                    break;
                case 5:
                    lcData = incomingDataInt.clone();
                    break;
                case 6:
                    rcData = incomingDataInt.clone();
                    break;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void parseMessageData(String msgData) {
        String[] parts = msgData.split("i");            //index, temp, batt
        //Log.i(TAG, parts[0]);              //debugging
        for (int i = 0; i < parts.length; i++) {
            incomingMsgInt[i] = Integer.parseInt(parts[i]);
        }
        for (int i = 0; i < sensorsExpected; i++) {
            setLedColor(statusLed[i], incomingMsgInt[i]);
        }
/*
        lfStatus = incomingMsgInt[0];
        rfStatus = incomingMsgInt[1];
        lrStatus = incomingMsgInt[2];
        rrStatus = incomingMsgInt[3];
        lcStatus = incomingMsgInt[4];
        rcStatus = incomingMsgInt[5];

 */
        sensorsConnected = incomingMsgInt[6];
    }

    //updates all UI info
    void updateUIData() {
        //updates temp
        lfGauge.setSpeed(lfData[1]);
        rfGauge.setSpeed(rfData[1]);
        lrGauge.setSpeed(lrData[1]);
        rrGauge.setSpeed(rrData[1]);
        lfBatt.setProgress(lfData[2]);
        rfBatt.setProgress(rfData[2]);
        lrBatt.setProgress(lrData[2]);
        rrBatt.setProgress(rrData[2]);


    }

    private void setLedColor(final Button b, final int status) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (status) {
                        case 0:
                            b.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                            break;
                        case 1:
                            b.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                            break;
                        case 2:
                            b.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                            break;
                        case 3:
                            b.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                            break;
                    }

                    Log.i(TAG, "color change called");
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLEService.TEMP_DATA);
        intentFilter.addAction(BluetoothLEService.STATUS_DATA);
        return intentFilter;
    }

    public void displayStats(View view) {
        Toast.makeText(this, view.toString(), Toast.LENGTH_LONG).show();
    }
}