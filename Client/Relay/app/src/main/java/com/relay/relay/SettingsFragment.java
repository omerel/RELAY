package com.relay.relay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;

import java.util.UUID;

import static com.relay.relay.MainActivity.SYSTEM_SETTING;
import static com.relay.relay.SignInActivity.CURRENT_UUID_PASSWORD;
import static com.relay.relay.SignInActivity.CURRENT_UUID_USER;
import static com.relay.relay.SignInActivity.IS_LOG_IN;


public class SettingsFragment extends Fragment implements View.OnClickListener {


    public static final String SYSTEM_SETTING_MUTE = "relay.system_setting.mute";
    public static final String SYSTEM_SETTING_CREDENTIAL = "relay.system_setting.credential";

    private final String TAG = "RELAY_DEBUG: " + SettingsFragment.class.getSimpleName();

    private final int CODE_FULL_NAME = 11;

    private Menu mMenu;
    private Node userNode;
    private String userUUID; // the user profile
    private String myUUID;
    private boolean mMute;

    // fragment view
    private View view = null;
    Switch switchMute;
    Button buttonChangePassword;
    Button buttonResetPassword;
    Button buttonCleanData;
    Button buttonDeleteUser;

    // database
    DataManager mDataManager;
    SharedPreferences sharedPreferences;

    private FirebaseAuth mAuth;

