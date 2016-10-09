package com.kaldapps.macrolensstepper;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;


public class main extends AppCompatActivity {

    private ESPStepper m_stepper;
    private Handler m_handler;
    private int m_numberOfRepetitions = 0;

    public static final String UpdateStringBroadcastMessage = "com.kaldapps.macrolensstepper.UpdatedESPStepper";
    static final String ESP_STEPPER_PARCLE_NAME = "ESP_STEPPER";
    static private int CONFIG_STEPPER_REQUEST_CODE = 1;


    Runnable m_statusChecker = new Runnable() {
        ESPStepper.ESPStepperStatus status;
        @Override
        public void run() {
            try {
                if (m_stepper.hasConnection()) {
                    status = m_stepper.getESPStatus();
                    m_stepper.aSyncGetStatus(getBaseContext());
                    Drawable led;
                    switch (status) {
                        case Ready:
                            led = ContextCompat.getDrawable(getBaseContext(), R.drawable.led_green);
                            break;
                        case NotReady:
                            led = ContextCompat.getDrawable(getBaseContext(), R.drawable.led_red);
                            break;
                        case Stepping:
                            led = ContextCompat.getDrawable(getBaseContext(), R.drawable.led_orange);
                            break;
                        default:
                            led = ContextCompat.getDrawable(getBaseContext(), R.drawable.led_red);
                    }
                    led.setBounds(5,5,5,5);
                    ((ImageView)findViewById(R.id.main_status_ImageView)).setImageDrawable(led);
                }
            } finally {
                m_handler.postDelayed(m_statusChecker,2000);
            }
        }
    };


    Runnable m_stepSetter = new Runnable() {
        @Override
        public void run() {
            int delayTime = 100;
            try {
                delayTime = Integer.parseInt(((EditText)
                        findViewById(R.id.main_time_between_steps)).getText().toString()) * 1000;
                int numberOfSteps = m_stepper.getConvertDistanceInSteps(Float.parseFloat(((
                        EditText) findViewById(R.id.main_distance_to_travel_edit)).getText().toString()));
                // select radio group -> get selected id -> get string value from it
                Boolean travelIn = ((RadioButton) findViewById((
                        (RadioGroup) findViewById(R.id.main_direction_radio_group)).getCheckedRadioButtonId())).getText().toString().equals("IN");
                if (m_stepper.hasConnection()) {
                    m_stepper.moveSteps(numberOfSteps, travelIn, getBaseContext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                m_numberOfRepetitions = 0;
                Toast.makeText(getBaseContext(),
                        "Sending command was in error",Toast.LENGTH_SHORT).show();
            } finally {
                if (m_numberOfRepetitions > 0) {
                    m_numberOfRepetitions--;
                    m_handler.postDelayed(m_stepSetter, delayTime);
                } else {
                    stopStepsCB();
                }

            }
        }
    };

    SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (progress == 100) {
                progress = 99;
            } else if (progress == 0) {
                progress = 1;
            }
            m_stepper.setSpeed((int)Math.round(
                    (ESPStepper.MAX_SPEED - ESPStepper.MIN_SPEED)*(progress/100.0) + ESPStepper.MIN_SPEED));

            // value now holds the decimal value between 0.0 and 10.0 of the progress
            // Example:
            // If the progress changed to 45, value would now hold 4.5
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        m_stepper = new ESPStepper();
        m_handler = new Handler();
        ((SeekBar)findViewById(R.id.main_speed_bar)).setOnSeekBarChangeListener(listener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        startCheckingStatus();
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopCheckingStatus();
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
        intent.putExtra(ESP_STEPPER_PARCLE_NAME, m_stepper);
        startActivityForResult(intent, CONFIG_STEPPER_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == CONFIG_STEPPER_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
              m_stepper = data.getParcelableExtra(ESP_STEPPER_PARCLE_NAME);
            }
        }
    }


    private void startCheckingStatus() {
        m_statusChecker.run();
    }


    private void stopCheckingStatus() {
        m_handler.removeCallbacks(m_statusChecker);
    }


    public void startSteps(View view) {
        if (((Button)view).getText().toString().equals("Start")) {
            ((Button)findViewById(R.id.main_start)).setText(getResources().getString(R.string.main_stop_button));
            // disable radio group
            enableMainWindow(false);
            m_stepper.sendSpeed();
            m_numberOfRepetitions = Integer.parseInt(
                    ((EditText)findViewById(R.id.number_of_repetitions_edit)).getText().toString());
            // m_stepSetter.run();
        } else if ((((Button)view).getText().toString().equals("Stop"))) {
            stopStepsCB();
        }
    }


    private void stopStepsCB() {
        ((Button)findViewById(R.id.main_start)).setText(getResources().getString(R.string.main_start_button));
        // disable radio group
        enableMainWindow(true);
        m_handler.removeCallbacks(m_stepSetter);
    }


    private void enableMainWindow(Boolean i_enable) {
        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.main_direction_radio_group);
        radioGroup.setEnabled(i_enable);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(i_enable);
        }
        findViewById(R.id.main_configure_button).setEnabled(i_enable);
        findViewById(R.id.main_speed_bar).setEnabled(i_enable);
        findViewById(R.id.number_of_repetitions_edit).setEnabled(i_enable);
        findViewById(R.id.main_time_between_steps).setEnabled(i_enable);
        findViewById(R.id.main_distance_to_travel_edit).setEnabled(i_enable);

    }
}
