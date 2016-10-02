package com.kaldapps.macrolensstepper;



import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruud on 29-7-16.
 */

class WifiHelper {
    private static final String TAG = "WifiHelper";
    private WifiManager m_wifiManager;
    private String m_oldNetworkSSID;
    private boolean m_forcedConnection;

    WifiHelper(WifiManager i_manager) {
        m_wifiManager = i_manager;
        m_oldNetworkSSID = getCurrentConnectionSSID();
        m_forcedConnection = false;
    }


    ArrayList<String> getScannedWifiAPs() {
        String currentConnection;
        ArrayList<String> wifiSDDs = new ArrayList<>();
        currentConnection = getCurrentConnectionSSID();
        if (!currentConnection.isEmpty()) {
            wifiSDDs.add(currentConnection);
        }

        List<ScanResult> wifiScanList = m_wifiManager.getScanResults();
        String SSID;
        for (int i = 1; i < wifiScanList.size(); i++) {
            SSID = wifiScanList.get(i).SSID;
            if (!wifiScanList.get(i).SSID.equals(currentConnection)) {
                wifiSDDs.add((wifiScanList.get(i)).SSID);
                if (SSID.contains("ESP")) {
                    wifiSDDs.remove(wifiSDDs.indexOf(SSID));
                    wifiSDDs.add(1, SSID);
                    break;
                }
            }
        }
        return wifiSDDs;
    }


    String getCurrentConnectionSSID() {
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
        }
        return currentConnectedSSID;
    }


    Boolean isConnected() {
        return !getCurrentConnectionSSID().equals("");
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


    WifiConfiguration getWifiConfigForSSID(String i_SSID) {
        WifiConfiguration wifiConfig = getKnownWifiConfigForSSID(i_SSID);
        if (wifiConfig != null) {
            return wifiConfig;
        } else {
            return makeConfiguration(i_SSID);
        }
    }


    private WifiConfiguration makeConfiguration(String i_ap) {
        Log.d(TAG,"makeConfiguration!!");
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


    static String convertStringToValidSSID(String i_ssid) {
        if (i_ssid.length() == 0) {
            return i_ssid;
        }

        if (i_ssid.charAt(0) != '"' |
                i_ssid.charAt(i_ssid.length() - 1) != '"') {
            i_ssid = "\"" + i_ssid + "\"";
        }
        return i_ssid;
    }


    private InetAddress getBroadcastAddress() throws IOException {
        DhcpInfo dhcp = m_wifiManager.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }


    boolean isPasswordKnown(WifiConfiguration i_wifiConfiguration) {
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


    static Boolean hasPassword(WifiConfiguration i_wifiConfiguration) {
        return i_wifiConfiguration.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.OPEN) ||
                i_wifiConfiguration.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.SHARED) ||
                i_wifiConfiguration.preSharedKey.length() != 0;
    }


    void updateConfiguration(WifiConfiguration wifiConfiguration) {
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


    boolean forceConnection(WifiConfiguration i_configuration) {
        m_forcedConnection = true;
        m_wifiManager.disconnect();
        m_wifiManager.enableNetwork(i_configuration.networkId,true);
        return m_wifiManager.reconnect();
    }


    void restoreOldConnection() {
        // if the wifi helper is used to connect to a specific network, it will remember which network it previously was attached to.
        // Calling this function will ensure that the previously attached network will be re-enabled
        if (m_forcedConnection) {
            List<WifiConfiguration> wifiConfigurations = m_wifiManager.getConfiguredNetworks();
            if (wifiConfigurations != null) {
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


    void sendUDPMessage(final String msg, final int i_port) {
        InetAddress address = null;
        try {
            address = getBroadcastAddress();
            DatagramSocket clientSocket = new DatagramSocket(1025);
            clientSocket.setBroadcast(true);

            byte[] sendData;
            sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData,
                    sendData.length, getBroadcastAddress(), i_port);
            sendPacket.setPort(i_port);
            clientSocket.send(sendPacket);

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Broadcast address: " + address.toString());
        System.out.println(" sending upd message!");
    }


    String receiveUDPMessage(int i_port, int i_length) {
        byte[] buf = new byte[i_length];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        DatagramSocket receive_socket = null;
        try {
            // Prepare a UDP-Packet that can contain the data we want to receive
            receive_socket = new DatagramSocket(i_port);
            // Receive the UDP-Packet
            receive_socket.setSoTimeout(5000);
            receive_socket.setReuseAddress(true);
            receive_socket.receive(packet);
            receive_socket.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Bitch didn't respond");
            return "";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (receive_socket != null)
            receive_socket.close();
        }
        return new String(packet.getData());
    }


    static private String getHtml(final String i_ip, final String i_addition) {
        String url = "http://" + i_ip + "/" + i_addition;
        StringBuilder htmlOutput = null;
        try {
            // Build and set timeout values for the request.
            URLConnection connection = (new URL(url)).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            // Read and store the result line by line then return the entire string.
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            htmlOutput = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                htmlOutput.append(line);
            }
            in.close();
        } catch ( IOException e) {
            e.printStackTrace();
        }

        if (htmlOutput != null) {
            return htmlOutput.toString();
        } else {
            return "";
        }
    }


    static private String sendHtml(final String i_ip, final String i_addition, final String i_payload) {
        String url = "http://" + i_ip + "/" + i_addition;
        OutputStreamWriter streamWriter;
        StringBuilder htmlOutput = null;
        try {
            URLConnection connection = (new URL(url)).openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            streamWriter = new OutputStreamWriter(connection.getOutputStream());
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            streamWriter.write(i_payload);
            streamWriter.close();

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            htmlOutput = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                System.out.println(line);
                htmlOutput.append(line);
            }
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (htmlOutput != null) {
            return htmlOutput.toString();
        } else {
            return "";
        }
    }


    @Nullable
    static JSONObject getJson(final String i_ip, final String i_addition) {
        String htmlString = getHtml(i_ip, i_addition);
        if (htmlString.equals("")) {
            return null;
        }
        JSONObject jObj;
        try {
            jObj = new JSONObject(htmlString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jObj;
    }


    static String sendJson(final JSONObject jsonObj, final String i_ip, final String i_addition) {
        return sendHtml(i_ip, i_addition, jsonObj.toString());
    }
}
