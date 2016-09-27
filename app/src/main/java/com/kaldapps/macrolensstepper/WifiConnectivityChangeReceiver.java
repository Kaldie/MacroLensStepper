package com.kaldapps.macrolensstepper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.HashMap;

/**
 * Created by ruud on 20-9-16.
 */

public class WifiConnectivityChangeReceiver extends BroadcastReceiver {
    public HashMap<String,Boolean> m_wifiList = new HashMap<>();
    public boolean m_hasValidConnection = false;

    public void onReceive(final Context i_context,
                          final Intent i_intent) {
        System.out.println(i_intent.toString());
        if (i_intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo =
                    i_intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo wifiInfo = i_intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (networkInfo.isConnected()) {
                // Wifi is connected
                System.out.println("Wifi is connected: " + String.valueOf(networkInfo));
                m_hasValidConnection = true;
                m_wifiList.put(wifiInfo.getSSID(), true);
            }
        } else if (i_intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo =
                    i_intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            WifiInfo wifiInfo = i_intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                    !networkInfo.isConnected()) {
                // Wifi is disconnected
                System.out.println("Wifi is disconnected: " + String.valueOf(networkInfo));
                m_hasValidConnection = false;
                if (wifiInfo != null) {
                    m_wifiList.put(wifiInfo.getSSID(), false);
                }
            }
        }
    }
}
