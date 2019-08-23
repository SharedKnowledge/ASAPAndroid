package net.sharksystem.asap.android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DiscoveryBroadcastReceiver extends BroadcastReceiver {
    private final BluetoothEngine bluetoothEngine;

    public DiscoveryBroadcastReceiver(BluetoothEngine bluetoothEngine) {
        this.bluetoothEngine = bluetoothEngine;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getLogStart(), "something changed in bt discovery - TODO - broadcast notify");
    }

    private String getLogStart() {
        return "ASAP_DiscoveryChangedBC";
    }
}
