package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.sharksystem.asap.android.MacLayerEngine;
import net.sharksystem.asap.android.ASAPService;

import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;

public class BluetoothEngine extends MacLayerEngine {
    private static BluetoothEngine engine = null;
    private BluetoothAdapter mBluetoothAdapter;
    private FoundBTDevicesBroadcastReceiver foundBTDeviceBR;

    private static final int DEFAULT_VISIBILITY_TIME = 120;
    public static int visibilityTimeInSeconds = DEFAULT_VISIBILITY_TIME;
    private ScanModeChangedBroadcastReceiver scanModeChangedBR = null;

    public static BluetoothEngine getAASPBluetoothEngine(ASAPService ASAPService,
                                                         Context context) {
        if(BluetoothEngine.engine == null) {
            BluetoothEngine.engine = new BluetoothEngine(ASAPService, context);
        }

        return BluetoothEngine.engine;
    }

    public static BluetoothEngine getAASPBluetoothEngine() {
        return BluetoothEngine.engine;
    }

    private BluetoothEngine(ASAPService ASAPService, Context context) {
        super(ASAPService, context);
    }

    public void start() {
        Log.d("AASPBluetoothEngine", "start bluetooth");
        this.setup();
    }

    public void stop() {
        Log.d("AASPBluetoothEngine", "stop bluetooth");
        this.shutdown();
    }

    /////////////////////////////////////////////////////////////////////////////////
    //                             do the real bluetooth stuff                     //
    /////////////////////////////////////////////////////////////////////////////////

    //https://developer.android.com/guide/topics/connectivity/bluetooth#java

    /**
     * Setup Bluetooth environment
     */
    private void setup() {

        // get default bt adapter - there could be proprietary adapters
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i("AASPBluetoothEngine", "device does not support bluetooth");
        }

        // those things are to be done in calling activity
        Log.d("AASPBluetoothEngine", "asking for BT enabling works?");
        /*
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.getContext().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        */

        // a list of already paired devices can be retrieved - don't that help but
        // could require some further investigations

        // create and register broadcast receiver
        this.foundBTDeviceBR = new FoundBTDevicesBroadcastReceiver(this);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.getContext().registerReceiver(this.foundBTDeviceBR, filter);

        // make this device visible
        // TODO


        // start device discovery
    }

    /**
     * Start a BT scanning sweep of the area. According to android manual, each
     * sweep takes 12 seconds - thus that method could be called frequently.
     */
    private void startDiscovery() {

    }

    /**
     * Make devices visible. Visibility time is a parameter. Android default is
     * 120 seconds.
     */
    private void startDiscoverable() {
        int effectiveVisibilityTime =
                BluetoothEngine.visibilityTimeInSeconds > 0 ?
                        BluetoothEngine.visibilityTimeInSeconds :
                        BluetoothEngine.DEFAULT_VISIBILITY_TIME;

        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                BluetoothEngine.visibilityTimeInSeconds);

        Log.d("AASPBluetoothEngine", "make device bt visibile for seconds: "
                + BluetoothEngine.visibilityTimeInSeconds);

        // get informed about changes of visibility

        if(this.scanModeChangedBR == null) {
            this.scanModeChangedBR = new ScanModeChangedBroadcastReceiver();
            IntentFilter filter = new IntentFilter(ACTION_SCAN_MODE_CHANGED);
            this.getContext().registerReceiver(this.foundBTDeviceBR, filter);
        }


        this.getContext().startActivity(discoverableIntent);

    }

    private void shutdown() {

        // stop bt adapter TODO

        // unregister broadcast receiver
        if(this.foundBTDeviceBR != null) {
            this.getContext().unregisterReceiver(this.foundBTDeviceBR);
        }

        if(this.scanModeChangedBR != null) {
            this.getContext().unregisterReceiver(this.scanModeChangedBR);
        }
    }

    void deviceFound(String name, String hardwareAddress) {
        Log.d("AASPBluetoothEngine", "deviceFound called");
        this.mBluetoothAdapter.getRemoteDevice(hardwareAddress);
    }
}
