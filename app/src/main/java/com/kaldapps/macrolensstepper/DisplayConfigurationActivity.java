package com.kaldapps.macrolensstepper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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


public class DisplayConfigurationActivity extends AppCompatActivity {

    WifiHelper m_wifiHelper;
    ESPStepper m_stepper;

    private BroadcastReceiver m_requestUpdateUIReceiver = new BroadcastReceiver() {
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


    private BroadcastReceiver m_passwordDialogReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ap = intent.getStringExtra("AP");
            String password = intent.getStringExtra("Password");
            m_wifiHelper.acceptPassword(ap,password);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_configuration);

        m_wifiHelper = new WifiHelper((WifiManager) getSystemService(Context.WIFI_SERVICE));
        m_stepper = getIntent().getParcelableExtra(main.ESP_STEPPER_PARCLE_NAME);
        initialiseSpinner();
    }


    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_requestUpdateUIReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_passwordDialogReceived);
        super.onPause();
    }

    @Override
    public void onResume() {
        // create the local broadcast manager to do stuff later
        LocalBroadcastManager.getInstance(this).registerReceiver(m_requestUpdateUIReceiver,
                new IntentFilter(ESPStepper.UPDATE_STEPPER_INTENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(m_passwordDialogReceived,
                new IntentFilter(PasswordDialogFragment.PASSWORD_RECEIVED_INTENT_TAG));
        super.onResume();
    }


    private void updateUI() {
        TextView x = ((TextView) findViewById(R.id.status_bar));
        x.setText(String.format("Established connection: %b", m_stepper.hasConnection()));
        TextView ipEdit = (TextView) findViewById(R.id.ip_address_text_view);
        ipEdit.setText(m_stepper.ipAddress());

    }


    void initialiseSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.normal_ap_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, m_wifiHelper.getScannedWifiAPs());
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                establishWifiConnection();
            }
            // is an abstract method from the derived class, this needs to be implemented
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    public void connectToStepper(View view) {
        // get the selected AP
        String selectedAP =
                ((Spinner) findViewById(R.id.normal_ap_spinner)).getSelectedItem().toString();
        // if the AP is equal to the one we are currently connected to
        if (m_wifiHelper.getCurrentConnectionSSID().equals(
                WifiHelper.convertStringToValidSSID(selectedAP))) {
            // attempt a connection to the stepper
            // this will trigger a series of upd conversations.
            // they will update the m_stepper variable and trigger an ui update broadcast
            m_stepper.attemptConnection(m_wifiHelper, this);
            // let the user know that we are doing something
            Toast.makeText(this,
                    "Connecting to Stepper.", Toast.LENGTH_SHORT).show();
            return;
        }
        // we are not connected to the user a-pointed AP.
        // Connect to this and let the user try again in a sec
        Toast.makeText(this,
                "Connecting to selected AP. Retry in a moment", Toast.LENGTH_SHORT).show();
        m_wifiHelper.forceConnection(
                m_wifiHelper.getWifiConfigForSSID(WifiHelper.convertStringToValidSSID(selectedAP)));
    }


    public void connectStepperToWifi(View view) {
        Intent connectStepperToWifiIntent= new Intent(this,StepperConnectionActivity.class);
        startActivity(connectStepperToWifiIntent);
    }

    public void getStatus(View view) {
        m_stepper.aSyncGetStatus(this);
    }


    public void configStepSize(View view) {
        String numberString =
                ((EditText)findViewById(R.id.number_of_steps_config_edit)).getText().toString();
        int numberOfSteps=0;
        double milimeters=0.0;
        if (numberString.length()>0) {
            numberOfSteps = Integer.parseInt(numberString);
        }
        numberString =
                ((EditText)findViewById(R.id.number_of_steps_config_edit)).getText().toString();
        if (numberString.length()>0) {
            milimeters = Double.parseDouble(numberString);
        }
        if (numberOfSteps > 0 && milimeters > 0) {
            m_stepper.setMmPerStep(milimeters/numberOfSteps);
            Intent returnIntent = new Intent();
            returnIntent.putExtra(main.ESP_STEPPER_PARCLE_NAME, m_stepper);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } else {
            Toast.makeText(this,"Please set the number of steps and the resulting distance!",
                    Toast.LENGTH_SHORT).show();
        }
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



    private void establishWifiConnection() {
        String normalAP = WifiHelper.convertStringToValidSSID(
                ((Spinner) findViewById(R.id.normal_ap_spinner)).getSelectedItem().toString());

        // if we are connected to the AP we are done
        if (normalAP.equals(m_wifiHelper.getCurrentConnectionSSID())) {
            System.out.println("Currently connected to the selected AP. SOOOOOOOO we are done! " +
                    "We even know that it works");
            return;
        }
        // attempt to get the config from the known ones
        WifiConfiguration configuration = m_wifiHelper.getWifiConfigForSSID(
                WifiHelper.convertStringToValidSSID(normalAP));
        // if we need a password, let it be provided by the user
        if (m_wifiHelper.hasPassword(configuration) && !m_wifiHelper.isPasswordKnown(configuration)) {
            createPasswordDialog(normalAP);
        }
    }


    private void createPasswordDialog(String i_SSID) {
        PasswordDialogFragment newPasswordFragment = PasswordDialogFragment.newInstance(i_SSID);
        newPasswordFragment.show(getFragmentManager(), "dialog");
    }
}
