package com.kaldapps.macrolensstepper;

import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruud on 29-7-16.
 */
public class WifiHelper extends AppCompatActivity {
    private WifiManager m_wifiManager;
    private String m_oldNetworkSSID;
    private boolean m_forcedConnection;

    public WifiHelper(WifiManager i_manager) {
         m_wifiManager = i_manager;
        m_oldNetworkSSID = getCurrentConnectionSSID();
    }

    public ArrayList<String> getScannedWifiAPs(boolean i_currentlyUsed) {
        String currentConnection;
        ArrayList<String> wifiSDD = new ArrayList<>();
        if (i_currentlyUsed) {
            currentConnection = getCurrentConnectionSSID();
            if (!currentConnection.isEmpty()) {
                wifiSDD.add(currentConnection);
                return wifiSDD;
            }
        }
        List<ScanResult> wifiScanList = m_wifiManager.getScanResults();
        for (int i = 1; i < wifiScanList.size(); i++) {
            wifiSDD.add((wifiScanList.get(i)).SSID);
        }
        for (String SSID : wifiSDD) {
            if (SSID.contains("ESP")) {
                wifiSDD.remove(wifiSDD.indexOf(SSID));
                wifiSDD.add(0, SSID);
                break;
            }
        }
        return wifiSDD;
    }


    private String getCurrentConnectionSSID() {
        // get the current connection
        String currentConnectedSSID = "";
        WifiInfo currentWifiInfo = m_wifiManager.getConnectionInfo();
        SupplicantState currentWifiState = currentWifiInfo.getSupplicantState();
        System.out.println("State is: " + currentWifiState.toString());
        switch (currentWifiState) {
            case ASSOCIATED:
            case ASSOCIATING:
            case COMPLETED:
                currentConnectedSSID = currentWifiInfo.getSSID();
                break;
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case SCANNING:
                getCurrentConnectionSSID();
                break;
        }
        return currentConnectedSSID;
    }


    @Nullable
    private WifiConfiguration getKnownWifiConfigForSSID(String i_SSID) {
        List<WifiConfiguration> wifiConfigurations = m_wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifiConfigurations) {
            if (config.SSID.equals(i_SSID)) {
                System.out.println("Found a wifi config. Returning this");
                return config;
            }
        }
        return null;
    }


    public WifiConfiguration getWifiConfigForSSID(String i_SSID) {
        WifiConfiguration wifiConfig = getKnownWifiConfigForSSID(i_SSID);
        if (wifiConfig != null) {
            return wifiConfig;
        } else {
            return makeConfiguration(i_SSID);
        }
    }


    private WifiConfiguration makeConfiguration(String i_ap) {
        System.out.println("makeConfiguration!!");
        List<ScanResult> scanResults = m_wifiManager.getScanResults();
        WifiConfiguration configuration = new WifiConfiguration();
        for (final ScanResult scanResult : scanResults) {
            scanResult.SSID = convertStringToValidSSID(scanResult.SSID);
            if (scanResult.SSID.equals(i_ap)) {
                configuration.BSSID = scanResult.BSSID;
                configuration.SSID = convertStringToValidSSID(scanResult.SSID);
                System.out.println(scanResult.capabilities);
                // Security mode
                if (scanResult.capabilities.contains("LEAP")) {
                    configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.LEAP);
                } else if (scanResult.capabilities.contains("WPA")) {
                    configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                } else if (scanResult.capabilities.contains("WPA2")) {
                    configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                } else if (scanResult.capabilities.contains("SHARED")) {
                    configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                }
                // GroupCiphers
                if (scanResult.capabilities.contains("CCMP")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                } else if (scanResult.capabilities.contains("TKIP")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                } else if (scanResult.capabilities.contains("WEP40")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                } else if (scanResult.capabilities.contains("WEP104")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                }
                // Allowed protocols, always go for the highest
                if (scanResult.capabilities.contains("WPA2")) {
                    configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                } else if (scanResult.capabilities.contains("WPA")) {
                    configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                }
                // key management
                if (scanResult.capabilities.contains("PSK")) {
                    configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                } else if (scanResult.capabilities.contains("EAP")) {
                    configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                } else if (scanResult.capabilities.contains("IEEE8021X")) {
                    configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                } else {
                    configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                return configuration;
            }
        }
        return configuration;
    }


    public static String convertStringToValidSSID(String i_ssid) {
        if (i_ssid.length() == 0) {
            return i_ssid;
        }

        if (i_ssid.charAt(0) != '"' |
                i_ssid.charAt(i_ssid.length() - 1) != '"') {
            i_ssid = "\"" + i_ssid + "\"";
        }
        return i_ssid;
    }


    public boolean isPasswordKnown(WifiConfiguration i_wifiConfiguration) {
        // check if we need a password
        if (i_wifiConfiguration.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.OPEN)) {
            if (i_wifiConfiguration.preSharedKey != null && !i_wifiConfiguration.preSharedKey.isEmpty()) {
                return true;
            }
        } else if (i_wifiConfiguration.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.SHARED)) {
            String wepKey = i_wifiConfiguration.wepKeys[i_wifiConfiguration.wepTxKeyIndex];
            if (wepKey != null && !wepKey.isEmpty()) {
                return true;
            }
        } else {
            // no key management
            return true;
        }
        // there is key management, but there is no known password
        return false;
    }


    public void updateConfiguration(WifiConfiguration wifiConfiguration) {
        if (getKnownWifiConfigForSSID(wifiConfiguration.SSID) != null) {
            // configuration is known update it
            m_wifiManager.updateNetwork(wifiConfiguration);
        } else {
            int newNetworkID = m_wifiManager.addNetwork(wifiConfiguration);
            if (newNetworkID != -1) {
                m_wifiManager.enableNetwork(newNetworkID,false);
            }
            System.out.println("New network id: " + newNetworkID);
        }
    }


    public boolean forceConnection(WifiConfiguration i_configuration) {
        m_forcedConnection = true;
        m_wifiManager.disconnect();
        m_wifiManager.enableNetwork(i_configuration.networkId,true);
        return m_wifiManager.reconnect();
    }


    public void restoreOldConnection() {
        // if the wifi helper is used to connect to a specific network, it will remember which network it previously was attached to.
        // Calling this function will ensure that the previously attached network will be re-enabled
        if (m_forcedConnection) {
            List<WifiConfiguration> wifiConfigurations = m_wifiManager.getConfiguredNetworks();
            // for the connection
            for (WifiConfiguration config : wifiConfigurations) {
                if (config.SSID.equals(m_oldNetworkSSID)) {
                    forceConnection(config);
                }
            }
            // then enable the other networks
            for (WifiConfiguration config : wifiConfigurations) {
                m_wifiManager.enableNetwork(config.networkId, false);
            }
        }
    }
}
