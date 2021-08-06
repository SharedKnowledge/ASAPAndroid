package net.sharksystem.asap.android.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class WifiPeerListListener implements WifiP2pManager.PeerListListener {
    private final WifiP2PEngine wifiP2PEngine;

    public WifiPeerListListener(WifiP2PEngine wifiP2PEngine) {
        this.wifiP2PEngine = wifiP2PEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                WifiP2pManager.PeerListListener interface support                 //
    //////////////////////////////////////////////////////////////////////////////////////

    /** list of devices which should be connected to */
    private List<WifiP2pDevice> devices2Connect = null;

    /**
     * called as result of a previous requestPeers call in wifip2pmanager
     * after receiving a ON_PEERS_CHANGED_ACTION. Note: There was a sheer
     * flood on those messages
     *
     * @param peers
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        // got a list of peers  - check it out
        Log.d(this.getLogStart(), "onPeersAvailable: peers available");

        // if our last general encounter was before our waiting period
        // we can drop the whole waiting list - any peer will be considered as
        // new - remove waiting queue

        if (this.devices2Connect == null) {
            this.devices2Connect = new ArrayList<>();
        }

        // walk trough list of available peers
        for (WifiP2pDevice device : peers.getDeviceList()) {
            boolean connect = false;
            Log.d(this.getLogStart(), "iterate new peer: " + device.deviceAddress);

            /*
            // should contact that device?
            if(this.shouldConnectToMACPeer(device.deviceAddress)) {
                Log.d(this.getLogStart(), "add device to devices2contact list");
                this.devices2Connect.add(device);
            } else {
                Log.d(this.getLogStart(), "do not contact device");
            }
             */
        }

        // are there devices to connect to?
        if (!devices2Connect.isEmpty()) {
            this.connectDevices();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectDevices() {
        if (this.devices2Connect == null || this.devices2Connect.isEmpty()) return;

        Log.d(this.getLogStart(), "encounteredDevices entered with non-empty list");

        // not null, not empty, go ahead
        for (WifiP2pDevice device : this.devices2Connect) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            Log.d(this.getLogStart(), "encounteredDevices: try address: " + device.deviceAddress);
            if(!this.wifiP2PEngine.permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) return;

            this.wifiP2PEngine.getWifiP2pManager().connect(
                    this.wifiP2PEngine.getWifiP2pChannel(),
                    config,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(getLogStart(),
                                    "wifi p2p connection request successful: "
                                        + config.deviceAddress);
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(getLogStart(),
                                    "wifi p2p connection request NOT successful: "
                                        + config.deviceAddress);
                        }
                    }
            );
        }

        // done: remove list
        this.devices2Connect = null;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

}
