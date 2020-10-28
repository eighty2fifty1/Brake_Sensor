package com.example.brakesensor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.example.brakesensor.databinding.ControlPanelActivityBinding;


public class ControlPanelActivity extends AppCompatActivity {
    private static final String TAG = ControlPanelActivity.class.getSimpleName();
    private BluetoothGattCharacteristic characteristic;
    private ControlPanelActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ControlPanelActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

    }

    @Override
    protected void onResume(){
        super.onResume();
        //bindService(MainActivity.leServiceIntent, MainActivity.mServiceConnection, BIND_ABOVE_CLIENT);

    }

    @Override
    protected void onPause(){
        super.onPause();
        //unbindService(MainActivity.mServiceConnection);
    }

    //////////////////////////////////////////////
    //                                          //
    //            BUTTON FUNCTIONS              //
    //                                          //
    //////////////////////////////////////////////

    public void setAxles(View view){

    }

    public void resetServer(View view){
        sendMessage("<3,0,0,0,0>");
    }

    public void clientSWReset(View view){
        sendMessage("<1,0,0,0,0>");
    }

    public void clientHWReset(View view){
        sendMessage("<2,0,0,0,0>");
    }

    public void getMACAddress(View view){
        sendMessage("<0,0,1,0,0>");
    }

    public void sleepSensor(View view){

    }

    public void sleepAll(View view){

    }

    public void forceScan(View view){
        sendMessage("<0,0,0,1,0>");
    }

    public void sendMessage(String msg){
        characteristic = BluetoothLEService.deviceGatt.getService(MyUUID.msgServiceUUID).getCharacteristic(MyUUID.msgCharacteristicUUID);
        Log.i("TAG", String.valueOf(characteristic));
        characteristic.setValue(msg);

        if (BluetoothLEService.deviceGatt.writeCharacteristic(characteristic)) {
            Log.i(TAG, "msg sent: " + msg);
        }
    }
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}