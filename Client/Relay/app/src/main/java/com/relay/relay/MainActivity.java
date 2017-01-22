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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener ,  ContactListFragment.OnContactSelected {

    public static String A = "Omer";
    public static String B = "Barr";
    public static String C = "Boris";
    public static String D = "Ariel";
    public static String DELIMITER = "<!-@-!>";

    private final String TAG = "RELAY_DEBUG: "+ MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private boolean clicked = false;
    public static String[] mId;
    public static Map<String,Object[]> db = new HashMap<>();
    public static Map<String,ArrayList<RelayMesage>> mMapMessages = new HashMap<>();


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


        // setup toolbar
        Toolbar mMyToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mMyToolbar);
        getSupportActionBar().setTitle(" RELAY DEMO ");


        // Set id options
        mId = new String[] {A,B,C,D};

        // initial default db
        initialDbAndArray();

        checkPermissions();
    }

    private void initialDbAndArray() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);

        DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
        String date = df.format(calendar.getTime());

        Object[] obj = new Object[2];
        obj[0] = date;
        obj[1] = "test"+DELIMITER+"this is a test";
        db.put(A,obj);
        db.put(B,obj);
        db.put(C,obj);
        db.put(D,obj);

        // initial mapMessages
        ArrayList<RelayMesage> array;

        // init A
        array = mMapMessages.get(A);
        array = new ArrayList<>();
        mMapMessages.put(A,array);

        // init B
        array = mMapMessages.get(B);
        array = new ArrayList<>();
        mMapMessages.put(B,array);

        // init C
        array = mMapMessages.get(C);
        array = new ArrayList<>();
        mMapMessages.put(C,array);

        // init D
        array = mMapMessages.get(D);
        array = new ArrayList<>();
        mMapMessages.put(D,array);

    }



    /** Listener to the menu **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {


            case R.id.menu_start_service:
                clicked = !clicked;

                if (clicked) {
                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                        clicked = !clicked;
                    } else {

                        if (mSender == null) {
                            clicked = !clicked;
                            Toast.makeText(getApplicationContext(), "Please choose your id first",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Start service",
                                    Toast.LENGTH_LONG).show();
                            Intent service = new Intent(MainActivity.this, ConnectivityManager.class);
                            service.putExtra("device_id", mSender);
                            startService(service);
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Stop service",
                            Toast.LENGTH_LONG).show();
                    killService();
                }
                break;

            case R.id.menu_sync_manual:
                if (clicked)
                    startManualSync();
                else
                    Toast.makeText(getApplicationContext(), "Service need to be started",
                            Toast.LENGTH_LONG).show();
                break;

            case R.id.menu_choose_id:

                AlertDialog.Builder builderType = new AlertDialog.Builder(this);
                builderType.setTitle("Select ID");
                builderType.setItems(mId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                //mTextViewSender.setText("["+mId[0]+"] ");
                                mSender = mId[0];
                                //mSendButton.setClickable(true);
                                getSupportActionBar().setTitle("Hi "+ mId[0]);
                                startContactFragment();
                                break;
                            case 1:
                              //  mTextViewSender.setText("["+mId[1]+"] ");
                                mSender = mId[1];
                                //mSendButton.setClickable(true);
                                getSupportActionBar().setTitle("Hi "+ mId[1]);
                                startContactFragment();
                                break;
                            case 2:
                              //  mTextViewSender.setText("["+mId[2]+"] ");
                                mSender = mId[2];
                                //mSendButton.setClickable(true);
                                getSupportActionBar().setTitle("Hi "+ mId[2]);
                                startContactFragment();
                                break;
                            case 3:
                              //  mTextViewSender.setText("["+mId[3]+"] ");
                                mSender = mId[3];
                                //mSendButton.setClickable(true);
                                getSupportActionBar().setTitle("Hi "+ mId[3]);
                                startContactFragment();
                                break;
                        }
                    }
                });
                builderType.show();
                break;
                case android.R.id.home:
                    onBackPressed();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    getSupportActionBar().setDisplayShowHomeEnabled(false);
                    getSupportActionBar().setTitle("Hi "+mSender+", Select Contact List");
                    updateFragments();
                    break;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /** Make the action button appear in old devices **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        // Update menu titles and icons
        return super.onCreateOptionsMenu(menu);
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
                        //String relayMessage = intent.getStringExtra("relayMessage");
                        RelayMesage newMessage;
                        String time = (String)(db.get(mSender)[0]);
                        String content = (String)(db.get(mSender)[1]);
                        String[] data = content.split(DELIMITER);

                        newMessage = new RelayMesage(data[0],mSender,data[1],null,time);

                        //mArrayMessages.add(newMessage);
                        mMapMessages.get(data[0]).add(newMessage);

                        updateFragments();

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


    public void updateFragments(){
        ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag("chatFragment");
        if (chatFragment != null)
            chatFragment.notifyDataSetChanged();

        ContactListFragment contactListFragment = (ContactListFragment) getSupportFragmentManager().findFragmentByTag("contactListFragment");
        if (contactListFragment != null)
            contactListFragment.notifyDataSetChanged();
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

    @Override
    public void OnContactSelected(String name) {
        final ChatFragment chatFragment =
                ChatFragment.newInstance(mSender, name);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.root_layout, chatFragment, "chatFragment")
                .addToBackStack(null)
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Chat with "+name);

    }


    public static class RelayMesage{

        String sender;
        String receive;
        String textMsg;
        String image;
        String time;

        public RelayMesage(String sender, String receive, String textMsg,String image,String time) {
            this.sender = sender;
            this.receive = receive;
            this.textMsg = textMsg;
            this.image = image;
            this.time = time;
        }

    }

    public void startContactFragment(){
        final ContactListFragment contactListFragment =
                ContactListFragment.newInstance(mSender);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.root_layout, contactListFragment, "contactListFragment")
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setTitle("Hi "+mSender+", Select Contact List");
    }
}


