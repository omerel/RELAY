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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button mSendButton;
    private EditText mContent;
    private TextView mTextViewSender;
    private TextView mTextViewReceiver;
    private boolean clicked = false;
    private String[] mId;
    public static Map<String,Object[]> db = new HashMap<>();

    //for testing
    RelayDevice mRelayDevice;
    Map<String,RelayDevice> mHashMapRelayDevice = new HashMap<>();

    private ArrayAdapter<String> mArrayAdapter;

    public static final String MESSAGE_RECEIVED = "relay.BroadcastReceiver.MESSAGE";
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    private String mSender;
    private String mReceiver;

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
        mSendButton = (Button) findViewById(R.id.send_button);
        mContent = (EditText) findViewById(R.id.content);
        mTextViewSender = (TextView) findViewById(R.id.sender);
        mTextViewReceiver = (TextView) findViewById(R.id.receiver);

        // Set on click listener
        mStartButton.setOnClickListener(this);
        mManualSyncButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);
        mTextViewSender.setOnClickListener(this);
        mTextViewReceiver.setOnClickListener(this);

        // bind device list
        mArrayAdapter = new ArrayAdapter<>(this,R.layout.item_device);
        mDevicesList.setAdapter(mArrayAdapter);

        // Set id options
        mId = new String[] {"A","B","C","D"};

        DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
        String date = df.format(Calendar.getInstance().getTime());

        Object[] obj = new Object[2];
        obj[0] = date;
        obj[1] = "test";
        db.put("A",obj);
        db.put("D",obj);
        db.put("C",obj);
        db.put("B",obj);

        checkPermissions();
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
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
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

                        if(mSender == null){
                            clicked = !clicked;
                            Toast.makeText(getApplicationContext(),"Please choose your id first",
                                    Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Start service",
                                    Toast.LENGTH_LONG).show();
                            Intent service = new Intent(MainActivity.this,ConnectivityManager.class);
                            service.putExtra("device_id",mSender);
                            startService(service);
                        }
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
            case (R.id.sender):

                AlertDialog.Builder builderType = new AlertDialog.Builder(this);
                builderType.setTitle("Select ID");
                builderType.setItems(mId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which){
                            case 0:
                                mTextViewSender.setText(mId[0]);
                                mSender = mId[0];
                                break;
                            case 1:
                                mTextViewSender.setText(mId[1]);
                                mSender = mId[1];
                                break;
                            case 2:
                                mTextViewSender.setText(mId[2]);
                                mSender = mId[2];
                                break;
                            case 3:
                                mTextViewSender.setText(mId[3]);
                                mSender = mId[3];
                                break;
                        }
                    }
                });
                builderType.show();
                mTextViewSender.setClickable(false);
                break;
            case (R.id.receiver):

                AlertDialog.Builder builderType2 = new AlertDialog.Builder(this);
                builderType2.setTitle("Select ID");
                builderType2.setItems(mId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which){
                            case 0:
                                mTextViewReceiver.setText(mId[0]);
                                mReceiver = mId[0];
                                break;
                            case 1:
                                mTextViewReceiver.setText(mId[1]);
                                mReceiver = mId[1];
                                break;
                            case 2:
                                mTextViewReceiver.setText(mId[2]);
                                mReceiver = mId[2];
                                break;
                            case 3:
                                mTextViewReceiver.setText(mId[3]);
                                mReceiver = mId[3];
                                break;
                        }
                    }
                });
                builderType2.show();
                break;
            case (R.id.send_button):
                String content = mContent.getText().toString();
                mContent.setText("");
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");

                if (!content.isEmpty()){

                    Object[] obj = new Object[2];
                    obj[0] = df.format(Calendar.getInstance().getTime());
                    obj[1] = "From: "+mSender+". "+content;

                    db.put(mReceiver,obj);
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
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

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

                        // When bluetooth state changed
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch(state) {
                            case BluetoothAdapter.STATE_OFF:
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                break;
                            case BluetoothAdapter.STATE_ON:
                                checkAdvertiseSupport();
                                break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                break;
                        }
                        break;
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

        Toast.makeText(getApplicationContext(), "Message arrived",
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
