package com.kaldapps.macrolensstepper;

import java.util.Properties;


/**
 * Created by ruud on 27-7-16.
 */

public class ESPStepper {
    Properties stepperConfiguration = new Properties();

    // default constructor
    public ESPStepper() {
        // initialise properties
        this.stepperConfiguration.put("currentAP", "Unknown");
        this.stepperConfiguration.put("currentIP", "Unknown");
        this.stepperConfiguration.put("mmPerStep", 0);
        }


    public void setCurrentAP(String i_ap) {
        this.stepperConfiguration.put("currentAP", i_ap);
    }


    public String getCurrentAP() {
        return this.stepperConfiguration.get("currentAP").toString();
    }


    public void setCurrentIP(String i_ip) throws IllegalArgumentException {
        if(validIP(i_ip)) {
            this.stepperConfiguration.setProperty("currentIP", i_ip);
        } else {
            throw new IllegalArgumentException("IP adres is anot a valid adres!");
        }
    }


    public String getCurrentIP() {
        return this.stepperConfiguration.get("currentIP").toString();
    }


    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
