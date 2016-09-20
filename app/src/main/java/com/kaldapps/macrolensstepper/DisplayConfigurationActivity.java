package com.kaldapps.macrolensstepper;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;


import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

public class DisplayConfigurationActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    WifiHelper m_wifiHelper;
    WifiConfiguration m_espConfiguration;
    WifiConfiguration m_normalConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_wifiHelper = new WifiHelper((WifiManager) getSystemService(Context.WIFI_SERVICE));
        setContentView(R.layout.activity_display_configuration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        m_espConfiguration = new WifiConfiguration();
        m_normalConfiguration = new WifiConfiguration();
        // get the wifi BSSD
        populateSpinners();
        Spinner espSpinner = (Spinner) findViewById(R.id.esp_ap_spinner);
        Spinner normal_ap_spinner = (Spinner) findViewById(R.id.normal_ap_spinner);
        espSpinner.setOnItemSelectedListener(this);
        normal_ap_spinner.setOnItemSelectedListener(this);
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
        AppIndex.AppIndexApi.start(client, viewAction);
    }


    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
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

        m_wifiHelper.restoreOldConnection();

        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    private void populateSpinners() {
        Spinner espBSSDSpinner = (Spinner) findViewById(R.id.esp_ap_spinner);
        Spinner normalBSSDSpinner = (Spinner) findViewById(R.id.normal_ap_spinner);
        // APs for esp

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, m_wifiHelper.getScannedWifiAPs(false));
        espBSSDSpinner.setAdapter(adapter);
        // APs for normal use

        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, m_wifiHelper.getScannedWifiAPs(true));
        normalBSSDSpinner.setAdapter(adapter);
    }


    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        Spinner espSpinner = (Spinner) findViewById(R.id.esp_ap_spinner);
        Spinner normal_ap_spinner = (Spinner) findViewById(R.id.normal_ap_spinner);
        if (parent.equals(espSpinner)) {
            establishESPWifiConfiguration();
            System.out.println("ESP spinner is selected");
        } else if (parent.equals(normal_ap_spinner)) {
            establishNormalWifiConfiguration();
            System.out.println("normal spinner is selected");
        }
    }

    // is an abstract method from the derived class, this needs to be implemented
    public void onNothingSelected(AdapterView<?> parent) {
    }


    public void configureConnectionOnClick(View view) {
        // this method will connect to the esp, check which connection it has
        // then configure it such that it will connect to the given Normal AP

        // first, check if the esp is already on the normal AP
        boolean isCorrect = m_wifiHelper.forceConnection(m_espConfiguration);
        if (isCorrect)
        {Toast.makeText(this, "Esp connection is active!", Toast.LENGTH_SHORT).show();}
        // secondly check the normal connection
        isCorrect &= m_wifiHelper.forceConnection(m_normalConfiguration);
        if (isCorrect) Toast.makeText(this, "Normal connection is active!", Toast.LENGTH_SHORT).show();

    }


    private void establishNormalWifiConfiguration() {
        String normalAP =
                ((Spinner) findViewById(R.id.normal_ap_spinner)).getSelectedItem().toString();
        m_normalConfiguration = m_wifiHelper.getWifiConfigForSSID(
                WifiHelper.convertStringToValidSSID(normalAP));
        if (!m_wifiHelper.isPasswordKnown(m_normalConfiguration)) {
            createPasswordDialog(normalAP);
        }
    }


    private void establishESPWifiConfiguration() {
        String espAPString =
                ((Spinner) findViewById(R.id.esp_ap_spinner)).getSelectedItem().toString();
        m_espConfiguration = m_wifiHelper.getWifiConfigForSSID(
                WifiHelper.convertStringToValidSSID(espAPString));
        if (!m_wifiHelper.isPasswordKnown(m_espConfiguration)) {
            createPasswordDialog(espAPString);
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
            if (i_SSID.compareTo(m_espConfiguration.SSID) == 0) {
                m_espConfiguration.preSharedKey = "\"" + i_password + "\"";
                wifiConfiguration = m_espConfiguration;
            } else if (i_SSID.compareTo(m_normalConfiguration.SSID) == 0) {
                m_normalConfiguration.preSharedKey = "\"" + i_password + "\"";
                wifiConfiguration = m_normalConfiguration;
            } else {
                System.out.println("Trying to set a password of an unknown SSID!");
            }
            if (wifiConfiguration != null) {
                m_wifiHelper.updateConfiguration(wifiConfiguration);
            }
        }
    }
}
