package com.relay.relay;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.relay.relay.DB.Test;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.UuidGenerator;

import java.util.UUID;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener ,
        PreferencesConnectionFragment.OnFragmentInteractionListener,
        InboxFragment.OnFragmentInteractionListener{

    private final String TAG = "RELAY_DEBUG: "+ MainActivity.class.getSimpleName();

    public static final String SYSTEM_SETTING = "relay.system_setting";
    public static final String CHANGE_PRIORITY_F = "relay.change_priority";
    public static final String MESSAGE_RECEIVED = "relay.BroadcastReceiver.MESSAGE";
    public static final String FRESH_FRAGMENT = "relay.BroadcastReceiver.FRESH_FRAGMENT";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    // tool bar and navigator
    private NavigationView navigationView;
    private View navHeaderView;

    // current fragment
    private Fragment mFragment;

    // animation between views
    private View mContentView;
    private View mLoadingView;
    private int mShortAnimationDuration;
    private TextView textViewUserName;
    private TextView textViewUserEmail;

    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    private  UUID mMyuuid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar  toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContentView = findViewById(R.id.content_body);
        mLoadingView = findViewById(R.id.loading_spinner);


        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        // get my uuid from login and put it in sharedPreferences
        //TODO delete uuidGenerator when creating login
        UuidGenerator uuidGenerator = new UuidGenerator();
        try {
            mMyuuid = uuidGenerator.GenerateUUIDFromEmail("Omer@gmail.com");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPreferences =  getSharedPreferences(SYSTEM_SETTING,0);
        // saving myuuid into sharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("my_uuid",mMyuuid.toString());


        navHeaderView= navigationView.getHeaderView(0);

        textViewUserName = (TextView) navHeaderView.findViewById(R.id.textView_menu_userName);
        textViewUserEmail = (TextView) navHeaderView.findViewById(R.id.textView_menu_user_mail);
        // Update navigator with name and email
        DataManager mDataManager = new DataManager(this);
        textViewUserEmail.setText(mDataManager.getNodesDB().getNode(mMyuuid).getEmail());
        String userName = mDataManager.getNodesDB().getNode(mMyuuid).getUserName();
        String fullName = mDataManager.getNodesDB().getNode(mMyuuid).getFullName();
        textViewUserName.setText("@"+userName+", "+fullName);

        // start on inbox
        displayFragment(0);

        // check Bluetooth support
        checkBluetoothAndBleSupport();

        // create BroadcastReceiver for new messages
        createBroadcastReceiver();

        checkPermissions();

        Test t = new Test(this);
         t.startTest();

        startService(new Intent(MainActivity.this,RelayConnectivityManager.class));
        Snackbar.make(this.mContentView, "Start RelayConnectivityManager service", Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // close menu
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // if on home screen -> exit
            if (mFragment.getClass().equals(InboxFragment.class)) {
                killService();
                unregisterReceiver(this.mBroadcastReceiver);
                super.onBackPressed();
            } else{
                //go back to home screen - inbox
                displayFragment(0);
                navigationView.setCheckedItem(R.id.nav_inbox);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_manual_handshake).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        // TODO  change
//        if (id == R.id.action_search) {
//           // navigationView.setCheckedItem(R.id.nav_connection_setting);
//            return super.onOptionsItemSelected(item);
//        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_inbox) {
            displayFragment(0);
        }else if (id == R.id.nav_connection_setting) {
            displayFragment(1);
        } else if (id == R.id.nav_user_properties) {

        } else if (id == R.id.nav_debug_screen) {

        } else if (id == R.id.nav_setting) {

        } else if (id == R.id.nav_logout) {

        } else if (id == R.id.nav_about_us) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void displayFragment(int position) {

        // Initially hide the content view.
        mContentView.setVisibility(View.INVISIBLE);

        mFragment = null;
        String title = getString(R.string.app_name);

        switch (position) {
            case 0:
                mFragment = new InboxFragment();
                title = getString(R.string.title_home_fragment);
                break;
            case 1:
                mFragment = new PreferencesConnectionFragment();
                title = getString(R.string.title_connection_fragment);
                break;
            case 2:
                break;
            default:
                break;
        }

        if (mFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.content_body, mFragment);
            fragmentTransaction.commit();

            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
        crossfade();
    }


    /**
     * Animation between two views
     */
    private void crossfade() {

        // setup progress bar
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.setAlpha(1f);

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        mContentView.setAlpha(0f);
        mContentView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        mContentView.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        mLoadingView.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoadingView.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * checkBluetoothAndBleSupport
     */
    private void checkBluetoothAndBleSupport() {

        if (BluetoothAdapter.getDefaultAdapter() == null)
            createAlertDialog("ERROR","Your device doesn't support bluetooth. you can't use" +
                    "this application",true);
        else {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                checkAdvertiseSupport();
        }
    }

    /**
     *  Create Exit alert dialog
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

    /**
     * checkAdvertiseSupport
     */
    private void checkAdvertiseSupport(){
        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported())
            createAlertDialog("NOTICE", "Your device doesn't support Bluetooth Low Energy " +
                    "advertisement (Beacon Transmission).\n Unfortunately your App's performance ,to get " +
                    "new messages from other users will be poor.\n To get better results use manual sync button" +
                    " when you are closes to another user.", false);
    }


    /**
     * BroadcastReceiver
     */
    private  void createBroadcastReceiver() {
        mFilter = new IntentFilter();
        mFilter.addAction(MESSAGE_RECEIVED);
        mFilter.addAction(FRESH_FRAGMENT);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action){
                    // When incoming message received
                    case MESSAGE_RECEIVED:
                        String relayMessage = intent.getStringExtra("relayMessage");
                        createAlertDialog("New message",relayMessage,false);
                        notifyMessageArrived(); //create ssound
                        break;
                    case FRESH_FRAGMENT:
                        refreshFragment(mFragment);
                        break;
                }

            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }

    private void refreshFragment(Fragment fragment){
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.detach(fragment);
        ft.attach(fragment);
        ft.commit();
    }
    /**
     *  Notify when new message arrived
     */
    public void notifyMessageArrived(){

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


    public void killService() {
        //  BroadCast to service
        Intent updateActivity = new Intent(RelayConnectivityManager.KILL_SERVICE);
        sendBroadcast(updateActivity);
    }


    public void changePriority(){
        //  BroadCast to service
        Intent updateActivity = new Intent(RelayConnectivityManager.CHANGE_PRIORITY_B);
        sendBroadcast(updateActivity);
    }

    @Override
    public void onFragmentInteraction(String string) {

        switch (string){
            case CHANGE_PRIORITY_F:
                changePriority();
                Snackbar.make(this.mContentView, " Changing connection priority " , Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                break;

        }
    }


}
