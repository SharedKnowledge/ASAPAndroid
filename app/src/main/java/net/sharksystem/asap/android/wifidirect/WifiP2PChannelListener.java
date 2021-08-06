package net.sharksystem.asap.android.wifidirect;

import android.net.wifi.p2p.WifiP2pManager;

public class WifiP2PChannelListener implements WifiP2pManager.ChannelListener {
    private final WifiP2PEngine wifiP2PEngine;

    public WifiP2PChannelListener(WifiP2PEngine wifiP2PEngine) {
        this.wifiP2PEngine = wifiP2PEngine;
    }

    @Override
    public void onChannelDisconnected() {

    }
}
