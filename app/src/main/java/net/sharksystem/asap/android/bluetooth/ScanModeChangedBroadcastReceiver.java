package net.sharksystem.asap.android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;

import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE;
import static android.bluetooth.BluetoothAdapter.EXTRA_SCAN_MODE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_NONE;

public class ScanModeChangedBroadcastReceiver extends BroadcastReceiver {
    private final Context context;

    public ScanModeChangedBroadcastReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_SCAN_MODE_CHANGED.equals(action)) {
            // scan mode changed

            int mode = intent.getIntExtra(EXTRA_SCAN_MODE, -1);
            Log.d(this.getLogStart(), "new bt scan mode: " + this.getModeDescription(mode));

            int previousMode = intent.getIntExtra(EXTRA_PREVIOUS_SCAN_MODE, -1);
            Log.d(this.getLogStart(), "previous scan mode: " +
                    this.getModeDescription(previousMode));

            // check if discovery is gone
            if(mode == SCAN_MODE_NONE || mode == SCAN_MODE_CONNECTABLE) {
                this.context.sendBroadcast(
                        new ASAPServiceRequestNotifyIntent(
                                ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERABLE_STOPPED
                        ));
            }

            // check if discoverable started
            if(mode == SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                this.context.sendBroadcast(
                        new ASAPServiceRequestNotifyIntent(
                                ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERABLE_STARTED
                        ));
            }
        }
    }

    private String getLogStart() {
        return "ScanModeChanged_BR";
    }

    private String getModeDescription(int mode) {
        switch(mode) {
            case SCAN_MODE_NONE:
                return ("SCAN_MODE_NONE: inquiry and page scan disabled");

            case SCAN_MODE_CONNECTABLE:
                return ("SCAN_MODE_CONNECTABLE");

            case SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                return ("SCAN_MODE_CONNECTABLE_DISCOVERABLE");

            default:
                return ("unknown value: " + mode);
        }
    }
}
