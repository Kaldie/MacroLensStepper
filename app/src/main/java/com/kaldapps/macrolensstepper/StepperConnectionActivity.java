package com.kaldapps.macrolensstepper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class StepperConnectionActivity extends AppCompatActivity {
    final static private String TAG = "StepperConnectActivity";
    private boolean m_espConnectionVerified = false;
    private boolean m_normalConnectionVerified = false;
    private boolean m_hasStepperConnection = false;
    private boolean m_hadSucces = false;
    private WifiHelper m_wifiHelper;
    private ESPStepper m_stepper;

    // listener who checks if any wifi activity has been seen
    private BroadcastReceiver onWifiChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Network Receiver received!");
            final String action = intent.getAction();
            if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    Log.d(TAG,info.getDetailedState().toString());

                    // if we connected to something, check if its one of the AP indicated by the spinners
                    if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                        Log.d(TAG, "connected to network");
                        String currentSSID = m_wifiHelper.getCurrentConnectionSSID();
                        if (!m_espConnectionVerified && currentSSID.equals(
                                ((Spinner) findViewById(R.id.EspAP)).getSelectedItem().toString())) {
                            m_espConnectionVerified = true;
                            Log.d(StepperConnectionActivity.TAG,
                                    "Established that ESP wifi can be connected");
                            m_hadSucces = true;
                            connectStepperToAP(null);

                        } else if (currentSSID.equals(
                                ((Spinner) findViewById(R.id.NormalAP)).getSelectedItem().toString())) {
                            m_normalConnectionVerified = true;
                            Log.d(StepperConnectionActivity.TAG,
                                    "Established that normal wifi can be connected");
                            m_hadSucces = true;
                            connectStepperToAP(null);
                        }
                    }
                }
                Log.d(StepperConnectionActivity.TAG, "Wifi is not connected");
            }
        }
    };

    // listener who check the stepper activity
    private BroadcastReceiver stepperBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            m_hasStepperConnection = m_stepper.hasConnection();
            if (m_stepper.hasConnection()){
                connectStepperToAP(null);
            } else {
                Toast.makeText(context,"Could not connect to the stepper. " +
                        "Please select the correct AP for the stepper",Toast.LENGTH_SHORT).show();
            }
        }
    };


    // listener who checks the activity on our spinners
    private AdapterView.OnItemSelectedListener spinnerOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            Spinner currentSpinner = (Spinner) parent;
            EditText correspondingEdit;
            if (currentSpinner.equals(findViewById(R.id.EspAP))) {
                correspondingEdit = (EditText)findViewById(R.id.EspPasswordEditText);
            } else if (currentSpinner.equals(findViewById(R.id.NormalAP))) {
                correspondingEdit = (EditText)findViewById(R.id.WifiPasswordEditText);
            } else {
                correspondingEdit = null;
            }
            String SSID = currentSpinner.getSelectedItem().toString();
            WifiConfiguration config =
                    m_wifiHelper.getWifiConfigForSSID(WifiHelper.convertStringToValidSSID(SSID));
            Boolean hasPassword = WifiHelper.hasPassword(config);

            Log.d(TAG, "SSID: " + SSID + " has password: " + hasPassword.toString());
            if (hasPassword) {
                correspondingEdit.setEnabled(true);
            } else {
                correspondingEdit.setEnabled(false);
            }
        }
        // is an abstract method from the derived class, this needs to be implemented
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stepper_connection);
        m_wifiHelper = new WifiHelper((WifiManager) getSystemService(Context.WIFI_SERVICE));
        m_stepper = new ESPStepper();

        Spinner spinner = (Spinner) findViewById(R.id.EspAP);
        initialiseSpinner(spinner);
        spinner = (Spinner) findViewById(R.id.NormalAP);
        initialiseSpinner(spinner);
        findViewById(R.id.EspPasswordEditText).setEnabled(false);
        findViewById(R.id.WifiPasswordEditText).setEnabled(false);
    }


    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(onWifiChanged, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(stepperBroadcastReceiver,
                new IntentFilter(ESPStepper.m_UPDATE_STEPPER_INTENT));
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(onWifiChanged);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepperBroadcastReceiver);
        super.onPause();
    }


    // method that checks an ap and password, but is a bit carefull sometimes
    // if the AP does not require a password, it returns true
    // use or operation to do blaaa....
    private Boolean checkAnAP(String i_SSID, String i_password) {
        Log.d(TAG,"checkAnAp");
        i_SSID = WifiHelper.convertStringToValidSSID(i_SSID);
        WifiConfiguration config = m_wifiHelper.getWifiConfigForSSID(i_SSID);
        // if we can find the config, and it has no password, we're good
        if (!WifiHelper.hasPassword(config)) {
            return true;
        } else {
            // if the password is not set, and we need it....
            if (i_password.length() == 0) {
                Toast.makeText(this,"Please set the password for the home wifi.",Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        // Updating the password as in the edit text and connecting to the normal AP
        Toast.makeText(this,"Verifying the connection",Toast.LENGTH_SHORT).show();
        config.preSharedKey = "\"" + i_password + "\"";
        m_wifiHelper.updateConfiguration(config);
        // force the connection and see if it works out fine.
        // after the wifi settles and it has a connection this function is called again
        m_wifiHelper.forceConnection(config);
        return false;
    }

    public void connectStepperToAP(View view) {
        Log.d(TAG,"connectStepperToAP");

        // verify the normal connection
        if (!m_normalConnectionVerified) {
            Log.d(TAG,"Check Normal connection");
            String ssid = ((Spinner) findViewById(R.id.NormalAP)).getSelectedItem().toString();
            String password = ((EditText)findViewById(R.id.WifiPasswordEditText)).getText().toString();
            m_normalConnectionVerified |= checkAnAP(ssid, password);
            if (!m_normalConnectionVerified) {
                return;
            }
        }

        // verify the esp connection
        if (!m_espConnectionVerified) {
            Log.d(TAG,"Check ESP connection");
            String ssid = ((Spinner) findViewById(R.id.EspAP)).getSelectedItem().toString();
            String password = ((EditText)findViewById(R.id.EspPasswordEditText)).getText().toString();
            m_espConnectionVerified |= checkAnAP(ssid, password);
            if (!m_espConnectionVerified) {
                return;
            }
        }

        // verify the stepper connection
        if (!m_hasStepperConnection) {
            m_stepper.attemptConnection(m_wifiHelper, this);
            Toast.makeText(this,
                    "Establishing contact with the stepper.",Toast.LENGTH_SHORT).show();
            return;
        }

        // if the current AP is still esp AP,  set the ap of the stepper to wifi.
        if (m_wifiHelper.getCurrentConnectionSSID().equals(
                ((Spinner) findViewById(R.id.EspAP)).getSelectedItem().toString())) {
            String ssid = ((Spinner) findViewById(R.id.NormalAP)).getSelectedItem().toString();
            String password = ((EditText) findViewById(R.id.WifiPasswordEditText)).getText().toString();
            m_stepper.connectToAP(this, ssid, password);
        }
    }

    private void initialiseSpinner(Spinner i_spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, m_wifiHelper.getScannedWifiAPs());
        i_spinner.setAdapter(adapter);
        i_spinner.setOnItemSelectedListener(spinnerOnItemSelectedListener);
    }
}


