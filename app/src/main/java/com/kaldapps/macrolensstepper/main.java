package com.kaldapps.macrolensstepper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;


public class main extends AppCompatActivity {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private WifiHelper m_wifiHelper;
    private ESPStepper m_stepper;
    public static final String UpdateStringBroadcastMessage = "com.kaldapps.macrolensstepper.UpdatedESPStepper";
    static final String ESP_STEPPER_PARCLE_NAME = "ESP_STEPPER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        m_wifiHelper = new WifiHelper((WifiManager) getSystemService(Context.WIFI_SERVICE));
        m_stepper = new ESPStepper();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startConfiguration(View view) {
        // start the configuration of the stepper stuff
        Intent intent = new Intent(this, DisplayConfigurationActivity.class);
        intent.putExtra(ESP_STEPPER_PARCLE_NAME,m_stepper);
        startActivity(intent);
    }
}
