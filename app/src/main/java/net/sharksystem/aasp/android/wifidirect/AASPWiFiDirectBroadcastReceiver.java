package net.sharksystem.aasp.android.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import static android.net.wifi.p2p.WifiP2pManager.EXTRA_P2P_DEVICE_LIST;

class AASPWiFiDirectBroadcastReceiver extends BroadcastReceiver {
    // https://developer.android.com/guide/topics/connectivity/wifip2p#java

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private final Context context;
    private final WifiP2pManager.PeerListListener peerListListener;
    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    private final AASPWifiP2PEngine aaspWifiP2PEngine;

    public AASPWiFiDirectBroadcastReceiver(
            AASPWifiP2PEngine aaspWifiP2PEngine,
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            Context context,
            WifiP2pManager.PeerListListener peerListListener,
            WifiP2pManager.ConnectionInfoListener connectionInfoListener
    ) {
        super();
        this.aaspWifiP2PEngine = aaspWifiP2PEngine;
        this.mManager = manager;
        this.mChannel = channel;
        this.context = context;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = connectionInfoListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // https://developer.android.com/guide/topics/connectivity/wifip2p#java

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d("AASP_BR", "BL: wifi p2p enabled");

                // discoverPeers peers
                this.aaspWifiP2PEngine.discoverPeers();
            } else {
                // Wi-Fi P2P is not enabled
                Log.d("AASP_BR","BL: wifi p2p not enabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            // that event is a result of a previous discoverPeers peers

            Log.d("AASP_BR", "p2p peers changed");
            // call for a list of peers
            if (mManager != null) {
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
                Log.d("AASP_BR", "BL: p2p peers connection changed: connected");
                this.mManager.requestConnectionInfo(this.mChannel, this.connectionInfoListener);
            } else {
                Log.d("AASP_BR", "BL: p2p peers connection changed: not connected");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            WifiP2pDevice device = (WifiP2pDevice)
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            // TODO do something useful with that information
        }
    }
}
