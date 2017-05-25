package com.relay.relay;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;

import java.util.Calendar;
import java.util.UUID;

import static com.relay.relay.MainActivity.SYSTEM_SETTING;
import static com.relay.relay.SignupActivity.FIRST_TIME_LOGIN;

public class SignInActivity extends AppCompatActivity {


    private static final String TAG = "SignInActivity";
    public static final String IS_LOG_IN = "is_log_in";
    public static final String CURRENT_UUID_USER = "current_uuid_user";
    public static final String CURRENT_UUID_PASSWORD = "current_uuid_password";
    private static final int REQUEST_SIGNUP = 0;

    EditText mEditTextInputEmail;
    EditText mEditTextInputPassword;
    Button mButtonSignIn;
    TextView mTextViewSignup;

    private SharedPreferences sharedPreferences;
    private boolean isLogIn;
    private String currentUUID;
    private String currentUUIDPassword;
    DataManager dataManager;

    private UuidGenerator uuidGenerator;

    // firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEditTextInputEmail = (EditText) findViewById(R.id.input_email);
        mEditTextInputPassword = (EditText) findViewById(R.id.input_password);
        mButtonSignIn = (Button)findViewById(R.id.btn_sign_in);
        mTextViewSignup = (TextView) findViewById(R.id.link_signup);
        // initialize data base
        dataManager = new DataManager(getBaseContext());

        //initialize fire base
        mAuth = FirebaseAuth.getInstance();

        uuidGenerator = new UuidGenerator();

        // hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // check if the is a user name
        sharedPreferences =  getSharedPreferences(SYSTEM_SETTING,0);
        isLogIn = sharedPreferences.getBoolean(IS_LOG_IN,false);
        currentUUID = sharedPreferences.getString(CURRENT_UUID_USER,"");
        currentUUIDPassword = sharedPreferences.getString(CURRENT_UUID_PASSWORD,"");

        // the user logged in before make sure that current user is not null
        if(isLogIn && !currentUUID.equals("") && !currentUUIDPassword.equals("") &&
                dataManager.isDataManagerSetUp() ) {
            onSignInSuccess(currentUUID,currentUUIDPassword);
        }
        else{
            // update islogin
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(IS_LOG_IN,false);
            editor.commit();
        }


        mButtonSignIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!validate()) {
                    onSignInFailed();
                    return;
                }
               // mButtonSignIn.setEnabled(false);
                String email = mEditTextInputEmail.getText().toString();
                String password = mEditTextInputPassword.getText().toString();

                // if the user was the last user who signed in the application signIn without
                // the need to check with server
                try {
                    String userUUID = uuidGenerator.GenerateUUIDFromEmail(email).toString();
                    if(currentUUID.equals(userUUID.toString())){
                         if (currentUUIDPassword.equals(password)){
                            onSignInSuccess(currentUUID,currentUUIDPassword);
                        }
                        else{
                             createAlertDialog("Sign in error","Password not valid.");
                             onSignInFailed();
                         }
                    }
                    else{
                        // check if connected to internet
                        boolean connected = isWifiAvailable() && isWifiConnected();
                        if (connected){
                            validateWithServer(email,password);
                        }
                        else{
                            createAlertDialog("Sign in error","to sign in with new user you must connect to the internet");
                            onSignInFailed();
                        }

                     }
                } catch (Exception e) {
                    e.printStackTrace();
                    // email not valid because of my email generator todo fix in the future
                    createAlertDialog("Sign in error","Email address is not valid at this moment.");
                    onSignInFailed();

                }


            }
        });

        mTextViewSignup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                signupDialog();
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }



    public void signIn(boolean okFromServer, String userUUID, String password) {
        Log.d(TAG, "Sign in");

        if (okFromServer){
            if (currentUUID.equals(FIRST_TIME_LOGIN)){
                onSignInSuccess(userUUID,password);
            }
            else{
                signInWithNewUserDialog("Alert",
                        "Sign in with new user will delete all data of others users.\n" +
                                "Connection to internet is necessary to restore your data ",userUUID,password);
            }
        }
    }

    private void validateWithServer(final String email, final String password) {

        final ProgressDialog progressDialog = new ProgressDialog(SignInActivity.this,
                R.style.AppTheme_Progress_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();


        // todo delete when no longer needed
        // backdoor to demo users with out using server
        if( email.split("@")[1].equals("relay.com") ){
            String userUUID = "";
            try {
                userUUID = uuidGenerator.GenerateUUIDFromEmail(email).toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            signInWithNewUserDialog("Alert",
                    "Sign in with new user will delete all data of others users.\n" +
                            "Connection to internet is necessary to restore your data ",userUUID,password);
        }


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            createAlertDialog("Sign in error",task.getException().getMessage());
                            onSignInFailed();
                        }
                        else{
                            // check if email verified
                            if (mAuth.getCurrentUser().isEmailVerified()){
                                String userUUID = "";
                                try {
                                    userUUID = uuidGenerator.GenerateUUIDFromEmail(email).toString();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                signIn(true,userUUID,password);
                            }
                            else{
                                // backdoor to users that create on fire base
                                 if( email.split("@")[1].equals("relay.com") ){
                                     String userUUID = "";
                                     try {
                                         userUUID = uuidGenerator.GenerateUUIDFromEmail(email).toString();
                                     } catch (Exception e) {
                                         e.printStackTrace();
                                     }
                                     signIn(true,userUUID,password);
                                }
                                // if not verified
                                else{
                                    createAlertDialog("Sign in error","Please check your email and verify the user first" );
                                    onSignInFailed();
                                }

                            }
                        }
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onSignInSuccess(String userUUID, String password) {

        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(CURRENT_UUID_USER,userUUID);
        editor.putString(CURRENT_UUID_PASSWORD,password);
        editor.putBoolean(IS_LOG_IN,true);
        editor.commit();

        mButtonSignIn.setEnabled(true);
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(CURRENT_UUID_USER,currentUUID);
        startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void onSignInFailed() {
        Toast.makeText(getBaseContext(), "Sign in failed", Toast.LENGTH_LONG).show();
        mButtonSignIn.setEnabled(true);
    }

    public void onSignInFailedWithServer(String msg) {
        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        mButtonSignIn.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = mEditTextInputEmail.getText().toString();
        String password = mEditTextInputPassword.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEditTextInputEmail.setError("enter a valid email address");
            valid = false;
        } else {
            mEditTextInputEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            mEditTextInputPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            mEditTextInputPassword.setError(null);
        }

        return valid;
    }


    /**
     *  Create alert dialog
     */
    private void signInWithNewUserDialog(String title, String msg, final String userUUID, final String password) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "continue",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mButtonSignIn.setEnabled(true);
                            // delete all data base
                            dataManager.deleteAlldataManager();

                            dataManager = new DataManager(getApplicationContext());

                            // set up new node with limit info and oldest date. when connected to relay server,
                            // the node will be update from the server
                            Calendar oldCalendarTime = Calendar.getInstance();
                            oldCalendarTime.set(Calendar.YEAR, 1999);
                            String email = uuidGenerator.GenerateEmailFromUUID(UUID.fromString(userUUID));
                            Node node = new Node(UUID.fromString(userUUID), oldCalendarTime,oldCalendarTime,
                                    2,email,"N/A",email.split("@")[0],email.split("@")[0], null,
                                    0,oldCalendarTime);
                            dataManager.getNodesDB().addNode(node);
                            dataManager.getNodesDB().setMyNodeId(node.getId());

                            onSignInSuccess(userUUID,password);
                        }
                    });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mButtonSignIn.setEnabled(true);
                        }
                    });

        alertDialog.show();
    }

    /**
     *  Create signup alert dialog
     */
    private void signupDialog () {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Warning");
        alertDialog.setMessage("Going to sign up page will delete all history data of existing users.\n" +
                "Connection to internet is necessary to sign up new user ");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "continue",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // delete all data base
                        dataManager.deleteAlldataManager();
                        // Start the Signup activity
                        Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                        startActivityForResult(intent, REQUEST_SIGNUP);
                        finish();
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });

        alertDialog.show();
    }


    public void createNewDataBase(String newDomainUser){

        // delete all data base
        dataManager = new DataManager(getBaseContext());
        dataManager.deleteAlldataManager();

        // TODO retrieve user data from server
    }

    /**
     *  Create alert dialog
     */
    private void createAlertDialog(String title,String msg) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();
    }

    @SuppressLint("WifiManagerLeak")
    private boolean isWifiAvailable() {
        return ((WifiManager)getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
    }

    private boolean isWifiConnected() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        boolean isWifiConnection =  activeNetworkInfo != null && activeNetworkInfo.isConnected() &&
                activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        Log.e(TAG, "Wifi connected: " + isWifiConnection);
        return isWifiConnection;
    }

}
