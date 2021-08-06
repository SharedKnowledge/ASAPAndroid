package net.sharksystem.asap.android.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import net.sharksystem.asap.android.service.MacLayerEngine;
import net.sharksystem.asap.android.service.ASAPConnectionLauncher;
import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.util.tcp.TCPChannelMaker;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.getMainLooper;

import androidx.core.app.ActivityCompat;

public class WifiP2PEngine extends MacLayerEngine implements
        //WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.ChannelListener {

    private static WifiP2PEngine wifiP2PEngine = null;

    // https://developer.android.com/guide/topics/connectivity/wifip2p#java

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectBroadcastReceiver mReceiver;

    WifiP2PEngine(ASAPService ASAPService, Context context) {
        super(ASAPService, context);
    }

    WifiP2pManager getWifiP2pManager() {
        return this.mManager;
    }

    WifiP2pManager.Channel getWifiP2pChannel() {
        return this.mChannel;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 factory / singleton                              //
    //////////////////////////////////////////////////////////////////////////////////////

    public static WifiP2PEngine getASAPWifiP2PEngine(ASAPService ASAPService, Context context) {
        if (WifiP2PEngine.wifiP2PEngine == null) {
            WifiP2PEngine.wifiP2PEngine = new WifiP2PEngine(ASAPService, context);
        }

        return WifiP2PEngine.wifiP2PEngine;
    }

    /////////////// WifiP2pManager.ChannelListener - see mManager.initialize
    public void onChannelDisconnected() {
        Log.d(this.getLogStart(), "channel disconnected / restart wifip2p");
        // TODO: that's ok?
        this.restart();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 setup / shutdown                                 //
    //////////////////////////////////////////////////////////////////////////////////////

    private void shutdown() {
        if (this.mReceiver != null) {
            try {
                this.getContext().unregisterReceiver(this.mReceiver);
            } catch (RuntimeException e) {
                // ignore that one - happens when not registered
            }
            this.mReceiver = null;
        }

        if (this.mManager != null && this.mChannel != null) {
            try {
                this.mManager.cancelConnect(this.mChannel, null);
            } catch (RuntimeException e) {
            }

            try {
                this.mManager.removeGroup(this.mChannel, null);
            } catch (RuntimeException e) {
            }

            try {
                this.mManager.stopPeerDiscovery(this.mChannel, null);
            } catch (RuntimeException e) {
            }

            this.mManager = null;
        }

        /*
        if(this.mChannel != null) {
            try {
                this.mChannel.close(); // it does not work. why?
            }
            catch(RuntimeException e) {}
*/
        this.mChannel = null;
//        }
    }

    private void setup() {
        if (this.mManager == null) {
            // get P2P service on this device
            this.mManager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);

            // get access to p2p framework: TODO shouldn't we have our own looper here?
            WifiP2PChannelListener channelListener = new WifiP2PChannelListener(this);

            this.mChannel = this.mManager.initialize(
                    this.getContext(), getMainLooper(), channelListener);

            // create broadcast listener to get publications regarding wifi (p2p)
            WifiPeerListListener peerListListener = new WifiPeerListListener(this);
            WifiP2pManager.ConnectionInfoListener connectionInfoListener =
                    new WifiPeerConnectionInfoListener(this);

            this.mReceiver = new WifiDirectBroadcastReceiver(this,
                    this.mManager, this.mChannel, this.getContext(),
                    peerListListener,
                    connectionInfoListener);

            // define what broadcasts we are interested in
            IntentFilter mIntentFilter = new IntentFilter();

            // see broadcast receiver for details of those events
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            // register (subscribe) to broadcasts
            try {
                this.getContext().registerReceiver(this.mReceiver, mIntentFilter);
            } catch (RuntimeException e) {
                // can happen when already registered - to be sure..
                try {
                    this.getContext().unregisterReceiver(this.mReceiver);
                    this.getContext().registerReceiver(this.mReceiver, mIntentFilter);
                } catch (RuntimeException e2) {
                    // for gods sake - ignore that - have no idea whats going on here.
                }
            }
        }
    }

    public void start() {
        Log.d(this.getLogStart(), "start / start setup wifip2p");
        this.setup();
    }

    public void stop() {
        Log.d(this.getLogStart(), "stop / shutdown wifip2p");
        this.shutdown();
    }

    @Override
    public boolean tryReconnect() {
        Log.d(this.getLogStart(), "tryReconnect not implemented.");
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                               peer discovery launch                              //
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * called to start peer discovery
     */
    @SuppressLint("MissingPermission")
    public void discoverPeers() {
        if(!this.permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }

        this.mManager.discoverPeers(this.mChannel,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(getLogStart(), "wifi p2p discovery started");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(getLogStart(), "cannot start wifi p2p discovery failed");
                    }
                });
    }

    @Override
    public void checkConnectionStatus() {
        Log.d(this.getLogStart(), "not implemented: checkConnectionStatus");
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
