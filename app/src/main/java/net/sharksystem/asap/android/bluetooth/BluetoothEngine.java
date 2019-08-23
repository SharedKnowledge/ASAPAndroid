package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.sharksystem.asap.android.MacLayerEngine;
import net.sharksystem.asap.android.ASAPService;
import net.sharksystem.asap.android.util.ASAPServiceRequestNotifyIntent;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;

public class BluetoothEngine extends MacLayerEngine {
    private static BluetoothEngine engine = null;
    private BluetoothAdapter mBluetoothAdapter;
    private FoundBTDevicesBroadcastReceiver foundBTDeviceBC;

    public static final int DEFAULT_VISIBILITY_TIME = 120;
    public static int visibilityTimeInSeconds = DEFAULT_VISIBILITY_TIME;
    private ScanModeChangedBroadcastReceiver scanModeChangedBC = null;
    private DiscoveryBroadcastReceiver discoveryChangesBC;
    private boolean btEnvironmentOn = false;

    public static BluetoothEngine getASAPBluetoothEngine(ASAPService ASAPService,
                                                         Context context) {
        if(BluetoothEngine.engine == null) {
            BluetoothEngine.engine = new BluetoothEngine(ASAPService, context);
        }

        return BluetoothEngine.engine;
    }

    public static BluetoothEngine getASAPBluetoothEngine() {
        return BluetoothEngine.engine;
    }

    private BluetoothEngine(ASAPService ASAPService, Context context) {
        super(ASAPService, context);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

    public void start() {
        Log.d(this.getLogStart(), "start bluetooth");
        this.setup();
    }

    public void stop() {
        Log.d(this.getLogStart(), "stop bluetooth");
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

        ///////////////////////////////////////////////////////////////////////////////////////
        //                                 setup bt environment                              //
        ///////////////////////////////////////////////////////////////////////////////////////

        // get default bt adapter
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i(this.getLogStart(), "device does not support bluetooth - give up");
            return;
        }

        // adapter enabled? if not - ask activity to ask user to enable
        Log.d(this.getLogStart(), "asking for BT enabling works?");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(this.getLogStart(),
                    "Bluetooth disabled - ask application for help - stop setting up bt");

            Intent requestIntent = new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_RQ_ASK_USER_TO_ENABLE_BLUETOOTH);

            this.getContext().sendBroadcast(requestIntent);

            return;
        }

        ///////////////////////////////////////////////////////////////////////////////////////
        // setup discoverable: init broadcast receiver and start first discoverable session  //
        ///////////////////////////////////////////////////////////////////////////////////////

        // setup broadcast receiver: get informed about changes of visibility
        if(this.scanModeChangedBC == null) {
            this.scanModeChangedBC = new ScanModeChangedBroadcastReceiver(this.getContext());

            IntentFilter filter = new IntentFilter(ACTION_SCAN_MODE_CHANGED);
            this.getContext().registerReceiver(this.scanModeChangedBC, filter);
        }

        // make this device visible - for some time
        this.startDiscoverable();

        ///////////////////////////////////////////////////////////////////////////////////////
        //    setup discovery: init broadcast receiver and start first discovery session     //
        ///////////////////////////////////////////////////////////////////////////////////////

        // create and register broadcast receiver that is informed about discovery changes
        if(this.discoveryChangesBC == null) {
            this.discoveryChangesBC = new DiscoveryBroadcastReceiver(this);

            // TODO: check if all actions added.
            IntentFilter filter = new IntentFilter(ACTION_DISCOVERY_STARTED);
            filter.addAction(ACTION_DISCOVERY_FINISHED);
            this.getContext().registerReceiver(this.discoveryChangesBC, filter);
        }

        // create and register broadcast receiver which is called whenever a device is found
        if(this.foundBTDeviceBC == null) {
            this.foundBTDeviceBC = new FoundBTDevicesBroadcastReceiver(this);

            IntentFilter filter = new IntentFilter(ACTION_FOUND);
            this.getContext().registerReceiver(this.foundBTDeviceBC, filter);
        }

        // start device discovery - for some time
        this.startDiscovery();

        this.btEnvironmentOn = true;

        /* Note
        discovery and discoverable are switched off after seconds (and user defined - discoverable)
        must be re-started
         */
    }

    public void startDiscoverable() {
        this.startDiscoverable(BluetoothEngine.DEFAULT_VISIBILITY_TIME);
    }

    /**
     * Make devices visible. Visibility time is a parameter. Android default is
     * 120 seconds.
     */
    public void startDiscoverable(int time) {
        // note: a value of 0 would mean: for ever - we don't allow that
        int effectiveVisibilityTime = time > 0 ? time : BluetoothEngine.DEFAULT_VISIBILITY_TIME;

        Log.d(this.getLogStart(), "ask activity to make device bt visibile for seconds: "
                + BluetoothEngine.visibilityTimeInSeconds);

        this.getContext().sendBroadcast(
                new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_RQ_ASK_USER_TO_START_BT_DISCOVERABLE,
                    effectiveVisibilityTime)
        );
    }

    /**
     * Start a BT scanning sweep of the area. According to android manual, each
     * sweep takes 12 seconds - thus that method could be called frequently.
     */
    public void startDiscovery() {
        if(this.mBluetoothAdapter.startDiscovery()) {
            Log.d(this.getLogStart(), "successfully started Bluetooth discovery");
        } else {
            Log.d(this.getLogStart(), "could not start Bluetooth discovery");
        }
    }

    private void shutdown() {
        // unregister broadcast receiver
        if(this.foundBTDeviceBC != null) {
            this.getContext().unregisterReceiver(this.foundBTDeviceBC);
        }

        if(this.scanModeChangedBC != null) {
            this.getContext().unregisterReceiver(this.scanModeChangedBC);
        }

        // stop BT adapter
        this.mBluetoothAdapter.cancelDiscovery();
        this.mBluetoothAdapter.disable();

        this.btEnvironmentOn = false;
    }

    void deviceFound(String name, String hardwareAddress, BluetoothClass btClass) {
        Log.d(this.getLogStart(), "deviceFound called");
        this.mBluetoothAdapter.getRemoteDevice(hardwareAddress);
    }
}
