package net.sharksystem.asap.android.serviceDiscovery.bluetoothServiceDiscovery;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;


/**
 * Listens on {@link BluetoothDevice#ACTION_UUID}, notifies the
 * engine by calling {@link BluetoothDiscoveryEngine#onUuidsFetched(BluetoothDevice, Parcelable[])}
 * when UUIDs where fetched.
 */
class UUIDFetchedReceiver extends BroadcastReceiver
{

    //
    //  ----------  instance variables ----------
    //

    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();

    /**
     * The SdpBluetoothDiscoveryEngine to be notified
     */
    private final BluetoothDiscoveryEngine discoveryEngine;


    //
    //  ----------  constructor and initialisation ----------
    //

    /**
     * Public constructor
     * @param discoveryEngine
     * The SdpBluetoothDiscoveryEngine to be notified
     */
    public UUIDFetchedReceiver(BluetoothDiscoveryEngine discoveryEngine)
    {
        this.discoveryEngine = discoveryEngine;
    }


    //
    //  ----------  broadcast receiver methods ----------
    //

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        if (action.equals(BluetoothDevice.ACTION_UUID))
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
            Log.d(TAG, "onReceive: received UUIDs for " + device);
            this.discoveryEngine.onUuidsFetched(device, uuidExtra);
        }
    }
}
