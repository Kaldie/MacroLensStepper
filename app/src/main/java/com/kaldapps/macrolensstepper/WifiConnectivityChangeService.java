package com.kaldapps.macrolensstepper;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by ruud on 20-9-16.
 */

public class WifiConnectivityChangeService extends Service{
    private final static String TAG = "WifiConnectivityChangeService";
    public static final String BROADCAST_ACTION = "com.kaldapps.macrolensstepper.displayevent";
    private final Handler handler = new Handler();
    Intent intent;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
