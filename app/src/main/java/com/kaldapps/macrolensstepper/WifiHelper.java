package com.kaldapps.macrolensstepper;



import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ruud on 29-7-16.
 */

class WifiHelper {
    private static final String TAG = "WifiHelper";
    private WifiManager m_wifiManager;
    private String m_oldNetworkSSID;

    WifiHelper(WifiManager i_manager) {
        m_wifiManager = i_manager;
        m_oldNetworkSSID = getCurrentConnectionSSID();
    }


    ArrayList<String> getScannedWifiAPs() {
        ArrayList<String> wifiSDDs = new ArrayList<>();
        List<ScanResult> wifiScanList = m_wifiManager.getScanResults();
        for (int i = 0; i < wifiScanList.size(); i++) {
            Log.d(TAG,wifiScanList.get(i).SSID);
            wifiSDDs.add(wifiScanList.get(i).SSID);
        }
        return wifiSDDs;
    }


    String getCurrentConnectionSSID() {
        // get the current connection
        String currentConnectedSSID = "";
        WifiInfo currentWifiInfo = m_wifiManager.getConnectionInfo();
        SupplicantState currentWifiState = currentWifiInfo.getSupplicantState();
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
        String properSSID;
        for (WifiConfiguration config : wifiConfigurations) {
            if (config != null && config.SSID!=null) {
                properSSID = WifiHelper.convertStringToValidSSID(config.SSID);
                if (config.SSID.equals(i_SSID) ||
                        properSSID.equals(i_SSID)) {
                    System.out.println("Found a wifi config. Returning this");
                    return config;
                }
            }
        }
        return null;
    }


    WifiConfiguration getWifiConfigForSSID(String i_SSID, Boolean i_forceFresh) {
        WifiConfiguration wifiConfig = getKnownWifiConfigForSSID(i_SSID);
        if (wifiConfig != null && !i_forceFresh) {
            return wifiConfig;
        } else {
            return makeConfiguration(i_SSID);
        }
    }

    // yea...java....no default params (got a nice solution...)
    WifiConfiguration getWifiConfigForSSID(String i_SSID) {
        return getWifiConfigForSSID(i_SSID, false);
    }


    private List<String> splitCapabilities(String i_capabilities) {
        String[] array = i_capabilities.split("\\]");
        List<String> capabilities = new ArrayList<>();
        for (String capability : array) {
            Character start = capability.charAt(0);
            Character end = capability.charAt(capability.length()-1);
            if (start == ']'|| start == '[') {
                capability = capability.substring(1);
            }
            if (end == ']'|| end == '[') {
                capability = capability.substring(0, capability.length() - 2);
            }
            capabilities.add(capability);
        }
        return capabilities;
    }


