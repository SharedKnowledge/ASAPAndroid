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
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.MultiASAPEngineFS_Impl;
import net.sharksystem.asap.android.ASAP;
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

    private static final String LOGSTART = "ASAPService";
    private String asapEngineRootFolderName;

    //private asapMultiEngine asapMultiEngine = null;
    private MultiASAPEngineFS asapMultiEngine;
    private ASAPOnlineMessageSender asapOnlineMessageSender;
    private CharSequence owner;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private long maxExecutionTime;

    String getASAPRootFolderName() {
        return this.asapEngineRootFolderName;
    }

    public MultiASAPEngineFS getMultiASAPEngine() {
        if(this.asapMultiEngine == null) {
            Log.d(LOGSTART, "try to get asapMultiEngine");

            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Log.d(LOGSTART,"no write permission!!");
                return null;
            }

            // we have write permissions

            // set up asapMultiEngine
            File rootFolder = new File(this.asapEngineRootFolderName);
            try {
                if (!rootFolder.exists()) {
                    Log.d(LOGSTART,"createFolder");
                    rootFolder.mkdirs();
                    Log.d(LOGSTART,"createdFolder");
                }
                this.asapMultiEngine = MultiASAPEngineFS_Impl.createMultiEngine(
                        this.owner, this.asapEngineRootFolderName, this.maxExecutionTime, this);
                Log.d(LOGSTART,"engine created");

                this.asapMultiEngine.addOnlinePeersChangedListener(this);
                Log.d(LOGSTART,"added online peer changed listener");

            } catch (IOException e) {
                Log.d(LOGSTART,"IOException");
                Log.d(LOGSTART,e.getLocalizedMessage());
                e.printStackTrace();
            } catch (ASAPException e) {
                Log.d(LOGSTART,"ASAPException");
                Log.d(LOGSTART,e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        return this.asapMultiEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 live cycle methods                               //
    //////////////////////////////////////////////////////////////////////////////////////

    // comes first
    public void onCreate() {
        super.onCreate();
        Log.d(LOGSTART,"onCreate");
    }

    // comes second - do initializing stuff here
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGSTART, "onStartCommand");
        if(intent == null) {
            Log.d(LOGSTART, "intent is null");
            this.owner = ASAP.UNKNOWN_USER;
            this.rootFolder = ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;
            this.onlineExchange = ASAP.ONLINE_EXCHANGE_DEFAULT;
            this.maxExecutionTime = MultiASAPEngineFS.DEFAULT_MAX_PROCESSING_TIME;
        } else {
            Log.d(LOGSTART, "service was created with an intent");

            ASAPServiceCreationIntent asapServiceCreationIntent =
                    new ASAPServiceCreationIntent(intent);

            Log.d(this.getLogStart(), "started with intent: "
                    + asapServiceCreationIntent.toString());

            this.owner = asapServiceCreationIntent.getOwner();
            this.rootFolder = asapServiceCreationIntent.getRootFolder();
            this.onlineExchange = asapServiceCreationIntent.isOnlineExchange();
            this.maxExecutionTime = asapServiceCreationIntent.getMaxExecutionTime();
        }

        // get root directory
        File asapRoot = null;
        //asapRoot = Environment.getExternalStoragePublicDirectory(this.rootFolder.toString());
        Log.d(LOGSTART, "use Util.getASAPRootDirectory()");
        asapRoot = Util.getASAPRootDirectory(this, this.rootFolder, this.owner);

        this.asapEngineRootFolderName = asapRoot.getAbsolutePath();
        Log.d(LOGSTART,"work with folder: " + this.asapEngineRootFolderName);

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGSTART,"onDestroy");
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger;

    public IBinder onBind(Intent intent) {
        Log.d(LOGSTART,"binding");

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
        Log.d(LOGSTART, "was notified by asap engine that chunk received - broadcast. Uri: "
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
            Log.d(LOGSTART, "send broadcast");
            this.sendBroadcast(intent);
        } else {
            // store it
            Log.d(LOGSTART, "store broadcast in list");
            this.chunkReceivedBroadcasts.add(intent);
        }
    }

    public void resumeBroadcasts() {
        Log.d(LOGSTART, "resumeBroadcasts");
        this.broadcastOn = true;

        int index = 0;
        // flag can change while in that method due to calls from other threads
        while(this.broadcastOn &&  this.chunkReceivedBroadcasts.size() > 0) {
            Log.d(LOGSTART, "send stored broadcast");
            this.sendBroadcast(chunkReceivedBroadcasts.remove(0));
        }
    }

    public void pauseBroadcasts() {
        Log.d(LOGSTART, "pauseBroadcasts");
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
    public void onlinePeersChanged(MultiASAPEngineFS multiASAPEngineFS) {
        Log.d(LOGSTART, "onlinePeersChanged");

        // broadcast
        String serializedOnlinePeers = Helper.collection2String(multiASAPEngineFS.getOnlinePeers());
        Log.d(this.getLogStart(), "online peers serialized: " + serializedOnlinePeers);

        ASAPServiceRequestNotifyIntent intent =
                new ASAPServiceRequestNotifyIntent(
                        ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_ONLINE_PEERS_CHANGED);

        intent.putExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1, serializedOnlinePeers);

        this.sendBroadcast(intent);
    }
}
