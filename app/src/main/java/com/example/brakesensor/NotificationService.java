package com.example.brakesensor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.nio.charset.StandardCharsets;

public class NotificationService extends Service {
    //types of messages to send
    private static final int OVERHEAT_WARNING = 1;
    private static final int SENSOR_LOW_BATT = 2;
    private static final int SENSOR_DISCONNECT = 3;
    private static final int REPEATER_DISCONNECT = 4;
    private static final int BATT_LOW_SETPOINT = 20;

    //debugging tag
    private static final String TAG = NotificationService.class.getSimpleName();
    private static final String CHANNEL_ID = "My Test Channel";

    private Intent mainActivityIntent;
    private Intent fullScreenIntent;
    private PendingIntent fullScreenPendingIntent;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);        //TODO figure out proper flag
        fullScreenIntent = new Intent(this, MainActivity.class);
        fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        createNotificationChannel();
        notificationManager = NotificationManagerCompat.from(this);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)                        //TODO find good notification icon
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        registerReceiver(gattUpdateReceiver, MainActivity.makeGattUpdateIntentFilter());
        return START_STICKY;
    }

    public void sendBatteryNotification(String posit) {
        builder.setContentTitle("test")               //TODO fix this
                .setContentText(posit + " sensor battery low.");
        notificationManager.notify(SENSOR_LOW_BATT, builder.build());
    }

    public void sendOverheatNotification(String posit) {
        Log.i(TAG, "overheat notification sending");
        builder.setContentTitle("test")
                .setContentText(posit + " SENSOR OVERHEAT WARNING.");
        notificationManager.notify(OVERHEAT_WARNING, builder.build());
    }

    public void sendSensorDisconnectNotification(int posit) {
        builder.setContentTitle("test")
                .setContentText(posit + " sensor disconnected cause unknown");
        notificationManager.notify(SENSOR_DISCONNECT, builder.build());
    }

    public void sendRepeaterDisconnectNotification() {
        builder.setContentTitle("test")
                .setContentText("Repeater disconnected cause unknown");
        notificationManager.notify(REPEATER_DISCONNECT, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        String incomingData = new String(characteristic.getValue(), StandardCharsets.UTF_8);
        intent.putExtra(BluetoothLEService.EXTRA_DATA, incomingData);
        sendBroadcast(intent);
    }

    // result of read or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "scan result received " + intent.getStringExtra(BluetoothLEService.EXTRA_DATA));

            if (BluetoothLEService.TEMP_DATA.equals(action)) {
                hiTempBattMonitor(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
            }
            else if (BluetoothLEService.STATUS_DATA.equals(action)){
                connStatusMonitor(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
            }
            else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)){
                sendRepeaterDisconnectNotification();
            }
        }
    };

    private void connStatusMonitor(String stringExtra) {
        String[] parts = stringExtra.split("i");            //lf, rf, lr, rr, lc, rc
        for (int i = 0; i < parts.length - 1; i++){
            if (Integer.parseInt(parts[i]) == 2){
                sendSensorDisconnectNotification(1);
            }
        }
    }

    private void hiTempBattMonitor(String stringExtra) {
        String[] parts = stringExtra.split("i");            //index, temp, batt
        Log.i(TAG, "monitoring temp and batt");
        //compares incoming values to stored setpoints for temp warning
        if (MainActivity.hiTempWarn[Integer.parseInt(parts[0]) - 1] >= Integer.parseInt(parts[1])) {
            Log.i(TAG, "comparing " + MainActivity.hiTempWarn[Integer.parseInt(parts[0]) - 1] + " to " + Integer.parseInt(parts[1]));
            Log.i(TAG, "sending " + parts[0]);
            sendOverheatNotification(parts[0]);            //TODO "i" needs to correspond with position
        }

        if (BATT_LOW_SETPOINT > Integer.parseInt(parts[2])) {
            sendBatteryNotification(parts[0]);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}