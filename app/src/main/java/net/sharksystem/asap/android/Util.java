package net.sharksystem.asap.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

public class Util {
    public static void unregisterBCR(String logstart, Context context, BroadcastReceiver bcr) {
        try {
            if (bcr != null) {
                context.unregisterReceiver(bcr);
            }
        }
        catch(RuntimeException e) {
            Log.d(logstart, "rt while try to unregister bc receiver: "
                    + e.getLocalizedMessage());
        }
    }
}
