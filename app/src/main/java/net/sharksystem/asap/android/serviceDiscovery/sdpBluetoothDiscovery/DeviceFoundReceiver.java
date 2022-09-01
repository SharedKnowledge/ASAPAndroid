package net.sharksystem.asap.android.serviceDiscovery.sdpBluetoothDiscovery;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;




/**
 * While a bluetooth device discovery is running, this
 * broadcast receiver will notify the SdpBluetooth engine 
 * about discovered devices using {@link SdpBluetoothDiscoveryEngine#onDeviceDiscovered(BluetoothDevice)}
 *
 * @author WilliBoelke
 */
class DeviceFoundReceiver extends BroadcastReceiver
{

    //
    //  ----------  instance variables ----------
    //

    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();
    /**
     * SdpBluetooth engine to notify
     */
    private final SdpBluetoothDiscoveryEngine discoveryEngine;


    //
    //  ----------  constructor and initialisation ----------
    //

    /**
     * Public constructor
     * @param discoveryEngine
     * The discovery engine to be notified about discovered devices
     */
    public DeviceFoundReceiver(SdpBluetoothDiscoveryEngine discoveryEngine){
        Log.d(TAG, "DeviceFoundReceiver: initialised receiver");
        this.discoveryEngine = discoveryEngine;
    }

    //
    //  ----------  broadcast receiver methods  ----------
    //

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_FOUND))
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "onReceive: discovered new device " + device);
            this.discoveryEngine.onDeviceDiscovered(device);
        }
    }
}