    private ProfileFragment.OnFragmentInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String userUUID) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString("userUUID", userUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "On create");
        // To enable editing the tool bar from fragment
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            userUUID = getArguments().getString("userUUID");
        }

        mDataManager = new DataManager(getContext());

        sharedPreferences = getActivity().getSharedPreferences(SYSTEM_SETTING, 0);
        myUUID = sharedPreferences.getString(CURRENT_UUID_USER, null);
        userNode = mDataManager.getNodesDB().getNode(UUID.fromString(userUUID));

        mMute = sharedPreferences.getBoolean(SYSTEM_SETTING_MUTE, true);

        mAuth = FirebaseAuth.getInstance();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.e(TAG, "onCreateView");
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        // todo not seen
        buttonChangePassword = (Button) view.findViewById(R.id.settings_fragment_button_1);
        buttonChangePassword.setOnClickListener(this);


        buttonCleanData = (Button) view.findViewById(R.id.settings_fragment_button_2);
        buttonCleanData.setOnClickListener(this);
        buttonDeleteUser = (Button) view.findViewById(R.id.settings_fragment_button_4);
        buttonDeleteUser.setOnClickListener(this);
        buttonResetPassword = (Button) view.findViewById(R.id.settings_fragment_button_5);
        buttonResetPassword.setOnClickListener(this);
        switchMute = (Switch) view.findViewById(R.id.settings_fragment_button_3);
        switchMute.setOnClickListener(this);
        switchMute.setChecked(mMute);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ProfileFragment.OnFragmentInteractionListener) {
            mListener = (ProfileFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    @Override
    public void onClick(View view) {

        boolean connected = false;
        switch (view.getId()) {

            case R.id.settings_fragment_button_1:
                // check if connected to internet
                connected = isWifiAvailable() && isWifiConnected();
                if (connected) {
                    openDialogSetPassword();
                } else {
                    createAlertDialog("Error", "You must be connected to the internet ");
                }
                break;
            case R.id.settings_fragment_button_2:
                makeSureCleanDialog("Clean Data","Are you sure?");
                break;
            case R.id.settings_fragment_button_4:
                // check if connected to internet
                connected = isWifiAvailable() && isWifiConnected();
                if (connected) {
                    deleteUserDialog();
                } else {
                    createAlertDialog("Error", "You must be connected to the internet ");
                }
                break;
            case R.id.settings_fragment_button_3:
                SharedPreferences.Editor editor;
                editor = sharedPreferences.edit();
                editor.putBoolean(SYSTEM_SETTING_MUTE, switchMute.isChecked());
                editor.commit();
                if (!switchMute.isChecked())
                    Toast.makeText(getContext(), "mute", Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings_fragment_button_5:
                // check if connected to internet
                connected = isWifiAvailable() && isWifiConnected();
                if (connected) {
                    makeSureResetDialog("Reset password","Are you sure?");
                } else {
                    createAlertDialog("Error", "You must be connected to the internet ");
                }

                break;

        }
    }


    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_manual_handshake).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_approve).setVisible(false);

        // save current menu;
        mMenu = menu;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == android.R.id.home) {
            getActivity().onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    /**
     * Create alert dialog
     */
    private void createAlertDialog(String title, String msg) {

        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(getActivity()).create();
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


    private void deleteUserDialog() {

        credentialsUser();
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Warning");
        alertDialog.setMessage("Delete user will delete all data that exist in your device and you" +
                " no longer will able to sign in with the current user");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteUser();
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


    private void deleteUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");
                            // delete all data base
                            mDataManager.deleteAlldataManager();
                            // Start the Signup activity
                            Intent intent = new Intent(getContext(), SignupActivity.class);
                            startActivityForResult(intent, 1);
                            getActivity().finish();
                            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                            Toast.makeText(getContext(), "User account deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void credentialsUser() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.

        UuidGenerator uuidGenerator = new UuidGenerator();
        String uuidString = sharedPreferences.getString(CURRENT_UUID_USER, "");
        String email = uuidGenerator.GenerateEmailFromUUID(UUID.fromString(uuidString));
        String password = sharedPreferences.getString(CURRENT_UUID_PASSWORD, "");
        final AuthCredential[] credential = {EmailAuthProvider
                .getCredential(email,password)};

        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential[0])
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    boolean succ = false;
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated.");
                        }
                    }
                });
    }


    @SuppressLint("WifiManagerLeak")
    private boolean isWifiAvailable() {
        return ( (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
    }

    private boolean isWifiConnected() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        boolean isWifiConnection =  activeNetworkInfo != null && activeNetworkInfo.isConnected() &&
                activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        Log.e(TAG, "Wifi connected: " + isWifiConnection);
        return isWifiConnection;
    }

    private void openDialogSetPassword(){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Set password");

        Context context = getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set up the input
        final EditText editTextPass = new EditText(getContext());
        editTextPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editTextPass.setHint("new password");
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editTextPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        layout.addView(editTextPass);

        // Set up the input
        final EditText editTextRePass = new EditText(getContext());
        editTextRePass.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editTextRePass.setHint("retype password");
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editTextRePass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        layout.addView(editTextRePass);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pass1 = editTextPass.getText().toString();
                String pass2 = editTextRePass.getText().toString();
                if (pass1.equals(pass2)) {
                    setPassword(pass1);
                }
                else {
                    Toast.makeText(getContext()," Error! Passwords are not match", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    public void setPassword(final String newPassword){

        // save password
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(CURRENT_UUID_PASSWORD,newPassword);
        editor.commit();

        credentialsUser();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User password updated.");
                            Toast.makeText(getContext(),"Password has been change", Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(getContext(),task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    public void resetPassword(){

        credentialsUser();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        UuidGenerator uuidGenerator = new UuidGenerator();
        String uuidString = sharedPreferences.getString(CURRENT_UUID_USER, "");
        String email = uuidGenerator.GenerateEmailFromUUID(UUID.fromString(uuidString));

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(getContext(),"Email sent", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    private void makeSureResetDialog (String title,String content) {

        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(content);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        resetPassword();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "no",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        alertDialog.show();
    }

    private void makeSureCleanDialog (String title,String content) {

        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(content);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDataManager.deleteAllMessagesNotInGraphRelation();
                        Toast.makeText(getContext(), "Data was cleaned", Toast.LENGTH_SHORT).show();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "no",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        alertDialog.show();
    }
}
