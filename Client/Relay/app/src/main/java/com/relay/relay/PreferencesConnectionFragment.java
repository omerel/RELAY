package com.relay.relay;

/**
 * Created by omer on 17/03/2017.
 */

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

public class PreferencesConnectionFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    SwitchPreferenceCompat switchPreferenceCompat;
    private SharedPreferences sharedPreferences;
    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        updateSwitchState();
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.app_preferences_connection);
        sharedPreferences = getActivity().getSharedPreferences(MainActivity.SYSTEM_SETTING, 0);
        // update default preference
        updateSwitchState();
    }

    private void updateSwitchState(){

        // set up bluetooth switch
        boolean bool = sharedPreferences.getBoolean(getString(R.string.key_enable_bluetooth),false);
        switchPreferenceCompat = (SwitchPreferenceCompat) getPreferenceManager().findPreference(getString(R.string.key_enable_bluetooth));
        // check if bluetooth didn't shot off
        if (BluetoothAdapter.getDefaultAdapter().isEnabled())
            switchPreferenceCompat.setChecked(bool);
        else
            switchPreferenceCompat.setChecked(false);

        // set up wifi switch
        bool = sharedPreferences.getBoolean(getString(R.string.key_enable_wifi),false);
        switchPreferenceCompat = (SwitchPreferenceCompat)getPreferenceManager().findPreference(getString(R.string.key_enable_wifi));
        // check if wifi didn't shot off
        if ( ((WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled() )
            switchPreferenceCompat.setChecked(bool);
        else
            switchPreferenceCompat.setChecked(false);

        // set up mobile data switch
        bool = sharedPreferences.getBoolean(getString(R.string.key_enable_data),false);
        switchPreferenceCompat = (SwitchPreferenceCompat)getPreferenceManager().findPreference(getString(R.string.key_enable_data));
        switchPreferenceCompat.setChecked(bool);
    }
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // convert to switchPreferenceCompat
        SwitchPreferenceCompat switchPreferenceCompat = (SwitchPreferenceCompat) preference;
        changeStatusIfClicked(switchPreferenceCompat);
        return super.onPreferenceTreeClick(preference);
    }


    public void changeStatusIfClicked(SwitchPreferenceCompat switchPreferenceCompat){
        String key = switchPreferenceCompat.getKey();
        boolean changePririty = false;
        String mode = key;

        // if Bluetooth pressed
        if (key.equals(getString(R.string.key_enable_bluetooth)) &&
                BluetoothAdapter.getDefaultAdapter().isEnabled()){
            // change state only if bluetooth is enable
            changePririty = true;
        }
        // if Wifi pressed
        if (key.equals(getString(R.string.key_enable_wifi)) &&
                ((WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled() ){
            changePririty = true;
        }
        // if Mobile data pressed
        if (key.equals(getString(R.string.key_enable_data))){
            // TODO
        }

        if (changePririty){
            // saving state into sharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(switchPreferenceCompat.getKey(),switchPreferenceCompat.isChecked());
            editor.commit();
            // update activity
            onButtonPressed(MainActivity.CHANGE_PRIORITY_F);
        }
        else{
            switchPreferenceCompat.setChecked(false);
            // Alert user
            Snackbar.make(this.getView(), " Please Turn "+mode+" first " , Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        }

    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String string);
    }

    public void onButtonPressed(String string) {
        if (mListener != null) {
            mListener.onFragmentInteraction(string);
        }
    }

}


