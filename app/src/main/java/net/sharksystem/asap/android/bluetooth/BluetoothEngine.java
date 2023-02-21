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
import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.asap.android.service.MacLayerEngine;
import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;

//https://developer.android.com/guide/topics/connectivity/bluetooth#java

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

    public static BluetoothEngine getASAPBluetoothEngine(ASAPService asapService,
                                                         Context context) {
        if(BluetoothEngine.engine == null) {
            BluetoothEngine.engine = new BluetoothEngine(asapService, context);
        }

        return BluetoothEngine.engine;
    }

    private BluetoothEngine(ASAPService ASAPService, Context context) {
        super(ASAPService, context);

        // get default bt adapter
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i(this.getLogStart(), "device does not support bluetooth - give up");
        }

        // auto start
        if(this.mBluetoothAdapter.isEnabled()) {
            Log.i(this.getLogStart(), "Bluetooth is on - activate ASAP bluetooth stuff");
            this.start();
        }
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
        try {
            this.shutdown();
        } catch (ASAPException e) {
            Log.e(this.getLogStart(), "could not shutdown bt: " + e.getLocalizedMessage());
        }
    }

    public boolean started() {
        return this.btEnvironmentOn;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //                             do the real bluetooth stuff                     //
    /////////////////////////////////////////////////////////////////////////////////

    /**
     * Setup Bluetooth environment. Note: Only the environment is created.
     * Discovery and discoverability is not initiated.
     */
    private void setup() {
        ///////////////////////////////////////////////////////////////////////////////////////
        //                                 setup bt environment                              //
        ///////////////////////////////////////////////////////////////////////////////////////

        // reference was set in constructor - or not
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i(this.getLogStart(), "device does not support bluetooth - give up");
            return;
        }

        // adapter enabled? if not - ask activity to ask user to enable
        Log.d(this.getLogStart(), "check if BT is enabled");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(this.getLogStart(),
                    "Bluetooth disabled - ask application to start BT - stop setting up so far");

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


    void doServerAcceptSocketKilled() {
        Log.d(this.getLogStart(), "was told accept socket died - shutdown");
        // server socket was killed - that most probably because BT was switched off by users
        try {
            this.shutdown();
        } catch (ASAPException e) {
            Log.d(this.getLogStart(), "problems with shutdown: " + e.getLocalizedMessage());
        }

        /* BT on Android shows that weired behaviour that process can write into a BT socket
        even when BT is off. That's impossible - but kill all sockets they are only zombies
         */

        for(BluetoothSocket socket : this.openSockets.values()) {
            String name = "no remote device";
            String address = name;
            BluetoothDevice remoteDevice = socket.getRemoteDevice();
            if( remoteDevice != null) {
                name = remoteDevice.getName();
                address = remoteDevice.getAddress();
            }

            Log.d(this.getLogStart(), "remove connection from list. "
                    + "name: " + name
                    + " | address: " + address
                    + " | isConnected: " + socket.isConnected());
            try {
                this.kill(address); // kill connection that runs on top of it
                socket.getInputStream().close(); // and underlying sockets
                socket.getOutputStream().close();
            } catch (IOException e) {
                Log.d(this.getLogStart(), "could not close");
            }
        }

        this.openSockets = new HashMap<>();
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
    public boolean startDiscovery() throws ASAPException {
        if(this.getBTAdapter().startDiscovery()) {
            Log.d(this.getLogStart(), "successfully started Bluetooth discovery");
            return true;
        } else {
            Log.e(this.getLogStart(), "could not start Bluetooth discovery");
            return false;
        }
    }

    private void shutdown() throws ASAPException {
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
        this.getBTAdapter().cancelDiscovery();
        this.getBTAdapter().disable();

        this.btEnvironmentOn = false;

        // broadcast the news
        this.getContext().sendBroadcast(
            new ASAPServiceRequestNotifyIntent(
                ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED));
    }

    BluetoothAdapter getBTAdapter() throws ASAPException {
        if(this.mBluetoothAdapter == null)
            throw new ASAPException("bluetooth not yet initialized");

        Log.d(this.getLogStart(), "my mac address: " + this.mBluetoothAdapter.getAddress());
        return this.mBluetoothAdapter;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //                       handle connections and connection attempts                    //
    /////////////////////////////////////////////////////////////////////////////////////////

    public static final int BT_MAJOR_CLASS_PHONE = 0x00000200;
    public static final int BT_MAJOR_CLASS_COMPUTER = 0x00000100;

    void tryConnect(BluetoothDevice btDevice, BluetoothClass btClass) throws ASAPException {
        btClass = btDevice.getBluetoothClass();
        String macAddress = btDevice.getAddress(); // MAC address

        int btClassMajorNumber = btClass.getMajorDeviceClass();

        StringBuilder sb = new StringBuilder();
        sb.append("device found: ");
        sb.append("mac: ");
        sb.append(macAddress);
        sb.append("|name: ");
        sb.append(btDevice.getName());
        sb.append("|btClass: ");
        if ((btClass != null)) {
            sb.append(String.format("0x%08X", btClass.getDeviceClass()));
            sb.append(" (major: ");
            sb.append(String.format("0x%08X", btClassMajorNumber));
            sb.append(") ");
        } else {
            sb.append("non ");
        }
        sb.append("|btUuids: ");
        sb.append(btDevice.getUuids());
        Log.d(this.getLogStart(), sb.toString());

        if( btClassMajorNumber == BT_MAJOR_CLASS_PHONE) {
            Log.d(this.getLogStart(), "it's a phone");
        } else if( btClassMajorNumber ==  BT_MAJOR_CLASS_COMPUTER) {
            Log.d(this.getLogStart(), "it's a computer");
        } else {
            Log.d(this.getLogStart(), "THIS VERSION only tries to contact " +
                    "PHONES or COMPUTER. This device is neither - ignore it.");
            return;
        }

        // strongly recommended to stop discovery
        //this.mBluetoothAdapter.cancelDiscovery();

        // already connected?
        if(this.checkAlreadyConnectedWithDevice(macAddress)) return;

        if(this.shouldConnectToMACPeer(macAddress)) {
            Log.d(this.getLogStart(), "create a BT client socket thread");
            new BluetoothClientSocketThread(this, btDevice).start();
        } else {
            Log.d(this.getLogStart(), "should and will not connect to that device: "
                    + macAddress);
        }
    }

    private boolean checkAlreadyConnectedWithDevice(String macAddress) {
        BluetoothSocket bluetoothSocket = this.openSockets.get(macAddress);
        if(bluetoothSocket != null) {
            if(bluetoothSocket.isConnected()) {
                Log.d(this.getLogStart(), "we already have an open and active connection to "
                        + macAddress + " | " + bluetoothSocket);
                return true;
            } else {
                // there is an entry but not connected anymore (?)
                Log.d(this.getLogStart(), "remove socket - it is not longer (?) connected "
                        + bluetoothSocket);
                this.openSockets.remove(macAddress);
            }
        }

        return false;
    }

    @Override
    public boolean tryReconnect() {
        if(!this.started()) {
            Log.d(this.getLogStart(), "don't try to reconnect: BT is not running");
            return false;
        }

        boolean anySuccess = false;

        // stolen from: https://developer.android.com/guide/topics/connectivity/bluetooth#java
        Set<BluetoothDevice> pairedDevices = this.mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice btDevice : pairedDevices) {
                String deviceName = btDevice.getName();
                String deviceHardwareAddress = btDevice.getAddress(); // MAC address

                try {
                    this.tryConnect(btDevice, btDevice.getBluetoothClass());
                    anySuccess = true; // at least no exception
                } catch (ASAPException e) {
                    Log.d(this.getLogStart(), "problems reconnecting: "
                            + e.getLocalizedMessage());
                }
            }
        }

        return anySuccess;
    }

    private Map<String, BluetoothSocket> openSockets = new HashMap<>();

    @Override
    public void checkConnectionStatus() {
        Log.d(this.getLogStart(), "check connection status");
        // some structures are sensitive about changes when their are used
        List<String> names2remove = new ArrayList<>();

        for(String connectionName : this.openSockets.keySet()) {
            BluetoothSocket bluetoothSocket = this.openSockets.get(connectionName);
            StringBuilder sb = new StringBuilder();
            sb.append("name: ");
            sb.append(connectionName);
            sb.append(" | isConnected: ");
            sb.append(bluetoothSocket.isConnected());
            sb.append(" | socket: ");
            sb.append(bluetoothSocket);

            Log.d(this.getLogStart(), sb.toString());

            if(!bluetoothSocket.isConnected()) {
                try {
                    names2remove.add(connectionName);
                    bluetoothSocket.close(); // not necessary but better save than sorry
                    Log.d(this.getLogStart(), "removed closed socket to " + connectionName);
                } catch (IOException e) {
                    Log.d(this.getLogStart(),
                            "checkConnectionStatus - closing an unconnected socket: "
                                    + e.getLocalizedMessage());
                }
            }
        }

        // remove
        for(String name : names2remove) {
            this.openSockets.remove(name);
        }
    }

    /**
     * Both client and server sockets
     * @param socket
     * @param initiator for debugging: who is calling client socket / server socket?
     * @throws IOException
     */
    void handleBTSocket(BluetoothSocket socket, boolean initiator) throws IOException {
        String remoteMacAddress = socket.getRemoteDevice().getAddress();

        StreamPair streamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
                socket.getInputStream(), socket.getOutputStream(), remoteMacAddress);

        this.getASAPService().getASAPEncounterManager().handleEncounter(
                streamPair, EncounterConnectionType.AD_HOC_LAYER_2_NETWORK, initiator);
    }

    public void propagateStatus(Context ctx) throws ASAPException {
        Log.d(this.getLogStart(), "going to send status broadcast messages");
        ASAPServiceRequestNotifyIntent notifyIntent = null;

        // Bluetooth running?
        if(this.getBTAdapter().isEnabled()) {
             notifyIntent = new ASAPServiceRequestNotifyIntent(
                            ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STARTED);
        } else {
            notifyIntent = new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED);
        }

        ctx.sendBroadcast(notifyIntent);

        if(this.getBTAdapter().isDiscovering()) {
            notifyIntent = new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STARTED);
        } else {
            notifyIntent = new ASAPServiceRequestNotifyIntent(
                    ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STOPPED);
        }

        ctx.sendBroadcast(notifyIntent);
    }
}
