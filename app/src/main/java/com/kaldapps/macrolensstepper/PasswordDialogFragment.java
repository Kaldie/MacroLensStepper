package com.kaldapps.macrolensstepper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by ruud on 30-7-16.
 */
public class PasswordDialogFragment extends DialogFragment {
    public final static String PASSWORD_RECEIVED_INTENT_TAG = "PASSWORD_RECEIVED";


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
                        Intent intent = new Intent(PASSWORD_RECEIVED_INTENT_TAG);
                        intent.putExtra("AP", ssid);
                        intent.putExtra("Password", password);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                })
                .create();
    }
}
