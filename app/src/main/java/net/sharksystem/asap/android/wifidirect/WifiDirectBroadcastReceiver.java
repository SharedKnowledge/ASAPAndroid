package net.sharksystem.asap.android.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    // https://developer.android.com/guide/topics/connectivity/wifip2p#java

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private final Context context;
    private final WifiP2pManager.PeerListListener peerListListener;
    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    private final WifiP2PEngine asapWifiP2PEngine;

    public WifiDirectBroadcastReceiver(
            WifiP2PEngine asapWifiP2PEngine,
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            Context context,
            WifiP2pManager.PeerListListener peerListListener,
            WifiP2pManager.ConnectionInfoListener connectionInfoListener
    ) {
        super();
        this.asapWifiP2PEngine = asapWifiP2PEngine;
        this.mManager = manager;
        this.mChannel = channel;
        this.context = context;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = connectionInfoListener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        // https://developer.android.com/guide/topics/connectivity/wifip2p#java

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d("Wifi_BR", "BL: wifi p2p enabled");

                // discoverPeers peers
                this.asapWifiP2PEngine.discoverPeers();
            } else {
                // Wi-Fi P2P is not enabled
                Log.d("Wifi_BR", "BL: wifi p2p not enabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            // that event is a result of a previous discoverPeers peers

            Log.d("Wifi_BR", "p2p peers changed");
            if (mManager != null) {
                if(!this.asapWifiP2PEngine.permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    return;
                }
                // ask for a list of new peers - send to PeerListListener
                mManager.requestPeers(mChannel, this.peerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections

            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);


            // are we connected
            if(networkInfo.isConnected()) {
                // yes - ask for connection information
                Log.d("Wifi_BR", "BL: p2p peers connection changed: connected");
                this.mManager.requestConnectionInfo(this.mChannel, this.connectionInfoListener);
            } else {
                Log.d("Wifi_BR", "BL: p2p peers connection changed: not connected");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            WifiP2pDevice device = (WifiP2pDevice)
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            // TODO do something useful with that information
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
