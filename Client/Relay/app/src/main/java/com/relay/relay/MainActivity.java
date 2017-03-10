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

import com.relay.relay.DB.Test;
import com.relay.relay.SubSystem.ConnectivityManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "RELAY_DEBUG: "+ MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Views
    private ListView mDevicesList;
    private Button mStartButton;
    private Button mManualSyncButton;
    private boolean clicked = false;

    //for testing
    RelayDevice mRelayDevice;
    Map<String,RelayDevice> mHashMapRelayDevice = new HashMap<>();

    private ArrayAdapter<String> mArrayAdapter;

    public static final String MESSAGE_RECEIVED = "relay.BroadcastReceiver.MESSAGE";
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBluetoothAndBleSupport();

        createBroadcastReceiver();

        // Bind layout's view to class
        mStartButton = (Button) findViewById(R.id.button);
        mDevicesList = (ListView)findViewById(R.id.listview);
        mManualSyncButton = (Button) findViewById(R.id.button2);

        // Set on click listener
        mStartButton.setOnClickListener(this);
        mManualSyncButton.setOnClickListener(this);

        // bind device list
        mArrayAdapter = new ArrayAdapter<>(this,R.layout.item_device);
        mDevicesList.setAdapter(mArrayAdapter);

        checkPermissions();

//        Test t = new Test(this);
//        t.startTest();

    }

    private void checkBluetoothAndBleSupport() {

        if (BluetoothAdapter.getDefaultAdapter() == null)
            createAlertDialog("ERROR","Your device doesn't support bluetooth. you can't use" +
                    "this application",true);
        else {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                checkAdvertiseSupport();
            else
                enableBluetooth();
        }
    }

    private void checkAdvertiseSupport(){
        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported())
            createAlertDialog("NOTICE", "Your device doesn't support Bluetooth Low Energy " +
                    "advertisement (Beacon Transmission).\n Unfortunately your App's performance ,to get " +
                    "new messages from other users will be poor.\n To get better results use manual sync button" +
                    " when you are closes to another user.", false);
    }

    private void enableBluetooth(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("This app needs Bluetooth to be available");
        builder.setMessage("By pressing ok bluetooth will be open");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                BluetoothAdapter.getDefaultAdapter().enable();
                checkAdvertiseSupport();
            }
        });
        builder.show();
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
                        startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
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

            case(R.id.button2):
                if(clicked)
                    startManualSync();
                else
                    Toast.makeText(getApplicationContext(),"Service need to be started",
                        Toast.LENGTH_LONG).show();
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
                        mArrayAdapter.add(MainActivity.this.setResult(relayMessage));
                        mDevicesList.setSelection(mArrayAdapter.getCount()-1);
                        notifyMessageArrived();
                }

            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }

    private String setResult(String relayMessage) {
        if (!mHashMapRelayDevice.containsKey(relayMessage)) {
            mRelayDevice = new RelayDevice(relayMessage);
        }
        else
            mRelayDevice = mHashMapRelayDevice.get(relayMessage);
        mRelayDevice.add();
        mHashMapRelayDevice.put(relayMessage,mRelayDevice);

        String result= "";

        result =result +  mRelayDevice.address+": " + mRelayDevice.count+" times\n";

        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
        String date = df.format(Calendar.getInstance().getTime());

        result = result +"time : "+date;
        return result;
    }


    public void killService() {
        //  BroadCast to service
        Intent updateActivity = new Intent(ConnectivityManager.KILL_SERVICE);
        sendBroadcast(updateActivity);
    }

    public void startManualSync(){
        //  BroadCast to service
        Intent updateActivity = new Intent(ConnectivityManager.MANUAL_SYNC);
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

    /**
     *  Create  Exit alert dialog
     */
    private void createAlertDialog(String title,String msg,boolean toExit) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        if (toExit){
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "EXIT",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                            System.exit(0);
                        }
                    });
        }
        else{
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }
        alertDialog.show();
    }


    private class RelayDevice{

        String address;
        int count;

        RelayDevice(String address){
            count = 0;
            this.address= address;
        }

        public void add(){
            count++;
        }
    }

}
