package net.sharksystem.asap.android;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPReceivedChunkListener;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.MultiASAPEngineFS_Impl;
import net.sharksystem.asap.android.bluetooth.BluetoothEngine;
import net.sharksystem.asap.android.wifidirect.WifiP2PEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that searches and creates wifi p2p connections
 * to run an ASAP session.
 */

public class ASAPService extends Service implements ASAPReceivedChunkListener {
    private static final String LOGSTART = "ASAPService";
    private String asapEngineRootFolderName;

    //private ASAPEngine ASAPEngine = null;
    private MultiASAPEngineFS ASAPEngine;

    public static final String ROOT_FOLDER_NAME = "SHARKSYSTEM_ASAP";

    String getASAPRootFolderName() {
        return this.asapEngineRootFolderName;
    }

    public MultiASAPEngineFS getASAPEngine() {
        if(this.ASAPEngine == null) {
            Log.d(LOGSTART, "try to get ASAPEngine");

            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Log.d(LOGSTART,"no write permission!!");
                return null;
            }

            // we have write permissions

            // set up ASAPEngine
            File rootFolder = new File(this.asapEngineRootFolderName);
            try {
                if (!rootFolder.exists()) {
                    Log.d(LOGSTART,"createFolder");
                    rootFolder.mkdirs();
                    Log.d(LOGSTART,"createdFolder");
                }
                this.ASAPEngine = MultiASAPEngineFS_Impl.createMultiEngine(
                        this.asapEngineRootFolderName, this);
                Log.d(LOGSTART,"engine created");
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

        return this.ASAPEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 live cycle methods                               //
    //////////////////////////////////////////////////////////////////////////////////////

    // comes first
    public void onCreate() {
        Log.d(LOGSTART,"creating");

        String text = "started: ";

        Log.d(LOGSTART,text);

        // get root directory
        File ASAPRoot = null;
        ASAPRoot = Environment.getExternalStoragePublicDirectory(ROOT_FOLDER_NAME);

        this.asapEngineRootFolderName = ASAPRoot.getAbsolutePath();
        Log.d(LOGSTART,"work with folder: "
                + this.asapEngineRootFolderName);

        Log.d(LOGSTART, "created");
    }

    // comes second - could remove that overwriting method
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGSTART, "start");
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGSTART,"destroy");
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

        Log.d("ASAPService", "start bluetooth");
    }

    void stopBluetooth() {
        Log.d("ASAPService", "stop bluetooth");

        BluetoothEngine asapBluetoothEngine = BluetoothEngine.getASAPBluetoothEngine();
        if(asapBluetoothEngine != null) {
            asapBluetoothEngine.stop();
        }

        Log.d("ASAPService", "stop bluetooth");
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                          chunk receiving management                              //
    //////////////////////////////////////////////////////////////////////////////////////

    private boolean broadcastOn = false;
    private List<ASAPBroadcastIntent> chunkReceivedBroadcasts = new ArrayList<>();

    @Override
    public void chunkReceived(String sender, String uri, int era) {
        // issue broadcast
        ASAPBroadcastIntent intent = null;
        try {
            intent = new ASAPBroadcastIntent(
                    sender, this.getASAPRootFolderName(), uri, era);
        } catch (ASAPException e) {
            e.printStackTrace();
            return;
        }

        if(this.broadcastOn) {
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
        // flag can change while in that methode due to calls from other threads
        while(this.broadcastOn &&  this.chunkReceivedBroadcasts.size() > 0) {
            Log.d(LOGSTART, "send stored broadcast");
            this.sendBroadcast(chunkReceivedBroadcasts.remove(0));
        }
    }

    public void pauseBroadcasts() {
        Log.d(LOGSTART, "pauseBroadcasts");
        this.broadcastOn = false;
    }
}
