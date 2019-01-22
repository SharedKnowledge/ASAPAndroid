package net.sharksystem.aasp.android;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import net.sharksystem.aasp.android.wifidirect.AASPWifiP2PEngine;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3EngineFS;
import net.sharksystem.asp3.ASP3Exception;

import java.io.File;
import java.io.IOException;

/**
 * Service that searches and creates wifi p2p connections
 * to run an AASP session.
 */

public class AASPService extends Service {
    private String aaspEngineRootFolderName;

    private ASP3Engine aaspEngine = null;
    public static final String ROOT_FOLDER_NAME = "SHARKSYSTEM_AASP";

    String getAASPRootFolderName() {
        return this.aaspEngineRootFolderName;
    }

    ASP3Engine getAASPEngine() {
        if(this.aaspEngine == null) {
            Toast.makeText(getApplicationContext(), "try to get AASPEngine", Toast.LENGTH_LONG).show();

            // check write permissions
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                Toast.makeText(getApplicationContext(), "no write permission!!", Toast.LENGTH_SHORT).show();
                return null;
            }

            // we have write permissions

            // set up AASPEngine
            File rootFolder = new File(this.aaspEngineRootFolderName);
            try {
                if (!rootFolder.exists()) {
                    Toast.makeText(getApplicationContext(), "createFolder", Toast.LENGTH_SHORT).show();
                    rootFolder.mkdirs();
                    Toast.makeText(getApplicationContext(), "createdFolder", Toast.LENGTH_SHORT).show();
                }
                this.aaspEngine = ASP3EngineFS.getASP3Engine(this.aaspEngineRootFolderName);
                Toast.makeText(getApplicationContext(), "engine created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (ASP3Exception e) {
                Toast.makeText(getApplicationContext(), "ASP3Exception", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "creating", Toast.LENGTH_SHORT).show();

        String text = "started: ";

        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

        // get root directory
        File aaspRoot = null;
        aaspRoot = Environment.getExternalStoragePublicDirectory(ROOT_FOLDER_NAME);

        this.aaspEngineRootFolderName = aaspRoot.getAbsolutePath();
        Toast.makeText(getApplicationContext(), "work with folder: "
                + this.aaspEngineRootFolderName, Toast.LENGTH_SHORT).show();

        Toast.makeText(getApplicationContext(), "created", Toast.LENGTH_SHORT).show();
    }

    // comes second - could remove that overwriting method
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "start", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "destroy", Toast.LENGTH_SHORT).show();
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger;

    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        // create handler
        this.mMessenger = new Messenger(new AASPMessageHandler(this));

        // return binder interface
        return mMessenger.getBinder();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                   Wifi Direct                                    //
    //////////////////////////////////////////////////////////////////////////////////////

    void startWifiDirect() {
        AASPWifiP2PEngine.getAASPWifiP2PEngine(this, this).start();
    }

    void stopWifiDirect() {
        AASPWifiP2PEngine aaspWifiP2PEngine = AASPWifiP2PEngine.getAASPWifiP2PEngine();
        if(aaspWifiP2PEngine != null) {
            aaspWifiP2PEngine.stop();
        }
    }
}
