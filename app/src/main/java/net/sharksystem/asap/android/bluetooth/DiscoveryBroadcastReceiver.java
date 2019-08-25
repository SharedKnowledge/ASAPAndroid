package net.sharksystem.asap.android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;


public class DiscoveryBroadcastReceiver extends BroadcastReceiver {
    private final BluetoothEngine bluetoothEngine;

    public DiscoveryBroadcastReceiver(BluetoothEngine bluetoothEngine) {
        this.bluetoothEngine = bluetoothEngine;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notifyIntent = null;

        switch(intent.getAction()) {
            case ACTION_DISCOVERY_STARTED:
                notifyIntent = new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STARTED
                );
                Log.d(this.getLogStart(), "discovery started");
                context.sendBroadcast(notifyIntent);
                break;

            case ACTION_DISCOVERY_FINISHED:
                notifyIntent = new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STOPPED
                );
                Log.d(this.getLogStart(), "discovery finished");
                context.sendBroadcast(notifyIntent);
                break;

            default:
                Log.d(this.getLogStart(), "unknwon action - weired");
        }
}


    private String getLogStart() {
        return "ASAP_DiscoveryChangedBC";
    }
}
