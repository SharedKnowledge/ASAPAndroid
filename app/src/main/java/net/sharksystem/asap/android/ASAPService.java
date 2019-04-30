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

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPReceivedChunkListener;
import net.sharksystem.asap.android.wifidirect.WifiP2PEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that searches and creates wifi p2p connections
 * to run an AASP session.
 */

public class ASAPService extends Service implements ASAPReceivedChunkListener {
    private static final String LOGSTART = "AASPService";
    private String aaspEngineRootFolderName;

    private ASAPEngine aaspEngine = null;
    public static final String ROOT_FOLDER_NAME = "SHARKSYSTEM_AASP";

    String getAASPRootFolderName() {
        return this.aaspEngineRootFolderName;
    }

    public ASAPEngine getAASPEngine() {
        if(this.aaspEngine == null) {
            Log.d(LOGSTART, "try to get AASPEngine");

            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Log.d(LOGSTART,"no write permission!!");
                return null;
            }

            // we have write permissions

            // set up AASPEngine
            File rootFolder = new File(this.aaspEngineRootFolderName);
            try {
                if (!rootFolder.exists()) {
                    Log.d(LOGSTART,"createFolder");
                    rootFolder.mkdirs();
                    Log.d(LOGSTART,"createdFolder");
                }
                this.aaspEngine = ASAPEngineFS.getASAPEngine(this.aaspEngineRootFolderName);
                Log.d(LOGSTART,"engine created");
            } catch (IOException e) {
                Log.d(LOGSTART,"IOException");
                Log.d(LOGSTART,e.getLocalizedMessage());
                e.printStackTrace();
            } catch (ASAPException e) {
                Log.d(LOGSTART,"AASPException");
                Log.d(LOGSTART,e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        return this.aaspEngine;
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
        File aaspRoot = null;
        aaspRoot = Environment.getExternalStoragePublicDirectory(ROOT_FOLDER_NAME);

        this.aaspEngineRootFolderName = aaspRoot.getAbsolutePath();
        Log.d(LOGSTART,"work with folder: "
                + this.aaspEngineRootFolderName);

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
        Log.d("AASPService", "start wifi p2p");
        WifiP2PEngine.getAASPWifiP2PEngine(this, this).start();
    }

    void stopWifiDirect() {
        Log.d("AASPService", "stop wifi p2p");
        WifiP2PEngine aaspWifiP2PEngine = WifiP2PEngine.getAASPWifiP2PEngine();
        if(aaspWifiP2PEngine != null) {
            aaspWifiP2PEngine.stop();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   Bluetooth                                      //
    //////////////////////////////////////////////////////////////////////////////////////

    void startBluetooth() {
        Log.d("AASPService", "start bluetooth");


        Log.d("AASPService", "start bluetooth - not yet fully implemented");
    }

    void stopBluetooth() {
        Log.d("AASPService", "stop bluetooth");
        Log.d("AASPService", "stop bluetooth - not yet implemented");
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
                    sender, this.getAASPRootFolderName(), uri, aaspEngine.getEra());
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
