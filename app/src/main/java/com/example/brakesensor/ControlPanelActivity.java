package com.example.brakesensor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.example.brakesensor.databinding.ControlPanelActivityBinding;


public class ControlPanelActivity extends AppCompatActivity {
    private static final String TAG = ControlPanelActivity.class.getSimpleName();
    private BluetoothGattCharacteristic characteristic;
    private ControlPanelActivityBinding binding;
    private int rbId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ControlPanelActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        switch (MainActivity.sensorsExpected) {
            case 2:
                rbId = binding.rb1.getId();
                break;
            case 4:
                rbId = binding.rb2.getId();
                break;
            case 6:
                rbId = binding.rb3.getId();
                break;
        }
        binding.axleGroup.check(rbId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //bindService(MainActivity.leServiceIntent, MainActivity.mServiceConnection, BIND_ABOVE_CLIENT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unbindService(MainActivity.mServiceConnection);
    }

    //////////////////////////////////////////////
    //                                          //
    //            BUTTON FUNCTIONS              //
    //                                          //
    //////////////////////////////////////////////

    public void setAxles(View view) {
        int id = binding.axleGroup.getCheckedRadioButtonId();
        switch (id) {
            case R.id.rb1:
                sendMessage("<0,2,0,0,0>");
                MainActivity.sensorsExpected = 2;
                break;
            case R.id.rb2:
                sendMessage("<0,4,0,0,0>");
                MainActivity.sensorsExpected = 4;
                break;
            case R.id.rb3:
                sendMessage("<0,6,0,0,0>");
                MainActivity.sensorsExpected = 6;
                break;
        }
        MainActivity.editor.putInt(String.valueOf(R.integer.sensors_expected), MainActivity.sensorsExpected);
        if (MainActivity.editor.commit()) {
            Log.i(TAG, "sensors saved");
        }
    }

    public void resetServer(View view) {
        sendMessage("<3,0,0,0,0>");
    }

    public void clientSWReset(View view) {
        sendMessage("<1,0,0,0,0>");
    }

    public void clientHWReset(View view) {
        sendMessage("<2,0,0,0,0>");
    }

    public void getMACAddress(View view) {
        sendMessage("<0,0,1,0,0>");
    }

    public void sleepSensor(View view) {
        //needs to start with 1
        String sel = binding.sensorPositSpinner.getSelectedItem().toString();
        Log.i(TAG, sel);
        String msg = "<0,0,0,0,0>";
        switch (sel) {
            case "LF":
                msg = "<0,0,0,0,1>";
                break;
            case "RF":
                msg = "<0,0,0,0,2>";
                break;
            case "LR":
                msg = "<0,0,0,0,3>";
                break;
            case "RR":
                msg = "<0,0,0,0,4>";
                break;
            case "LC":
                msg = "<0,0,0,0,5>";
                break;
            case "RC":
                msg = "<0,0,0,0,6>";
                break;
        }
        sendMessage(msg);
    }

    public void sleepAll(View view) {
        sendMessage("<0,0,0,0,13>");
    }

    public void forceScan(View view) {
        sendMessage("<0,0,0,1,0>");
    }

    public void sendMessage(String msg) {
        try {
            characteristic = BluetoothLEService.deviceGatt.getService(MyUUID.msgServiceUUID).getCharacteristic(MyUUID.msgCharacteristicUUID);
            //Log.i("TAG", String.valueOf(characteristic));
            characteristic.setValue(msg);

            if (BluetoothLEService.deviceGatt.writeCharacteristic(characteristic)) {
                Log.i(TAG, "msg sent: " + msg);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}