package com.relay.relay;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.relay.relay.DB.BlConnectionLogDB;
import com.relay.relay.Util.BlConnectionLoggerListArrayAdapter;
import com.relay.relay.Util.BluetoothConnectionLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static com.relay.relay.Util.StatusBar.FLAG_ADVERTISEMENT;
import static com.relay.relay.Util.StatusBar.FLAG_CONNECTING;
import static com.relay.relay.Util.StatusBar.FLAG_ERROR;
import static com.relay.relay.Util.StatusBar.FLAG_HANDSHAKE;
import static com.relay.relay.Util.StatusBar.FLAG_NO_CHANGE;
import static com.relay.relay.Util.StatusBar.FLAG_SEARCH;

public class DebugActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "RELAY_DEBUG: "+ DebugActivity.class.getSimpleName();

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static String LOG_FILE = "relay_Log_file.csv";

    private List<BluetoothConnectionLogger> logList;
    private BlConnectionLoggerListArrayAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textViewDevice;
    private Button buttonSendLog;


    private Calendar calendar = Calendar.getInstance();
    private String dateIssue = calendar.get(Calendar.DATE)+"/"+calendar.get(Calendar.MONTH)+
            "/"+calendar.get(Calendar.YEAR)+"  "+calendar.get(Calendar.HOUR_OF_DAY)+":"+
            calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND);

    private String[] deviceDetails = {Build.BRAND,Build.MODEL,Build.VERSION.RELEASE,
            Build.getRadioVersion(),
            String.valueOf(BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()),dateIssue};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Device log information");

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_log);
        textViewDevice = (TextView) findViewById(R.id.text_view_device_details);
        buttonSendLog = (Button) findViewById(R.id.button_send_log);
        buttonSendLog.setOnClickListener(this);


        textViewDevice.setText("Brand: "+deviceDetails[0] +
                        "\nModel: "+deviceDetails[1] +
                        "\nVersion: "+ deviceDetails[2] +
                        "\nRadio version: "+deviceDetails[3] +
                        "\nSupport BLE advertisement: "+ deviceDetails[4] +
                        "\nDate of issue: "+ deviceDetails[5] );

        BlConnectionLogDB blConnectionLogDB = new BlConnectionLogDB(this);
        logList = blConnectionLogDB.getLogList();


        adapter = new BlConnectionLoggerListArrayAdapter(this, logList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setItemViewCacheSize(20);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.button_send_log:
                new Thread(new Runnable() {
                    public void run() {
                        generateLog(LOG_FILE,deviceDetails,logList);}}).start();
                break;
        }

    }



    public void generateLog(String fileName,String[] deviceDetails, List<BluetoothConnectionLogger> logList) {
        try {

          //  verifyStoragePermissions(this);

            String logMsg;
            String time;
            String type;
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"/");
            if (!root.exists()) {
                root.mkdirs();
            }
            File csvFile = new File(root, fileName);
            FileWriter writer = new FileWriter(csvFile);

            writer.append("Device details:\n");
            writer.append("Brand,Model,Version,Radio Version,Support BLE advertisement,Date of issue\n");
            writer.append(deviceDetails[0]+","+deviceDetails[1]+","+deviceDetails[2]+","+
                    deviceDetails[3]+","+deviceDetails[4]+","+deviceDetails[5]+"\n");
            writer.append("Log info:"+"\n");
            writer.append("Type,Time,Content"+"\n");

            for(int i = 0; i < logList.size(); i++ ){
                switch(logList.get(i).getFlagCode()){
                    case FLAG_ADVERTISEMENT:
                        type = "ADVERTISEMENT";
                        break;
                    case FLAG_SEARCH:
                        type = "SEARCH";
                        break;
                    case FLAG_CONNECTING:
                        type = "CONNECTING";
                        break;
                    case FLAG_HANDSHAKE:
                        type = "HANDSHAKE";
                        break;
                    case FLAG_ERROR:
                        type = "ERROR";
                        break;
                    case FLAG_NO_CHANGE:
                        type = "SCHEDULE";
                        break;
                    default:
                        type = "INFO";
                        break;
                }

                time = logList.get(i).getTimeWithMsg().split("-")[0];
                logMsg = logList.get(i).getTimeWithMsg().split("-")[1];
                writer.append(type+","+time+","+logMsg+"\n");
            }

            writer.flush();
            writer.close();
            Log.e(TAG,"File created");

            sendMail();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,e.getMessage().toString());
        }
    }


    private void sendMail(){
        //File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"/RelayLogFiles/"+LOG_FILE);
        File filelocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+LOG_FILE);
        Uri path = Uri.fromFile(filelocation);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // set the type to 'email'
        emailIntent .setType("vnd.android.cursor.dir/email");
        String to[] = {"elomer@gmail.com","barrinbar@gmail.com"};
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
        // the mail subject
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Relay, automated log file message. Device: "+Build.BRAND+","+Build.MODEL);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "See attachment\n");
        startActivity(Intent.createChooser(emailIntent , ""));
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
