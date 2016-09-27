package com.kaldapps.macrolensstepper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;

public class DisplayConfigurationActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    WifiHelper m_wifiHelper;
    WifiConfiguration m_wifiConfiguration;
    ESPStepper m_stepper;

    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Updating DisplayConfigurationActivity ui: " +
                    intent.getBooleanExtra(main.UpdateStringBroadcastMessage, false));
            if (!intent.getBooleanExtra(main.UpdateStringBroadcastMessage, false))
                return;
            // Get extra data included in the Intent
            updateUI();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_configuration);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        m_wifiHelper = new WifiHelper((WifiManager) getSystemService(Context.WIFI_SERVICE));
        m_wifiConfiguration = new WifiConfiguration();

        m_stepper = getIntent().getParcelableExtra("ESPStepper");

        // get the wifi BSSD
        Spinner normalSpinner = (Spinner) findViewById(R.id.normal_ap_spinner);
        populateSpinner(normalSpinner);
        normalSpinner.setOnItemSelectedListener(this);
    }


    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DisplayConfiguration Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.kaldapps.macrolensstepper/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        // create the local broadcast manager to do stuff later
        LocalBroadcastManager.getInstance(this).registerReceiver(m_broadcastReceiver,
                new IntentFilter(ESPStepper.m_UPDATE_STEPPER_INTENT));
        super.onResume();
    }


    private void updateUI() {
        TextView x = ((TextView) findViewById(R.id.status_bar));
        x.setText(String.format("Established connection: %b", m_stepper.hasConnection()));
        EditText ipEdit = (EditText) findViewById(R.id.ip_adres_edit);
        ipEdit.setText(m_stepper.ipAddress());

    }


    private void populateSpinner(Spinner i_spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, m_wifiHelper.getScannedWifiAPs());
        i_spinner.setAdapter(adapter);
    }


    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        establishWifiConfiguration();
    }

    // is an abstract method from the derived class, this needs to be implemented
    public void onNothingSelected(AdapterView<?> parent) {
    }


    public void connectToStepper(View view) {
        String selectedAP =
                ((Spinner) findViewById(R.id.normal_ap_spinner)).getSelectedItem().toString();
        if (m_wifiHelper.getCurrentConnectionSSID().equals(
                WifiHelper.convertStringToValidSSID(selectedAP))) {
            m_stepper.attemptConnection(m_wifiHelper, this);
            Toast.makeText(this,
                    "Connecting to Stepper.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,
                "Connecting to selected AP. Retry in a moment", Toast.LENGTH_SHORT).show();
        m_wifiHelper.forceConnection(
                m_wifiHelper.getWifiConfigForSSID(WifiHelper.convertStringToValidSSID(selectedAP)));
    }


    public void connectStepperToWifi(View view) {
        ArrayList<String> scannedAP = m_wifiHelper.getScannedWifiAPs();
        String espAP = scannedAP.get(1);
        if (!espAP.contains("ESP")) {
            Toast.makeText(this,"Cannot find the ESP stepper. Please check if its turned on",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!espAP.equals(m_wifiHelper.getCurrentConnectionSSID())) {
            WifiConfiguration espConfiguration = m_wifiHelper.getWifiConfigForSSID(espAP);
            if (!m_wifiHelper.isPasswordKnown(espConfiguration)) {
                createPasswordDialog(espConfiguration.SSID);
                Toast.makeText(this,
                        "Enter password and retry", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void getStatus(View view) {
        m_stepper.aSyncGetStatus(this);
    }

    public void go_in_button_clicked(View view) {
        move_steps(false);
    }


    public void go_out_button_clicked(View view) {
        move_steps(true);
    }


    private void move_steps(boolean i_out){
        int numberOfSteps = Integer.parseInt((
                (EditText)findViewById(R.id.number_of_steps_config_edit)).getText().toString());
        m_stepper.moveSteps(numberOfSteps,i_out, this);
    }



    private void establishWifiConfiguration() {
        String normalAP = WifiHelper.convertStringToValidSSID(
                ((Spinner) findViewById(R.id.normal_ap_spinner)).getSelectedItem().toString());

        // if we are connected to the AP we are done
        if (normalAP.equals(m_wifiHelper.getCurrentConnectionSSID())) {
            System.out.println("Currently connected to the selected AP. SOOOOOOOO we are done! " +
                    "We even know that it works");
            return;
        }
        // attempt to get the config from the known ones
        m_wifiConfiguration = m_wifiHelper.getWifiConfigForSSID(
                WifiHelper.convertStringToValidSSID(normalAP));
        // if we need a password, let it be provided by the user
        if (!m_wifiHelper.isPasswordKnown(m_wifiConfiguration)) {
            createPasswordDialog(normalAP);
        }
    }


    private void createPasswordDialog(String i_SSID) {
        PasswordDialogFragment newPasswordFragment = PasswordDialogFragment.newInstance(i_SSID);
        newPasswordFragment.show(getFragmentManager(), "dialog");
    }


    public void acceptPassword(String i_SSID, String i_password) {
        if (i_password != null && !i_password.isEmpty() &&
                i_SSID != null && !i_SSID.isEmpty()) {
            i_SSID = WifiHelper.convertStringToValidSSID(i_SSID);
            System.out.println("Password '" + i_password + "' is selected for ssid: " + i_SSID + " !");
            WifiConfiguration wifiConfiguration = null;
            if (i_SSID.compareTo(m_wifiConfiguration.SSID) == 0) {
                m_wifiConfiguration.preSharedKey = "\"" + i_password + "\"";
                wifiConfiguration = m_wifiConfiguration;
            } else {
                System.out.println("Trying to set a password of an unknown SSID!");
            }
            if (wifiConfiguration != null) {
                m_wifiHelper.updateConfiguration(wifiConfiguration);
                m_wifiHelper.forceConnection(wifiConfiguration);
            }
        }
    }


}
