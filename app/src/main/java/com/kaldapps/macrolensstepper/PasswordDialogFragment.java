package com.kaldapps.macrolensstepper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by ruud on 30-7-16.
 */
public class PasswordDialogFragment extends DialogFragment {
    public enum TypeOfConnection {
        NORMAL,
        ESP
    }


    public static PasswordDialogFragment newInstance(String i_ssid) {
        PasswordDialogFragment frag = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString("SSID", i_ssid);
        frag.setArguments(args);
        return frag;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final EditText passwordEditText = new EditText(getActivity());
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        final String ssid = getArguments().getString("SSID");
        return new AlertDialog.Builder(getActivity())
                .setTitle("Wifi password for: " + ssid)
                .setMessage("Please supply the password for the network!")
                .setView(passwordEditText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = passwordEditText.getText().toString();
                        ((DisplayConfigurationActivity)getActivity()).acceptPassword(ssid, password);
                    }
                })
                .create();
    }
}
