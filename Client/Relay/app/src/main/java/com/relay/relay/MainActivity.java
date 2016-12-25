package com.relay.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "RELAY_DEBUG: "+ MainActivity.class.getSimpleName();


    // Views
    private ListView mDevicesList;
    private Button mStartButton;
    private boolean clicked =false;


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
                Toast.makeText(getApplicationContext(),"Start service",
                        Toast.LENGTH_LONG).show();
                startService(new Intent(MainActivity.this,ConnectivityManager.class));
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

}