    private List<String> getConnectionTypes(List<String> i_splittedCapabilities) {
        List<String> unsortedConnectionTypes = new ArrayList<>();
        String method;
        for (String capability : i_splittedCapabilities) {
            method = capability.split("-")[0];
            unsortedConnectionTypes.add(method);
        }

        Collections.sort(unsortedConnectionTypes,new Comparator<String>() {
            int value(String input) {
                if (input.equals("WPA2")) {
                    return 3;
                } else if (input.equals("WPA")) {
                    return 2;
                } else if (input.equals("WEP")) {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public int compare(String o1, String o2) {
                int o1Value = value(o1);
                int o2Value = value(o2);
                if (o2Value == o1Value)
                    return 0;
                if (o1Value> o2Value)
                    return 1;
                else {
                    return 0;
                }
            }
        });
        return unsortedConnectionTypes;
    }


    private String getCapabilitiesOfConnection(List<String> i_splittedCapabilities,
                                               String i_connection) {
        for (String capability : i_splittedCapabilities) {
            if  (i_connection.equals(capability.split("-")[0])) {
                return capability;
            }
        }
        return "";
    }


    private WifiConfiguration makeConfiguration(String i_ap) {
        Log.d(TAG, "makeConfiguration!!");
        List<ScanResult> scanResults = m_wifiManager.getScanResults();
        WifiConfiguration configuration = new WifiConfiguration();
        for (final ScanResult scanResult : scanResults) {
            scanResult.SSID = convertStringToValidSSID(scanResult.SSID);
            if (scanResult.SSID.equals(i_ap)) {
                configuration.SSID = scanResult.SSID;
                configuration.status = WifiConfiguration.Status.ENABLED;

                List<String> capabilities = splitCapabilities(scanResult.capabilities);
                String favConnection = getConnectionTypes(capabilities).get(0);
                String capability = getCapabilitiesOfConnection(capabilities, favConnection);
                configuration.allowedKeyManagement.clear();
                configuration.allowedGroupCiphers.clear();
                configuration.allowedAuthAlgorithms.clear();
                configuration.allowedProtocols.clear();
                configuration.allowedPairwiseCiphers.clear();

                if (capability.contains("TKIP")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                }
                if (capability.contains("CCMP")) {
                    configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                }
                switch (favConnection) {
                    case "WEP" :
                        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        break;
                    case "WPA":
                    case "WPA2":
                        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                        configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        break;
                    default:
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
        if (i_wifiConfiguration.allowedProtocols.get(WifiConfiguration.Protocol.RSN) ||
                i_wifiConfiguration.allowedProtocols.get(WifiConfiguration.Protocol.WPA)) {
            if (i_wifiConfiguration.preSharedKey != null && i_wifiConfiguration.preSharedKey.length() > 0)
                return true;
        }
        if (i_wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && (
                i_wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104) ||
                        i_wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104))
                ) {
            if (i_wifiConfiguration.wepKeys[i_wifiConfiguration.wepTxKeyIndex].length() > 0) {
                return true;
            }
        }
        return false;
    }


    Boolean hasPassword(WifiConfiguration i_wifiConfiguration) {
        WifiConfiguration config = makeConfiguration(i_wifiConfiguration.SSID);
        return config.allowedProtocols.get(WifiConfiguration.Protocol.RSN) ||
                config.allowedProtocols.get(WifiConfiguration.Protocol.WPA) ||
                config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40) ||
                config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104);
    }


    int updateConfiguration(WifiConfiguration wifiConfiguration) {
        int newNetworkID;
        WifiConfiguration knownConfig = getKnownWifiConfigForSSID(wifiConfiguration.SSID);
        System.out.println(knownConfig.toString());
        System.out.println(wifiConfiguration.toString());
        if (knownConfig != null && knownConfig.BSSID != null) {
            wifiConfiguration.networkId = knownConfig.networkId;
            wifiConfiguration.priority = knownConfig.priority;
            // configuration is known update it
            Log.d(TAG,"Update network");
            newNetworkID = m_wifiManager.updateNetwork(knownConfig);
            m_wifiManager.enableNetwork(newNetworkID,false);
        } else {
            newNetworkID= m_wifiManager.addNetwork(wifiConfiguration);
            if (newNetworkID != -1) {
                m_wifiManager.enableNetwork(newNetworkID,false);
            }
        }
        Log.d(TAG,"New network id: " + newNetworkID);
        return newNetworkID;
    }


    boolean forceConnection(WifiConfiguration i_configuration) {
        getWifiConfigForSSID(i_configuration.SSID);
        m_wifiManager.disconnect();
        m_wifiManager.enableNetwork(i_configuration.networkId,true);
        return m_wifiManager.reconnect();
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

    public void acceptPassword(String i_SSID, String i_password) {
        if (i_password != null && !i_password.isEmpty() &&
                i_SSID != null && !i_SSID.isEmpty()) {
            i_SSID = WifiHelper.convertStringToValidSSID(i_SSID);
            System.out.println("Password '" + i_password + "' is selected for ssid: " + i_SSID + " !");
            WifiConfiguration wifiConfiguration = getWifiConfigForSSID(i_SSID, true);
            Log.d(TAG,wifiConfiguration.toString());
            if (wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40) ||
                    wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104)) {
                if (i_password.matches("^[0-9a-fA-F]+$")) {
                    wifiConfiguration.wepKeys[0] = i_password;
                } else {
                    wifiConfiguration.wepKeys[0] = "\"" + i_password + "\"";
                }
                wifiConfiguration.wepTxKeyIndex = 0;
            } else {
                Log.d(TAG,"Updated the pre shared key.");
                wifiConfiguration.preSharedKey = "\"" + i_password + "\"";
            }

            wifiConfiguration.networkId = updateConfiguration(wifiConfiguration);
            if (wifiConfiguration.networkId == -1) {
                Log.d(TAG,"Boo boo at updating the AP!");
                return;
            }
            forceConnection(wifiConfiguration);
        }
    }
}
