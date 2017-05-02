package com.relay.relay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.UuidGenerator;

import java.util.UUID;

import static com.relay.relay.MainActivity.SYSTEM_SETTING;

public class LoginActivity extends AppCompatActivity {


    private static final String TAG = "LoginActivity";
    public static final String IS_LOG_IN = "is_log_in";
    public static final String CURRENT_UUID_USER = "current_uuid_user";
    private static final int REQUEST_SIGNUP = 0;

    EditText mEditTextInputEmail;
    EditText mEditTextInputPassword;
    Button mButtonLogin;
    TextView mTextViewSignup;

    private SharedPreferences sharedPreferences;
    private boolean isLogIn;
    private String currentUUID;
    DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEditTextInputEmail = (EditText) findViewById(R.id.input_email);
        mEditTextInputPassword = (EditText) findViewById(R.id.input_password);
        mButtonLogin = (Button)findViewById(R.id.btn_login);
        mTextViewSignup = (TextView) findViewById(R.id.link_signup);

        dataManager = new DataManager(getBaseContext());

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // check if the is a user name
        sharedPreferences =  getSharedPreferences(SYSTEM_SETTING,0);
        isLogIn = sharedPreferences.getBoolean(IS_LOG_IN,false);
        currentUUID = sharedPreferences.getString(CURRENT_UUID_USER,"");

        // the user logged in before make sure that current user is not null
        if(isLogIn && !currentUUID.equals("") && dataManager.isDataManagerSetUp() ) {
            onLoginSuccess();
        }
        else{
            // update islogin
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(IS_LOG_IN,false);
            editor.commit();
        }


        mButtonLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!validate()) {
                    onLoginFailed();
                    return;
                }
                mButtonLogin.setEnabled(false);
                String email = mEditTextInputEmail.getText().toString();
                String password = mEditTextInputPassword.getText().toString();
                validateWithServer(email,password);
            }
        });

        mTextViewSignup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                signupDialog();
            }
        });
    }


    public void login(boolean okFromServer,String msg,String userUUID) {
        Log.d(TAG, "Login");
        SharedPreferences.Editor editor;

        if (okFromServer){
            if (currentUUID.equals(userUUID)){
                editor = sharedPreferences.edit();
                editor.putString(CURRENT_UUID_USER,userUUID);
                editor.putBoolean(IS_LOG_IN,true);
                editor.commit();
                onLoginSuccess();
            }
            else{
                loginWithNewUserDialog("Alert",
                        "Login with new user will delete all data of others users.\n" +
                                "Connection to internet is necessary to connect with new user ",userUUID);
            }

        }
        else{
            onLoginFailedWithServer(msg);
        }




    }

    private void validateWithServer(final String email, final String password) {

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Progress_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        progressDialog.dismiss();
                        boolean answerFromServer = true;
                        if (answerFromServer){
                            UuidGenerator uuidGenerator = new UuidGenerator();
                            try {
                                String userUUID =uuidGenerator.GenerateUUIDFromEmail(email).toString();
                                login(true,"Error, problem with server",userUUID);
                            } catch (Exception e) {
                                login(false,"Error, email is not valid","");
                                e.printStackTrace();
                            }
                        }
                    }
                }, 3000);

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

    public void onLoginSuccess() {
        mButtonLogin.setEnabled(true);
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(CURRENT_UUID_USER,currentUUID);
        startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        mButtonLogin.setEnabled(true);
    }

    public void onLoginFailedWithServer(String msg) {
        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        mButtonLogin.setEnabled(true);
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
    private void loginWithNewUserDialog (String title,String msg,final String userUUID) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "continue",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mButtonLogin.setEnabled(true);
                            //TODO need to fulfill  createNewDataBase method
//                            SharedPreferences.Editor editor = sharedPreferences.edit();
//                            editor.putString(CURRENT_UUID_USER,userUUID);
//                            editor.putBoolean(IS_LOG_IN,true);
//                            editor.commit();
//                            createNewDataBase(userUUID);
//                            onLoginSuccess();
                        }
                    });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mButtonLogin.setEnabled(true);
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
        alertDialog.setMessage("Going to sign up page will delete all history data of current user.\n" +
                "Connection to internet is necessary to signup new user ");
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
}
