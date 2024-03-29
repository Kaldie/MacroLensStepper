package com.kaldapps.macrolensstepper;



import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


class ESPStepper implements Parcelable {
    private static final String TAG = "ESPStepper";
    private static final String m_ESP_UPD_MESSAGE = "Are You Espressif IOT Smart Device?";
    private static final int m_UDP_PORT = 1025;
    public static final int MAX_SPEED = 10;
    public static final int MIN_SPEED = 1000;
    private String m_connectedAP = "unknown";
    private String m_currentIP = "unknown";
    private double m_mmPerStep = 0;
    private int m_speed = 1000;

    private ConnectionStatus m_connectionStatus = ConnectionStatus.Unknown;
    private StepperStatus m_stepperStatus = StepperStatus.Unknown;

    static final String UPDATE_STEPPER_INTENT = "update_stepper_intent";




    enum ConnectionStatus {
        Idle, GotIP, Connecting, NoApFound, WrongPassword, ConnectFail, Unknown}
    enum StepperStatus {
        Initalisation, Stepping, Waiting, Ready, Unknown}
    enum ESPStepperStatus {
        Ready, Stepping, NotReady, Unknown}

    // default constructor
    ESPStepper() {
    }


    ESPStepper(Parcel i_parcel) {
        m_connectedAP = i_parcel.readString();
        m_currentIP = i_parcel.readString();
        m_mmPerStep = i_parcel.readDouble();
        m_connectionStatus = ConnectionStatus.valueOf(i_parcel.readString());
        m_stepperStatus = StepperStatus.valueOf(i_parcel.readString());
    }


