package net.sharksystem.asap.android.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.sharksystem.asap.ASAPChunkReceivedListener;
import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPOnlineMessageSender;
import net.sharksystem.asap.ASAPOnlineMessageSenderEngineSide;
import net.sharksystem.asap.ASAPOnlinePeersChangedListener;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.android.bluetooth.BluetoothEngine;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.asap.android.wifidirect.WifiP2PEngine;
import net.sharksystem.asap.util.Helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that searches and creates wifi p2p connections
 * to run an ASAP session.
 */

public class ASAPService extends Service implements ASAPChunkReceivedListener,
        ASAPOnlinePeersChangedListener {

    private String asapEngineRootFolderName;

    //private asapMultiEngine asapMultiEngine = null;
    private ASAPPeer asapMultiEngine;
    private ASAPOnlineMessageSender asapOnlineMessageSender;
    private CharSequence owner;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private long maxExecutionTime;
    private ArrayList<CharSequence> supportedFormats;

    String getASAPRootFolderName() {
        return this.asapEngineRootFolderName;
    }

    public ASAPPeer getASAPPeer() {
        Log.d(this.getLogStart(), "asap multi engine is a singleton.");
        if(this.asapMultiEngine == null) {
            Log.d(this.getLogStart(), "going to set up asapMultiEngine");

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

                this.asapMultiEngine = ASAPPeerFS.createASAPPeer(
                        this.owner, this.asapEngineRootFolderName,
                        this.maxExecutionTime, this.supportedFormats, this);

                Log.d(this.getLogStart(),"engines created");

                // listener for radar app
                this.asapMultiEngine.addOnlinePeersChangedListener(this);
                Log.d(this.getLogStart(),"added online peer changed listener");

                // add online feature to each engine
                this.asapMultiEngine.activateOnlineMessages();
                Log.d(this.getLogStart(),"online messages activated for ALL asap engines");

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
            Log.d(this.getLogStart(), "multi engine was already created");
        }

        return this.asapMultiEngine;
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
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getLogStart(), "onStartCommand");
        if(intent == null) {
            Log.d(this.getLogStart(), "intent is null");
            this.owner = ASAPAndroid.UNKNOWN_USER;
            this.rootFolder = ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;
            this.onlineExchange = ASAPAndroid.ONLINE_EXCHANGE_DEFAULT;
            this.maxExecutionTime = ASAPPeer.DEFAULT_MAX_PROCESSING_TIME;
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
                this.owner = ASAPAndroid.UNKNOWN_USER;
            }
            if(this.rootFolder == null || this.rootFolder.length() == 0) {
                Log.d(this.getLogStart(), "intent did not define root folder - set default:");
                this.rootFolder = ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;
            }
        }

        // get root directory
        File asapRoot = null;
        Log.d(this.getLogStart(), "use Util.getASAPRootDirectory()");
        asapRoot = Util.getASAPRootDirectory(this, this.rootFolder, this.owner);

        this.asapEngineRootFolderName = asapRoot.getAbsolutePath();
        Log.d(this.getLogStart(),"work with folder: " + this.asapEngineRootFolderName);

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
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

        BluetoothEngine.getASAPBluetoothEngine(this, this).start();

        Log.d("ASAPService", "started bluetooth");
    }

    void stopBluetooth() {
        Log.d("ASAPService", "stop bluetooth");

        BluetoothEngine asapBluetoothEngine =
                BluetoothEngine.getASAPBluetoothEngine(this, this);

        if(asapBluetoothEngine != null) {
            asapBluetoothEngine.stop();
        }

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

    private String getLogStart() {
        return Util.getLogStart(this);
    }


    //////////////////////////////////////////////////////////////////////////////////////
    //                                  status management                               //
    //////////////////////////////////////////////////////////////////////////////////////

    public void propagateProtocolStatus() throws ASAPException {
        BluetoothEngine.getASAPBluetoothEngine(this, this)
                .propagateStatus(this);
        // Wifi.propagateStatus();
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

    public ASAPOnlineMessageSender getASAPOnlineMessageSender() throws ASAPException {
        if(this.asapOnlineMessageSender == null) {
            if(this.asapMultiEngine == null) {
                throw new ASAPException("asap engine not initialized");
            }

            this.asapOnlineMessageSender =
                    new ASAPOnlineMessageSenderEngineSide(this.asapMultiEngine);
        }

        return this.asapOnlineMessageSender;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //                           ASAPOnlinePeersChangedListener                     //
    //////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onlinePeersChanged(ASAPPeer asapPeer) {
        Log.d(this.getLogStart(), "onlinePeersChanged");

        // broadcast
        String serializedOnlinePeers = Helper.collection2String(asapPeer.getOnlinePeers());
        Log.d(this.getLogStart(), "online peers serialized: " + serializedOnlinePeers);

        ASAPServiceRequestNotifyIntent intent =
                new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_ONLINE_PEERS_CHANGED);

        intent.putExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1, serializedOnlinePeers);

        this.sendBroadcast(intent);
    }
}
