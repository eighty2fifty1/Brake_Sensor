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
import com.example.brakesensor.databinding.ActivityMainBinding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    //private Context context = this;
    public static SharedPreferences sharedPrefs;
    public static SharedPreferences.Editor editor;
    private Intent notifyServiceIntent;
    private ActivityMainBinding binding;

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
    private KdGaugeView[] tempGauge = new KdGaugeView[6];
    private Button[] statusLed = new Button[6];

    public static int[] hiTempWarn = {200,200,200,200,200,200};


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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
        sensorsExpected = sharedPrefs.getInt(String.valueOf(R.integer.sensors_expected), defaultSensors);

        configureUI(sensorsExpected);

        //enables UI components
        tempGauge[0] = binding.lfGauge;
        tempGauge[1] = binding.rfGauge;
        tempGauge[2] = binding.lrGauge;
        tempGauge[3] = binding.rrGauge;
        tempGauge[4] = binding.lcGauge;
        tempGauge[5] = binding.rcGauge;
        statusLed[0] = binding.lfLed;
        statusLed[1] = binding.rfLed;
        statusLed[2] = binding.lrLed;
        statusLed[3] = binding.rrLed;
        statusLed[4] = binding.lcLed;
        statusLed[5] = binding.rcLed;



        leServiceIntent = new Intent(this, BluetoothLEService.class);
        notifyServiceIntent = new Intent(this, NotificationService.class);
        //bindService(leServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(leServiceIntent);
        startService(notifyServiceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureUI(sensorsExpected);

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        //bindService(leServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
        stopService(notifyServiceIntent);       //should disable notifications while app is running in foreground

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
                startActivity(new Intent(this, DataLoggingActivity.class));
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
        startService(notifyServiceIntent);
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

        sensorsConnected = incomingMsgInt[6];
    }

    //updates all UI info
    void updateUIData() {
        //updates temp
        tempGauge[0].setSpeed(lfData[1]);
        tempGauge[1].setSpeed(rfData[1]);
        tempGauge[2].setSpeed(lrData[1]);
        tempGauge[3].setSpeed(rrData[1]);
        tempGauge[4].setSpeed(lcData[1]);
        tempGauge[5].setSpeed(rcData[1]);
        binding.lfBatt.setProgress(lfData[2]);
        binding.rfBatt.setProgress(rfData[2]);
        binding.lrBatt.setProgress(lrData[2]);
        binding.rrBatt.setProgress(rrData[2]);
        binding.lcBatt.setProgress(lcData[2]);
        binding.rcBatt.setProgress(rcData[2]);
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
                    //Log.i(TAG, "color change called");
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    public static IntentFilter makeGattUpdateIntentFilter() {
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

    private void configureUI(int sensors){
        switch (sensors / 2){
            case 1:
                binding.lrLed.setVisibility(View.INVISIBLE);
                binding.lrBatt.setVisibility(View.INVISIBLE);
                binding.lrGauge.setVisibility(View.INVISIBLE);
                binding.rrLed.setVisibility(View.INVISIBLE);
                binding.rrBatt.setVisibility(View.INVISIBLE);
                binding.rrGauge.setVisibility(View.INVISIBLE);
                binding.lcLed.setVisibility(View.INVISIBLE);
                binding.lcBatt.setVisibility(View.INVISIBLE);
                binding.lcGauge.setVisibility(View.INVISIBLE);
                binding.rcLed.setVisibility(View.INVISIBLE);
                binding.rcBatt.setVisibility(View.INVISIBLE);
                binding.rcGauge.setVisibility(View.INVISIBLE);
                break;
            case 2:
                binding.lcLed.setVisibility(View.INVISIBLE);
                binding.lcBatt.setVisibility(View.INVISIBLE);
                binding.lcGauge.setVisibility(View.INVISIBLE);
                binding.rcLed.setVisibility(View.INVISIBLE);
                binding.rcBatt.setVisibility(View.INVISIBLE);
                binding.rcGauge.setVisibility(View.INVISIBLE);

                //TODO modify constraints of rear widgets programatically
                break;
            case 3:
                break;
        }
    }
}