    public static final Parcelable.Creator<ESPStepper> CREATOR = new Parcelable.Creator<ESPStepper>() {
        public ESPStepper createFromParcel(Parcel pc) {
            return new ESPStepper(pc);
        }
        public ESPStepper[] newArray(int size) {
            return new ESPStepper[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel i_parcel, int i_flags) {
        i_parcel.writeString(m_connectedAP);
        i_parcel.writeString(m_currentIP);
        i_parcel.writeDouble(m_mmPerStep);
        i_parcel.writeString(m_connectionStatus.name());
        i_parcel.writeString(m_stepperStatus.name());
    }

    public String connectedAP() {
        return this.m_connectedAP;
    }


    public void setMmPerStep(double i_mmPerStep) {
        m_mmPerStep = i_mmPerStep;
    }

    public double mmPerStep() {
        return m_mmPerStep;
    }

    public void setCurrentIP(String i_ip) throws IllegalArgumentException {
        if (validIP(i_ip)) {
            m_currentIP = i_ip;
        } else {
            throw new IllegalArgumentException("IP address is not a valid address!");
        }
    }

    void setSpeed(int speed) {
        this.m_speed = speed;
    }

    int speed() {
        return m_speed;
    }


    String ipAddress() {
        if (m_currentIP.contains("Unknown")) {
            return "";
        }
        return m_currentIP;
    }


    Boolean hasConnection() {
        if (m_connectionStatus == ConnectionStatus.Idle ||
                m_connectionStatus == ConnectionStatus.GotIP) {
            return true;
        } else {
            return false;
        }
    }

    public ESPStepperStatus getESPStatus() {
        if (!hasConnection()) {
            return ESPStepperStatus.NotReady;
        }

        switch (m_stepperStatus) {
            case Unknown :
                return ESPStepperStatus.NotReady;
            case Ready:
            case Waiting:
                return ESPStepperStatus.Ready;
            case Initalisation:
            case Stepping:
                return ESPStepperStatus.Stepping;
            default:
                return ESPStepperStatus.Unknown;
        }
    }

    private static boolean validIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            return !ip.endsWith(".");

        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    Boolean attemptConnection(WifiHelper i_helper, Context i_context) {
        // shout an udp packet over the connection
        if (!i_helper.isConnected()) {
            return false;
        }
        aSyncAttemptConnection(i_helper, i_context);
        return true;
    }


    private boolean parseUDPresponse(String i_response) {
        if (i_response.equals("")) {
            return false;
        }
        String[] splits = i_response.split(" ");
        if (splits.length != 3) return false;

        System.out.println("IP address guess is: " + splits[2] + " Which is a valid ip adres: "
                + validIP(splits[2].replaceAll("[^\\d.]", "")));
        m_currentIP = splits[2].replaceAll("[^\\d.]", "");
        return validIP(m_currentIP);
    }


    @SuppressWarnings("unchecked")
    private void aSyncAttemptConnection(final WifiHelper i_helper, final Context i_context) {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                i_helper.sendUDPMessage(m_ESP_UPD_MESSAGE, m_UDP_PORT);
                String udpResponse = i_helper.receiveUDPMessage(m_UDP_PORT, 50);
                System.out.println("UDP resonse was: " + udpResponse);
                if(parseUDPresponse(udpResponse)) {
                    getConnectionStatus();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Intent intent = new Intent(UPDATE_STEPPER_INTENT);
                intent.putExtra(main.UpdateStringBroadcastMessage, true);
                m_connectedAP = i_helper.getCurrentConnectionSSID();
                LocalBroadcastManager.getInstance(i_context).sendBroadcast(intent);
            }
        }.execute();
    }


    private void getConnectionStatus() {
        ESPStepper.ConnectionStatus espStatus = ESPStepper.ConnectionStatus.Unknown;
        try {
            JSONObject jObj = WifiHelper.getJson(ipAddress(), "client?command=status");
            String statusString = "";
            if (jObj != null) {
                statusString = jObj.getJSONObject("Status").getString("status");
            }
            if (statusString.equals("AP found and connected")) {
                espStatus = ESPStepper.ConnectionStatus.GotIP;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        m_connectionStatus = espStatus;
        System.out.println("Connection status is: " + m_connectionStatus.toString());
    }


    private void getStepperStatus() {
        StepperStatus espStatus = StepperStatus.Unknown;
        try {
            JSONObject jObj = WifiHelper.getJson(ipAddress(), "config?command=stepper");
            if (jObj != null) {
                // get the status of the stepper
                String statusString = jObj.getJSONObject("Stepper").
                        getJSONObject("Stepper_Config").getString("status");
                switch (statusString) {
                    case "Waiting" :
                        espStatus = StepperStatus.Waiting;
                        break;
                    case "Initalisation" :
                    espStatus = StepperStatus.Initalisation;
                        break;
                    case "Stepping" :
                    espStatus = StepperStatus.Stepping;
                        break;
                    case "Ready" :
                    espStatus = StepperStatus.Ready;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        m_stepperStatus = espStatus;
        System.out.println("Stepper status is: " + m_stepperStatus.toString());
    }


    @SuppressWarnings("unchecked")
    void aSyncGetStatus(final Context i_context) {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                getConnectionStatus();
                if (m_connectionStatus != ConnectionStatus.Unknown) {
                    getStepperStatus();
                }
                return "";
            }

            @Override
            protected void onPostExecute(Object i_object) {
                Intent intent = new Intent(UPDATE_STEPPER_INTENT);
                intent.putExtra(main.UpdateStringBroadcastMessage, true);
                LocalBroadcastManager.getInstance(i_context).sendBroadcast(intent);
            }
        }.execute();

    }


    @SuppressWarnings("unchecked")
    void sendSpeed() {
        final JSONObject outerObject;
        try {
            outerObject = new JSONObject().put(
                    "Stepper", new JSONObject().put(
                            "Stepper_Config", new JSONObject().put("step_interval", m_speed)));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] obj) {
                JSONObject firstObject = (JSONObject)obj[0];
                Log.d(TAG,"send speed json object is:" + firstObject.toString());
                //WifiHelper.sendJson(firstObject, m_currentIP, "config?command=stepper");
                return "";
            }
        }.execute(outerObject);
    }


    @SuppressWarnings("unchecked")
    void moveSteps(int numberOfSteps, boolean i_out, Context i_context) {
        if(!hasConnection()) {
            Toast.makeText(i_context, "Do not have a valid connection!",Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject innerObject = new JSONObject();
        JSONObject outerObject = new JSONObject();
        JSONObject readyObject = new JSONObject();
        try {
            innerObject.put("steps_to_set", numberOfSteps);
            if (i_out) {
                innerObject.put("step_forward", "True");
            } else {
                innerObject.put("step_forward", "False");
            }
            outerObject.put("Stepper_Step", innerObject);

            readyObject.put("Stepper_Config", new JSONObject().put("status","Ready"));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] obj) {
                JSONObject firstObject = (JSONObject)obj[0];
                JSONObject secondObject = (JSONObject)obj[1];

                WifiHelper.sendJson(firstObject, m_currentIP, "config?command=stepper");
                WifiHelper.sendJson(secondObject, m_currentIP, "config?command=stepper");
                return "";
            }
        }.execute(outerObject, readyObject);
    }


    @SuppressWarnings("unchecked")
    void connectToAP(Context i_context, String i_ssid, String i_password) {
        Log.d(TAG, "Some1 wanted to make u connect to another AP");
        if(!hasConnection()) {
            Toast.makeText(i_context, "Do not have a valid connection!",Toast.LENGTH_SHORT).show();
            return;
        }
        String token = "1234567890123456789012345678901234567890";
        JSONObject outerObject = new JSONObject();
        JSONObject commandObject = new JSONObject();
        try {
            commandObject.put("SSID", i_ssid);
            commandObject.put("password", i_password);
            commandObject.put("token", token);
            outerObject.put("Request",
                    new JSONObject().put("Station",
                            new JSONObject().put("Connect_Station", commandObject)));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] obj) {
                JSONObject firstObject = (JSONObject)obj[0];
                Log.d(TAG,firstObject.toString());
                //WifiHelper.sendJson(firstObject, m_currentIP, "config?command=wifi");
                //WifiHelper.getJson(m_currentIP,"config?command=reboot");
                return "";
            }
        }.execute(outerObject);
    }

    public int getConvertDistanceInSteps(float i_distance) {
        return (int)Math.round(i_distance/m_mmPerStep);
    }
}
