package com.relay.relay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;

import java.util.Calendar;
import java.util.UUID;

import static com.relay.relay.LoginActivity.CURRENT_UUID_USER;
import static com.relay.relay.LoginActivity.IS_LOG_IN;
import static com.relay.relay.MainActivity.SYSTEM_SETTING;

public class SignupActivity extends AppCompatActivity implements SignupStepFragment.OnFragmentInteractionListener {

    private final String TAG = "RELAY_DEBUG: "+ SignupActivity.class.getSimpleName();
    public  static  final int STEP_1_FULL_NAME = 1;
    public static final int STEP_2_USER_NAME = 2;
    public static final int STEP_3_EMAIL = 3;
    public static final int STEP_4_PASSWORD = 4;
    public static final int STEP_5_PICTURE = 5;
    public static final int STEP_6_RESIDENCE = 6;
    public static final int STEP_7_FINISH = 7;
    public static final int STEP_NEXT = 10;
    public static final int STEP_ERROR = 12;
    public static final int STEP_BACK = 14;


    private int currentStep;

    private String mFullName;
    private String mUserName;
    private String mPhoneNumber;
    private String mEmail;
    private int mResidence;
    private String mPassword;
    private Bitmap mPicture;
    private String mConfimCode;

    // current fragment
    private Fragment mFragment;

    // animation between views
    private View mContentView;
    private View mLoadingView;
    private int mShortAnimationDuration;

    // views
    private Button mButtonSignUp;
    private TextView mTextViewLogin;

    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sharedPreferences =  getSharedPreferences(SYSTEM_SETTING,0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_LOG_IN,false);
        editor.commit();

        mFullName = null;
        mUserName = null;
        mPhoneNumber = null;
        mEmail = null;
        mResidence = 0;
        mPassword = null;
        mPicture = null;
        mConfimCode = "";

        mContentView = findViewById(R.id.content_body);
        mLoadingView = findViewById(R.id.loading_spinner);
        mButtonSignUp = (Button) findViewById(R.id.btn_signup);
        mTextViewLogin = (TextView)findViewById(R.id.link_login);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);


        mButtonSignUp.setVisibility(View.INVISIBLE);


        // start signup
        displayFragment(STEP_1_FULL_NAME,null);

        mButtonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        mTextViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
    }


    private void displayFragment(int step,Object input) {

        //Initially hide the content view.
        mFragment = null;
        currentStep = step;
        mContentView.setVisibility(View.INVISIBLE);
        mFragment = SignupStepFragment.newStepInstance(step,input);
        if (mFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.content_body, mFragment);
            fragmentTransaction.commit();
        }
        crossfade(mShortAnimationDuration);
    }

    /**
     * Animation between two views
     */
    private void crossfade(int mShortAnimationDuration) {

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

    @Override
    public void onFragmentInteraction(int answer,Object input) {

        switch (answer){
            case STEP_NEXT:
                if(currentStep == STEP_7_FINISH) {
                    updateInput(currentStep,input);
                }
                else{
                    if (currentStep == STEP_6_RESIDENCE)
                        mButtonSignUp.setVisibility(View.VISIBLE);
                    updateInput(currentStep,input);
                    displayFragment(currentStep+1,getInput(currentStep+1));
                }
                break;
            case STEP_ERROR:
                Toast.makeText(getBaseContext(), (String)input, Toast.LENGTH_LONG).show();
                break;
            case STEP_BACK:
                if(currentStep > STEP_1_FULL_NAME) {
                    mButtonSignUp.setVisibility(View.INVISIBLE);
                    displayFragment(currentStep-1,getInput(currentStep-1));
                }
                break;
        }
    }

    private void updateInput(int answer, Object input) {
        switch (answer){
            case STEP_1_FULL_NAME:
                if (input != null)
                    mFullName = (String)input;
                break;
            case STEP_2_USER_NAME:
                if (input != null)
                    mUserName = (String)input;
                break;
            case STEP_3_EMAIL:
                if (input != null)
                    mEmail = (String)input;
                break;
            case STEP_4_PASSWORD:
                if (input != null)
                    mPassword = (String)input;
                break;
            case STEP_5_PICTURE:
                if (input != null)
                    mPicture = (Bitmap)input;
                break;
            case STEP_6_RESIDENCE:
                if (input != null)
                    mResidence = (int) input;
                break;
            case STEP_7_FINISH:
                if (input != null)
                    mConfimCode = (String) input;
                break;
        }
    }

    private Object getInput(int answer) {
        switch (answer){
            case STEP_1_FULL_NAME:
                if (mFullName != null)
                    return mFullName;
                else
                    return null;
            case STEP_2_USER_NAME:
                if (mUserName != null)
                    return mUserName;
                else
                    return null;
            case STEP_3_EMAIL:
                if (mEmail != null)
                    return mEmail;
                else
                    return null;
            case STEP_4_PASSWORD:
                if (mPassword != null)
                    return mPassword;
                else
                    return null;

            case STEP_5_PICTURE:
                if (mPicture != null)
                    return mPicture;
                else
                    return null;

            case STEP_6_RESIDENCE:
                if (mResidence != 0)
                    return mResidence;
                else
                    return 0;
            case STEP_7_FINISH:
                if (mConfimCode != null)
                    return mConfimCode;
                else
                    return "";
        }
        return null;
    }

    public void signup() {
        Log.d(TAG, "Signup");
        validateWithServer();
    }

    public void onSignupFailed(String msg) {
        createAlertDialog("Server error",msg);
       // mButtonSignUp.setVisibility(View.INVISIBLE);
    }


    private void validateWithServer() {

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Progress_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        progressDialog.dismiss();
                        boolean answerFromServer = true;
                        if (answerFromServer && mConfimCode.equals("1234")){
                            onSignupSuccess();
                        }
                        else{
                            if(!answerFromServer)
                                onSignupFailed("Email is already occupied , please change mail");
                            if( !mConfimCode.equals("1234"))
                                onSignupFailed("Confirmation code not correct, please try again");
                        }
                    }
                }, 3000);

    }
    public void onSignupSuccess() {
        //TODO create new node and set it up as my node!

        DataManager dataManager = new DataManager(this);
        UuidGenerator uuidGenerator = new UuidGenerator();
        UUID uuid = null;
        try {
            uuid = uuidGenerator.GenerateUUIDFromEmail(mEmail);
        } catch (Exception e) {
            e.printStackTrace();
            onSignupFailed("Error with email");
            return;
        }
        Node node = new Node(uuid, Calendar.getInstance(),Calendar.getInstance(),
                2,mEmail,mPhoneNumber,mUserName,mFullName, ImageConverter.ConvertBitmapToBytes(mPicture),
                mResidence,Calendar.getInstance());
        dataManager.getNodesDB().addNode(node);
        dataManager.getNodesDB().setMyNodeId(node.getId());


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CURRENT_UUID_USER,uuid.toString());
        editor.commit();

        finish();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        Toast.makeText(getBaseContext(), "Signup Success", Toast.LENGTH_LONG).show();
    }

    /**
     *  Create alert dialog
     */
    private void createAlertDialog(String title,String msg) {

        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();
    }
}
