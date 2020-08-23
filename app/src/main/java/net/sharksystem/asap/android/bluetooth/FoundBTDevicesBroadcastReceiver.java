package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.ASAPException;

class FoundBTDevicesBroadcastReceiver extends BroadcastReceiver {

    private final BluetoothEngine bluetoothEngine;

    FoundBTDevicesBroadcastReceiver(BluetoothEngine bluetoothEngine) {
        this.bluetoothEngine = bluetoothEngine;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Log.d(this.getLogStart(), "got ACTION_FOUND intent");
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            Log.d(this.getLogStart(), deviceName + "/" + deviceHardwareAddress);

            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            Log.d(this.getLogStart(), "found BT class: " + btClass.toString());

            try {
                this.bluetoothEngine.tryConnect(device, btClass);
            } catch (ASAPException e) {
                Log.e(this.getLogStart(),
                        "could not handle device found: " + e.getLocalizedMessage());
            }
        }
    }
}
