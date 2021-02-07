package net.sharksystem.asap.android.service;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Messenger;
import androidx.core.content.ContextCompat;
import android.util.Log;

import net.sharksystem.asap.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.ASAPException;
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
import net.sharksystem.asap.engine.ASAPChunkReceivedListener;
import net.sharksystem.asap.utils.Helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class controls layer 2 connections
 */

public class ASAPService extends Service
        implements ASAPChunkReceivedListener, ASAPEnvironmentChangesListener {

    /** time in minutes until a new connection attempt is made to an already paired device is made*/
    public static final int WAIT_MINUTES_UNTIL_TRY_RECONNECT = 1; // debugging
    //public static final int WAIT_UNTIL_TRY_RECONNECT = 30; // real life

    private String asapEngineRootFolderName;

    //private asapMultiEngine asapMultiEngine = null;
    private ASAPPeerFS asapPeer;
    private CharSequence owner;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private long maxExecutionTime;
    private ArrayList<CharSequence> supportedFormats;

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

            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Log.d(this.getLogStart(),"no write permission!!");
                return null;
            }

            // we have write permissions

            // set up asapMultiEngine
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

        // get root directory
        File asapRoot = null;
        Log.d(this.getLogStart(), "use Util.getASAPRootDirectory()");
        asapRoot = Util.getASAPRootDirectory(this, this.rootFolder, this.owner);

        this.asapEngineRootFolderName = asapRoot.getAbsolutePath();
        Log.d(this.getLogStart(),"work with folder: " + this.asapEngineRootFolderName);

        this.startReconnectPairedDevices();

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
        Log.d("ASAPService", "start wifi p2p");
        WifiP2PEngine.getASAPWifiP2PEngine(this, this).start();
    }

    void stopWifiDirect() {
        Log.d("ASAPService", "stop wifi p2p");
        WifiP2PEngine ASAPWifiP2PEngine = WifiP2PEngine.getASAPWifiP2PEngine();
        if(ASAPWifiP2PEngine != null) {
            ASAPWifiP2PEngine.stop();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   Bluetooth                                      //
    //////////////////////////////////////////////////////////////////////////////////////

    void startBluetooth() {
        Log.d("ASAPService", "start bluetooth");

        BluetoothEngine btEngine = BluetoothEngine.getASAPBluetoothEngine(this, this);
        btEngine.start();

        Log.d(this.getLogStart(), "start reconnect thread");
        this.startReconnectPairedDevices();

        Log.d("ASAPService", "started bluetooth");
    }

    void stopBluetooth() {
        Log.d("ASAPService", "stop bluetooth");

        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        if(asapBluetoothEngine != null) {
            asapBluetoothEngine.stop();
        }

        Log.d(this.getLogStart(), "stop reconnect thread");
        this.stopReconnectPairedDevices();

        Log.d("ASAPService", "stopped bluetooth");
    }

    void startBluetoothDiscoverable() {
        Log.d("ASAPService", "start bluetooth discoverable");

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
        public static final int MAX_FAILATTEMPTS = 3;

        void terminate() { this.terminated = true; }

        public void run() {
            Log.d(ASAPService.this.getLogStart(), "start new ReconnectTriggerThread");
            int failedAttemptsCounter = 0;

            while (!this.terminated) {
                if(!ASAPService.this.tryReconnect()) {
                    failedAttemptsCounter++;
                    if(failedAttemptsCounter == MAX_FAILATTEMPTS) {
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
    public void chunkReceived(String format, String sender, String uri, int era) {
        Log.d(this.getLogStart(), "was notified by asap engine that chunk received - broadcast. Uri: "
                + uri);
        // issue broadcast
        ASAPChunkReceivedBroadcastIntent intent = null;
        try {
            intent = new ASAPChunkReceivedBroadcastIntent(
                    format, sender, this.getASAPRootFolderName(), uri, era);
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
        String serializedOnlinePeers = Helper.collection2String(onlinePeers);
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
