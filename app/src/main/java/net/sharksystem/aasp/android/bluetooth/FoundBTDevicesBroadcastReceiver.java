package net.sharksystem.aasp.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class FoundBTDevicesBroadcastReceiver extends BroadcastReceiver {

    private final BluetoothEngine bluetoothEngine;

    FoundBTDevicesBroadcastReceiver(BluetoothEngine bluetoothEngine) {
        this.bluetoothEngine = bluetoothEngine;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address

            Log.d("FoundBTDev_BR", deviceName + "/" + deviceHardwareAddress);

            this.bluetoothEngine.deviceFound(deviceName, deviceHardwareAddress);
        }
    }
}
