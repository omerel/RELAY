package com.relay.relay;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "RELAY_DEBUG: "+ MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Views
    private ListView mDevicesList;
    private Button mStartButton;
    private boolean clicked = false;


    private ArrayAdapter<String> mArrayAdapter;

    public static final String MESSAGE_RECEIVED = "relay.BroadcastReceiver.MESSAGE";
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createBroadcastReceiver();

        // Bind layout's view to class
        mStartButton = (Button) findViewById(R.id.button);
        mDevicesList = (ListView)findViewById(R.id.listview);

        // Set on click listener
        mStartButton.setOnClickListener(this);

        // bind device list
        mArrayAdapter = new ArrayAdapter<>(this,R.layout.item_device);
        mDevicesList.setAdapter(mArrayAdapter);

        // make sure bluetooth enable
        BluetoothAdapter.getDefaultAdapter().enable();

        checkPermissions();
    }

    @TargetApi(23)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect ble devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    /**
     * on click listener
     * */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.button):

                clicked = !clicked;

                if (clicked){
                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        BluetoothAdapter.getDefaultAdapter().enable();
                        clicked = !clicked;
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Start service",
                                Toast.LENGTH_LONG).show();
                        startService(new Intent(MainActivity.this,ConnectivityManager.class));
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Stop service",
                            Toast.LENGTH_LONG).show();
                    killService();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killService();
        unregisterReceiver(this.mBroadcastReceiver);
    }

    private  void createBroadcastReceiver() {

        mFilter = new IntentFilter();
        mFilter.addAction(MESSAGE_RECEIVED);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){

                    // When incoming message received
                    case MESSAGE_RECEIVED:
                        String relayMessage = intent.getStringExtra("relayMessage");
                        mArrayAdapter.add(relayMessage);
                        mDevicesList.setSelection(mArrayAdapter.getCount()-1);
                        notifyMessageArrived();
                }

            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }


    public void killService() {
        //  BroadCast to service
        Intent updateActivity = new Intent(ConnectivityManager.KILL_SERVICE);
        sendBroadcast(updateActivity);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover ble devices when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }



    /**
     *  Notify when new message arrived
     */

    public void notifyMessageArrived(){

        Toast.makeText(getApplicationContext(), "Finish handshake",
                Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //Define Notification Manager
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            //Define sound URI
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSound(soundUri); //This sets the sound to play
            //Display notification
            notificationManager.notify(0, mBuilder.build());
        }
    }
}
