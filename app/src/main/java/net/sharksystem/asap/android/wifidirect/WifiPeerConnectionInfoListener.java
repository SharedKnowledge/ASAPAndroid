package net.sharksystem.asap.android.wifidirect;

import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.service.ASAPConnectionLauncher;
import net.sharksystem.util.tcp.TCPChannelMaker;

public class WifiPeerConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {
    private final WifiP2PEngine wifiP2PEngine;

    public WifiPeerConnectionInfoListener(WifiP2PEngine wifiP2PEngine) {
        this.wifiP2PEngine = wifiP2PEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                  ConnectionInfoListener interface support                        //
    //////////////////////////////////////////////////////////////////////////////////////

    TCPChannelMaker serverChannelCreator = null;

    /**
     * result of a requestConnectionInfo on wifip2pmanager after receiving a p2p connection
     * changed event via broadcast listener
     *
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(getLogStart(), "onConnectionInfoAvailable");

        TCPChannelMaker.max_connection_loops = 10;

        TCPChannelMaker channelCreator = null;
        if(info.isGroupOwner) {
            Log.d(getLogStart(), "group owner - should create server");

            // create tcp server as group owner
            if(this.serverChannelCreator == null) {
                Log.d(getLogStart(), "start server channel maker");
                this.serverChannelCreator =
                        TCPChannelMaker.getTCPServerCreator(ASAPAndroid.PORT_NUMBER, true);
            } else {
                Log.d(getLogStart(), "server channel maker already exists");
            }

            channelCreator = this.serverChannelCreator;

        } else {
            String hostAddress = info.groupOwnerAddress.getHostAddress();
            Log.d(getLogStart(), " start server channel maker: " + hostAddress);
            // create client connection to group owner
            channelCreator = TCPChannelMaker.getTCPClientCreator(hostAddress, ASAPAndroid.PORT_NUMBER);
        }

        // TODO - route this over encounter manager
        // create an ASAPSession with connection parameters
        ASAPConnectionLauncher ASAPConnectionLauncher = new ASAPConnectionLauncher(channelCreator,
                this.wifiP2PEngine.getASAPService().getASAPPeer());

        ASAPConnectionLauncher.start();
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
