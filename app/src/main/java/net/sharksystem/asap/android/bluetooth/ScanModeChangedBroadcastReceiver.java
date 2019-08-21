package net.sharksystem.asap.android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE;
import static android.bluetooth.BluetoothAdapter.EXTRA_SCAN_MODE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_NONE;

class ScanModeChangedBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_SCAN_MODE_CHANGED.equals(action)) {
            // scan mode changed

            int mode = intent.getIntExtra(EXTRA_SCAN_MODE, -1);
            Log.d(this.getLogStart(), "new bt scan mode: " + this.getModeDescription(mode));

            mode = intent.getIntExtra(EXTRA_PREVIOUS_SCAN_MODE, -1);
            Log.d(this.getLogStart(), "previous scan mode: " + this.getModeDescription(mode));
        }
    }

    private String getLogStart() {
        return "ScanMode_BR";
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
                return ("unknwon value: " + mode);
        }
    }
}
