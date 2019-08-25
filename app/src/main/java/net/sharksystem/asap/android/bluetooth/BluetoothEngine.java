package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAPSession;
import net.sharksystem.asap.android.MacLayerEngine;
import net.sharksystem.asap.android.ASAPService;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;

import java.io.IOException;

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
    private BluetoothServerSocketThread btServerSocketThread;

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
     * Setup Bluetooth environment. Note: Only the environment is created.
     * Discovery and discoverability is not initiated.
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
        Log.d(this.getLogStart(), "check if BT is enabled");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(this.getLogStart(),
                    "Bluetooth disabled - ask application for help - stop setting up bt");

            Intent requestIntent = new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_RQ_ASK_USER_TO_ENABLE_BLUETOOTH);

            this.getContext().sendBroadcast(requestIntent);

            return;
        } else {
            Log.d(this.getLogStart(), "BT is enabled - BT up and running - now setup BC Receiver");
        }

        ///////////////////////////////////////////////////////////////////////////////////////
        //                              setup broadcast receiver                             //
        ///////////////////////////////////////////////////////////////////////////////////////

        // setup broadcast receiver: get informed about changes of visibility
        Log.d(this.getLogStart(), "set up ACTION_SCAN_MODE_CHANGED BC Receiver");
        if(this.scanModeChangedBC == null) {
            this.scanModeChangedBC = new ScanModeChangedBroadcastReceiver(this.getContext());

            IntentFilter filter = new IntentFilter(ACTION_SCAN_MODE_CHANGED);
            this.getContext().registerReceiver(this.scanModeChangedBC, filter);
        }

        // create and register broadcast receiver that is informed about discovery changes
        Log.d(this.getLogStart(), "set up Discovery changes BC Receiver");
        if(this.discoveryChangesBC == null) {
            this.discoveryChangesBC = new DiscoveryBroadcastReceiver(this);

            // TODO: check if all actions added.
            IntentFilter filter = new IntentFilter(ACTION_DISCOVERY_STARTED);
            filter.addAction(ACTION_DISCOVERY_FINISHED);
            this.getContext().registerReceiver(this.discoveryChangesBC, filter);
        }

        // create and register broadcast receiver which is called whenever a device is found
        Log.d(this.getLogStart(), "set up found BT devices BC Receiver");
        if(this.foundBTDeviceBC == null) {
            this.foundBTDeviceBC = new FoundBTDevicesBroadcastReceiver(this);

            IntentFilter filter = new IntentFilter(ACTION_FOUND);
            this.getContext().registerReceiver(this.foundBTDeviceBC, filter);
        }

        ///////////////////////////////////////////////////////////////////////////////////////
        //                                start BT server socket                             //
        ///////////////////////////////////////////////////////////////////////////////////////

        try {
            this.btServerSocketThread = new BluetoothServerSocketThread(this);
            this.btServerSocketThread.start();
        } catch (IOException | ASAPException e) {
            Log.e(this.getLogStart(), "could not set up BT server socket - quite fatal");
        }

        this.btEnvironmentOn = true;

        // broadcast the news
        this.getContext().sendBroadcast(
                new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STARTED));
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
            Log.e(this.getLogStart(), "could not start Bluetooth discovery");
        }
    }

    private void shutdown() {
        // unregister broadcast receiver
        if (this.foundBTDeviceBC != null) {
            Util.unregisterBCR(this.getLogStart(), this.getContext(), this.foundBTDeviceBC);
            this.foundBTDeviceBC = null;
        }

        if(this.scanModeChangedBC != null) {
            Util.unregisterBCR(this.getLogStart(), this.getContext(), this.scanModeChangedBC);
            this.scanModeChangedBC = null;
        }

        if(this.discoveryChangesBC != null) {
            Util.unregisterBCR(this.getLogStart(), this.getContext(), this.discoveryChangesBC);
            this.discoveryChangesBC = null;
        }

        // stop server socket
        if(this.btServerSocketThread != null) {
            this.btServerSocketThread.stopAccept();
            this.btServerSocketThread = null;
        }

        // stop BT adapter
        this.mBluetoothAdapter.cancelDiscovery();
        this.mBluetoothAdapter.disable();

        this.btEnvironmentOn = false;

        // broadcast the news
        this.getContext().sendBroadcast(
            new ASAPServiceRequestNotifyIntent(
                ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED));
    }

    BluetoothAdapter getBTAdapter() throws ASAPException {
        if(this.mBluetoothAdapter == null)
            throw new ASAPException("bluetooth not yet initialized");

        return this.mBluetoothAdapter;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //                       handle connections and connection attempts                    //
    /////////////////////////////////////////////////////////////////////////////////////////

    void deviceFound(BluetoothDevice btDevice, BluetoothClass btClass) {
        Log.d(this.getLogStart(), "deviceFound called");
        String macAddress = btDevice.getAddress();// MAC address
        btDevice.getName(); // name

        // strongly recommended to stop discovery
        //this.mBluetoothAdapter.cancelDiscovery();

        if(this.shouldConnectToMACPeer(macAddress)) {
            Log.d(this.getLogStart(), "create and BT client socket thread");
            new BluetoothClientSocketThread(this, btDevice).start();
        }
    }

    /**
     * Both client and server sockets
     * @param socket
     * @throws IOException
     */
    void handleBTSocket(BluetoothSocket socket) throws IOException {
        Log.d(this.getLogStart(), "new BT connection established");
        /* don't check here with shouldConnectToMACPeer if to talk
         with remote peer.

         It must be checked before establishing a connection as client!
         Checked again here would fail - due to missing waiting period.

         Thus, this must be check in server socket as well. See comments there.

         more:
         There is also a race condition between both peers - who will create connection
         earlier - that mechanism drops the later connection attempt.
         */

        // set up new ASAP Session and we are done here.

        new ASAPSession(socket.getInputStream(), socket.getOutputStream(),
                this.getASAPService().getASAPEngine(), this).start();
    }
}
