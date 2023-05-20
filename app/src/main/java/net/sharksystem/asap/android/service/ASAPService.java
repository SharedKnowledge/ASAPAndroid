package net.sharksystem.asap.android.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.ASAPEncounterManagerImpl;
import net.sharksystem.asap.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.ASAPPeerService;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.android.bluetooth.BluetoothEngine;
import net.sharksystem.asap.android.lora.LoRaEngine;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.asap.android.wifidirect.WifiP2PEngine;
import net.sharksystem.asap.engine.ASAPChunkAssimilatedListener;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.HubConnectionManagerMessageHandler;
import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.hub.peerside.ASAPHubManagerImpl;
import net.sharksystem.utils.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class controls layer 2 connections
 */

public class ASAPService extends Service
        implements ASAPChunkAssimilatedListener, ASAPEnvironmentChangesListener {

    /** time in minutes until a new connection attempt is made to an already paired device is made*/
    public static final int WAIT_MINUTES_UNTIL_TRY_RECONNECT = 1; // debugging: TODO
    //public static final int WAIT_UNTIL_TRY_RECONNECT = 30; // real life

    private String asapEngineRootFolderName;

    private ASAPPeerFS asapPeer;
    private ASAPEncounterManager asapEncounterManager;
    private CharSequence owner;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private long maxExecutionTime;
    private ArrayList<CharSequence> supportedFormats;
    private ASAPHubManager asapASAPHubManager;
    private HubConnectionManagerMessageHandler hubConnectionManager;

    String getASAPRootFolderName() {
        return this.asapEngineRootFolderName;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 construction                                     //
    //////////////////////////////////////////////////////////////////////////////////////

    public ASAPPeerFS getASAPPeer() {
        Log.d(this.getLogStart(), "asap peer is a singleton.");
        if(this.asapPeer == null) {
            Log.d(this.getLogStart(), "going to set up asapPeer");

            /*
            we are beyond level 19
            https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE
            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Log.e(this.getLogStart(),"SERIOUS: no write permission!!");
                return null;
            }
             */

            // we have write permissions

            // set up ASAPPeer
            File rootFolder = new File(this.asapEngineRootFolderName);
            try {
                if (!rootFolder.exists()) {
                    Log.d(this.getLogStart(),"root folder does not exist - create");
                    rootFolder.mkdirs();
                    Log.d(this.getLogStart(),"done creating root folder");
                }

                this.asapPeer = new ASAPPeerFS(this.owner, this.asapEngineRootFolderName,
                        this.supportedFormats);

                // overwrite chunk received listener
                this.asapPeer.overwriteChuckReceivedListener(this);

                Log.d(this.getLogStart(),"peer service side created");

                // listener for radar app
                this.asapPeer.addASAPEnvironmentChangesListener(this);
                //this.asapPeer.addOnlinePeersChangedListener(this);
                Log.d(this.getLogStart(),"added environment changes listener");

            } catch (IOException e) {
                Log.d(this.getLogStart(),"IOException");
                Log.d(this.getLogStart(),e.getLocalizedMessage());
                e.printStackTrace();
            } catch (ASAPException e) {
                Log.d(this.getLogStart(),"ASAPException");
                Log.d(this.getLogStart(),e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else {
            Log.d(this.getLogStart(), "peer was already created");
        }

        return this.asapPeer;
    }

    public synchronized ASAPEncounterManager getASAPEncounterManager()  {
        if(this.asapEncounterManager == null) {
            this.asapEncounterManager = new ASAPEncounterManagerImpl(this.getASAPPeer());
        }

        return this.asapEncounterManager;
    }

    public synchronized HubConnectionManagerMessageHandler getHubConnectionManager()  {
        if(this.hubConnectionManager == null) {
            this.hubConnectionManager =
                    new HubConnectionManagerServiceSide(
                            this.getASAPEncounterManager(),
                            this.getASAPPeer());
        }

        return this.hubConnectionManager;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 live cycle methods                               //
    //////////////////////////////////////////////////////////////////////////////////////

    // comes first
    public void onCreate() {
        super.onCreate();
        Log.d(this.getLogStart(),"onCreate");
    }

    // comes second - do initializing stuff here

    /**
     * Set parameters and prepare environment to create an ASAPPeer on service side
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getLogStart(), "onStartCommand");
        if(intent == null) {
            Log.d(this.getLogStart(), "intent is null");
            this.owner = ASAPPeer.UNKNOWN_USER;
            this.rootFolder = ASAPPeerFS.DEFAULT_ROOT_FOLDER_NAME;
            this.onlineExchange = ASAPPeer.ONLINE_EXCHANGE_DEFAULT;
            this.maxExecutionTime = ASAPPeerService.DEFAULT_MAX_PROCESSING_TIME;
        } else {
            Log.d(this.getLogStart(), "service was created with an intent");

            ASAPServiceCreationIntent asapServiceCreationIntent =
                    new ASAPServiceCreationIntent(intent);

            Log.d(this.getLogStart(), "started with intent: "
                    + asapServiceCreationIntent.toString());

            this.owner = asapServiceCreationIntent.getOwner();
            this.rootFolder = asapServiceCreationIntent.getRootFolder();
            this.onlineExchange = asapServiceCreationIntent.isOnlineExchange();
            this.maxExecutionTime = asapServiceCreationIntent.getMaxExecutionTime();
            this.supportedFormats = asapServiceCreationIntent.getSupportedFormats();

            // set defaults if null
            if(this.owner == null || this.owner.length() == 0) {
                Log.d(this.getLogStart(), "intent did not define owner - set default:");
                this.owner = ASAPPeer.UNKNOWN_USER;
            }
            if(this.rootFolder == null || this.rootFolder.length() == 0) {
                Log.d(this.getLogStart(), "intent did not define root folder - set default:");
                this.rootFolder = ASAPPeerFS.DEFAULT_ROOT_FOLDER_NAME;
            }
        }

        // get root directory - assumed that application side has sent a valid folder name
        File asapRoot = new File(this.rootFolder.toString());

        /*
        Log.d(this.getLogStart(), "use Util.getASAPRootDirectory()");
        asapRoot = Util.getASAPRootDirectory(this, this.rootFolder, this.owner);
         */

        this.asapEngineRootFolderName = asapRoot.getAbsolutePath();
        Log.d(this.getLogStart(),"work with folder: " + this.asapEngineRootFolderName);

        //this.startReconnectPairedDevices(); // TODO good idea - crashes emulators

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();

        this.stopReconnectPairedDevices();

        Log.d(this.getLogStart(),"onDestroy");
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger;

    public IBinder onBind(Intent intent) {
        Log.d(this.getLogStart(),"binding");

        // create handler
        this.mMessenger = new Messenger(new ASAPMessageHandler(this));

        // return binder interface
        return mMessenger.getBinder();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   Wifi Direct                                    //
    //////////////////////////////////////////////////////////////////////////////////////

    void startWifiDirect() {
        Log.d(this.getLogStart(), "start wifi p2p");
        WifiP2PEngine.getASAPWifiP2PEngine(this, this).start();
    }

    void stopWifiDirect() {
        Log.d(this.getLogStart(), "stop wifi p2p");
        WifiP2PEngine ASAPWifiP2PEngine =
                WifiP2PEngine.getASAPWifiP2PEngine(this, this);
        if(ASAPWifiP2PEngine != null) {
            ASAPWifiP2PEngine.stop();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   Bluetooth                                      //
    //////////////////////////////////////////////////////////////////////////////////////

    void startBluetooth() {
        Log.d(this.getLogStart(), "start bluetooth");

        BluetoothEngine btEngine = BluetoothEngine.getASAPBluetoothEngine(this, this);
        btEngine.start();

        Log.d(this.getLogStart(), "start reconnect thread");
        this.startReconnectPairedDevices();

        Log.d(this.getLogStart(), "started bluetooth");
    }

    void stopBluetooth() {
        Log.d(this.getLogStart(), "stop bluetooth");

        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        if(asapBluetoothEngine != null) {
            asapBluetoothEngine.stop();
        }

        Log.d(this.getLogStart(), "stop reconnect thread");
        this.stopReconnectPairedDevices();

        Log.d(this.getLogStart(), "stopped bluetooth");
    }

    void startBluetoothDiscoverable() {
        Log.d(this.getLogStart(), "start bluetooth discoverable");

        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        asapBluetoothEngine.startDiscoverable();

        Log.d(this.getLogStart(), "started bluetooth discoverable");
    }


    public void startBluetoothDiscovery() throws ASAPException {
        Log.d(this.getLogStart(), "start bluetooth discovery");

        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        if(asapBluetoothEngine.startDiscovery()) {
            Log.d(this.getLogStart(), "started bluetooth discovery");
        } else {
            Log.d(this.getLogStart(), "starting bluetooth discovery failed");
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                  LoRa via BT                                     //
    //////////////////////////////////////////////////////////////////////////////////////

    void startLoRa() {
        Log.d(this.getLogStart(), "start LoRa");

        // Helper for finding an ASAPLoRaBTModule
        //TODO - actually there should be a Setting or selection Dialog here
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        btAdapter.enable();
        btAdapter.cancelDiscovery();

        for (BluetoothDevice btDevice : btAdapter.getBondedDevices()) {
            if (btDevice.getName().indexOf("ASAP-LoRa") == 0) {
                LoRaEngine.getASAPLoRaEngine(this, this).setAsapLoRaBTModule(btDevice);
                break;
            }
        }
        // End helper for finding an ASAPLoRaBTModule

        LoRaEngine.getASAPLoRaEngine(this, this).start();
    }

    void stopLoRa() {
        Log.d(this.getLogStart(), "start LoRa");
        LoRaEngine ASAPLoRaEngine = LoRaEngine.getASAPLoRaEngine();
        if(ASAPLoRaEngine != null) {
            ASAPLoRaEngine.stop();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                  ASAP hub management                             //
    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * timeouts inside hub - especially how long can a data connection be dead before killed
     */
    public static final int DEFAULT_TIMEOUT_ASAP_HUB_MILLIS = 5000;

    /**
     * hub manager connects hub and creates peer encounter. This happens in
     * frequent intervals. That is the default interval.
     */
    public static final int DEFAULT_WAIT_INTERVALS_ASAP_HUB_SECONDS = 30;
    public ASAPHubManager getASAPHubManager() {
        synchronized(this) {
            if (this.asapASAPHubManager == null) {
                this.asapASAPHubManager = ASAPHubManagerImpl.createASAPHubManager(
                        this.getASAPEncounterManager(),
                        DEFAULT_WAIT_INTERVALS_ASAP_HUB_SECONDS);
            }
        }
        return this.asapASAPHubManager;
    }

    private synchronized void stopASAPHubManager() {
        synchronized (this) {
            if (this.asapASAPHubManager != null) {
                this.asapASAPHubManager.kill();
                this.asapASAPHubManager = null;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   hub management                                 //
    //////////////////////////////////////////////////////////////////////////////////////

    public void refreshHubConnectionList(boolean defaultMultichannel) {
        // TODO
    }

    /*
    void connectASAPHubs(boolean multichannel) {
        Log.d(this.getLogStart(), "connect hubs called");
        // create a wrapper around our ASAPPeer
        SharkPeerBasic sharkPeerBasic = new SharkPeerBasicImpl(this.getASAPPeer());
        Collection<HubConnectorDescription> hubDescriptions = sharkPeerBasic.getHubDescriptions();
        if(hubDescriptions == null || hubDescriptions.isEmpty()) {
            Log.d(this.getLogStart(), "no hub descriptions - no hub connector to start");
            return;
        }

        // there is at least one hub description
        Log.d(this.getLogStart(), "get hub manager reference");
        ASAPHubManager asapHubManager = this.getASAPHubManager();

        Log.d(this.getLogStart(),
                "try to establish hub connection(s) - multichannel " + multichannel);

        asapHubManager.connectASAPHubs(hubDescriptions, this.getASAPPeer(), multichannel);

        ASAPServiceRequestNotifyIntent intent =
                new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_HUBS_CONNECTED);

        this.sendBroadcast(intent);
    }
     */

    void disconnectASAPHubs() {
        this.getASAPHubManager().disconnectASAPHubs();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                  status management                               //
    //////////////////////////////////////////////////////////////////////////////////////

    public void propagateProtocolStatus() throws ASAPException {
        BluetoothEngine.getASAPBluetoothEngine(this, this)
                .propagateStatus(this);
        // Wifi.propagateStatus();
    }

    private void checkLayer2ConnectionStatus() {
        // force layer 2 engine to check their connection status
        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        asapBluetoothEngine.checkConnectionStatus();
    }

    private ReconnectTrigger reconnectTrigger = null;
    public void startReconnectPairedDevices() {
        if(this.reconnectTrigger != null) {
            this.reconnectTrigger.terminate();
        }

        this.reconnectTrigger = new ReconnectTrigger();
        this.reconnectTrigger.start();
    }

    public void stopReconnectPairedDevices() {
        if(this.reconnectTrigger != null) {
            this.reconnectTrigger.terminate();
            this.reconnectTrigger = null;
        }
    }

    private boolean tryReconnect() {
        Log.d(this.getLogStart(), "try to reconnect with paired devices");

        BluetoothEngine btEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        if(btEngine != null) {
            return btEngine.tryReconnect();
        }

        return false;
    }

    private class ReconnectTrigger extends Thread {
        private boolean terminated = false;
        public static final int MAX_FAILED_ATTEMPTS = 3;

        void terminate() { this.terminated = true; }

        public void run() {
            Log.d(ASAPService.this.getLogStart(), "start new ReconnectTriggerThread");
            int failedAttemptsCounter = 0;

            while (!this.terminated) {
                if(!ASAPService.this.tryReconnect()) {
                    failedAttemptsCounter++;
                    if(failedAttemptsCounter == MAX_FAILED_ATTEMPTS) {
                        this.terminate();
                        break;
                    }
                }

                try {
                    Thread.sleep(ASAPService.WAIT_MINUTES_UNTIL_TRY_RECONNECT * 1000 * 60);
                } catch (InterruptedException e) {
                    // ignore this and go ahead
                }
            }
            Log.d(ASAPService.this.getLogStart(), "ReconnectTriggerThread terminated");
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                          chunk receiving management                              //
    //////////////////////////////////////////////////////////////////////////////////////

    private boolean broadcastOn = false;
    private List<ASAPChunkReceivedBroadcastIntent> chunkReceivedBroadcasts = new ArrayList<>();

    @Override
    public void chunkStored(String format, String senderE2E, String uri, int era, // E2E part
                              List<ASAPHop> asapHops) {

        Log.d(this.getLogStart(), "was notified by asap engine that chunk received - broadcast. Uri: "
                + uri);
        // issue broadcast
        ASAPChunkReceivedBroadcastIntent intent = null;
        try {
            intent = new ASAPChunkReceivedBroadcastIntent(
                    format, senderE2E, this.getASAPRootFolderName(), uri, era, asapHops);
        } catch (ASAPException e) {
            e.printStackTrace();
            return;
        }

        if(this.broadcastOn) {
            Log.d(this.getLogStart(), "send broadcast");
            this.sendBroadcast(intent);
        } else {
            // store it
            Log.d(this.getLogStart(), "store broadcast in list");
            this.chunkReceivedBroadcasts.add(intent);
        }
    }


    @Override
    public void transientMessagesReceived(ASAPMessages asapMessages, ASAPHop asapHop) throws IOException {
        // TODO urgent
        Log.e(this.getLogStart(), "transientMessagesReceived not yet implemented");

    }


    public void resumeBroadcasts() {
        Log.d(this.getLogStart(), "resumeBroadcasts");
        this.broadcastOn = true;

        int index = 0;
        // flag can change while in that method due to calls from other threads
        while(this.broadcastOn &&  this.chunkReceivedBroadcasts.size() > 0) {
            Log.d(this.getLogStart(), "send stored broadcast");
            this.sendBroadcast(chunkReceivedBroadcasts.remove(0));
        }
    }

    public void pauseBroadcasts() {
        Log.d(this.getLogStart(), "pauseBroadcasts");
        this.broadcastOn = false;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //                           ASAPOnlinePeersChangedListener                     //
    //////////////////////////////////////////////////////////////////////////////////

    private int numberOnlinePeers = 0;

    @Override
    public void onlinePeersChanged(Set<CharSequence> onlinePeers) {
        Log.d(this.getLogStart(), "onlinePeersChanged");

        // broadcast
        String serializedOnlinePeers = SerializationHelper.collection2String(onlinePeers);
        Log.d(this.getLogStart(), "online peers serialized: " + serializedOnlinePeers);

        ASAPServiceRequestNotifyIntent intent =
                new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_ONLINE_PEERS_CHANGED);

        intent.putExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1, serializedOnlinePeers);

        this.sendBroadcast(intent);

        this.checkLayer2ConnectionStatus();

        // force reconnection - to re-establish a accidentally broken connection
        int onlinePeerPreviously = this.numberOnlinePeers;
        Log.d(this.getLogStart(), "formed number of online peers: " + onlinePeerPreviously);
        if(onlinePeers != null) {
            this.numberOnlinePeers = onlinePeers.size();
            Log.d(this.getLogStart(), "current number of online peers: "
                    + this.numberOnlinePeers);

            if(this.numberOnlinePeers < onlinePeerPreviously) {
                this.startReconnectPairedDevices();
            }
        }
    }

    private String getLogStart() {
        return Util.getLogStart(this);
    }
}
