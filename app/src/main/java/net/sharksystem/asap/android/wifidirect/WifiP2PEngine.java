package net.sharksystem.asap.android.wifidirect;

import android.content.Context;
import android.content.IntentFilter;
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

public class WifiP2PEngine extends MacLayerEngine implements
        WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.ChannelListener {

    private static WifiP2PEngine wifiP2PEngine = null;

    // https://developer.android.com/guide/topics/connectivity/wifip2p#java

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectBroadcastReceiver mReceiver;

    WifiP2PEngine(ASAPService ASAPService, Context context) {
        super(ASAPService, context);
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 factory / singleton                              //
    //////////////////////////////////////////////////////////////////////////////////////

    public static WifiP2PEngine getASAPWifiP2PEngine(ASAPService ASAPService, Context context) {
        if(WifiP2PEngine.wifiP2PEngine == null) {
            WifiP2PEngine.wifiP2PEngine = new WifiP2PEngine(ASAPService, context);
        }

        return WifiP2PEngine.wifiP2PEngine;
    }

    public static WifiP2PEngine getASAPWifiP2PEngine() {
        return WifiP2PEngine.wifiP2PEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 life cycle methods                               //
    //////////////////////////////////////////////////////////////////////////////////////

    /////////////// WifiP2pManager.ChannelListener - see mManager.initialize
    public void onChannelDisconnected() {
        Log.d(this.getLogStart(),"channel disconnected / restart wifip2p");
        // TODO: that's ok?
        this.restart();
    }

    private void shutdown() {
        if(this.mReceiver != null) {
            try {
                this.getContext().unregisterReceiver(this.mReceiver);
            }
            catch(RuntimeException e) {
                // ignore that one - happens when not registered
            }
            this.mReceiver = null;
        }

        if(this.mManager != null && this.mChannel != null) {
            try {
                this.mManager.cancelConnect(this.mChannel, null);
            }
            catch(RuntimeException e) {}

            try {
                this.mManager.removeGroup(this.mChannel, null);
            }
            catch(RuntimeException e) {}

            try {
                this.mManager.stopPeerDiscovery(this.mChannel, null);
            }
            catch(RuntimeException e) {}

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
        if(this.mManager == null) {
            // get P2P service on this device
            this.mManager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);

            // get access to p2p framework: TODO shouldn't we have our own looper here?
            this.mChannel = mManager.initialize(this.getContext(), getMainLooper(), this);

            // create broadcast listener to get publications regarding wifi (p2p)
            this.mReceiver = new WifiDirectBroadcastReceiver(this,
                    mManager, mChannel, this.getContext(),
                    this, this);

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
            }
            catch(RuntimeException e) {
                // can happen when already registered - to be sure..
                try {
                    this.getContext().unregisterReceiver(this.mReceiver);
                    this.getContext().registerReceiver(this.mReceiver, mIntentFilter);
                }
                catch(RuntimeException e2) {
                    // for gods sake - ignore that - have no idea whats going on here.
                }
            }
        }
    }

    public void start() {
        Log.d("AASPWifiP2PEngine", "start / start setup wifip2p");
        this.setup();
    }

    public void stop() {
        Log.d("AASPWifiP2PEngine", "stop / shutdown wifip2p");
        this.shutdown();
    }

    /**
     * called to start peer discovery
     */
    void discoverPeers() {
        mManager.discoverPeers(this.mChannel,
            new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // TODO remove that after debugging
                    Log.d(getLogStart(), "discoverPeers: peer discovery started");
                }

                @Override
                public void onFailure(int reasonCode) {
                    // TODO remove that after debugging
                    Log.d(getLogStart(), "discoverPeers: peer discovery failed");
                }
            });
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

        if(this.devices2Connect == null) {
            this.devices2Connect = new ArrayList<>();
        }

        // walk trough list of available peers
        for(WifiP2pDevice device : peers.getDeviceList()) {
            boolean connect = false;
            Log.d(this.getLogStart(), "iterate new peer: " + device.deviceAddress);

            // should contact that device?
            if(this.shouldConnectToMACPeer(device.deviceAddress)) {
                Log.d(this.getLogStart(), "add device to devices2contact list");
                this.devices2Connect.add(device);
            } else {
                Log.d(this.getLogStart(), "do not contact device");
            }
        }

        // are there devices to connect to?
        if(!devices2Connect.isEmpty()) {
            this.connectDevices();
        }
    }

    private void connectDevices() {
        if(this.devices2Connect == null || this.devices2Connect.isEmpty()) return;

        Log.d(this.getLogStart(), "encounteredDevices entered with non-empty list");

        // not null, not empty, go ahead
        for(WifiP2pDevice device : this.devices2Connect) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            Log.d(this.getLogStart(), "encounteredDevices: try address: " + device.deviceAddress);
            this.mManager.connect(this.mChannel, config,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(getLogStart(),"encounteredDevices: connect called successfully");
                    }

                    @Override
                    public void onFailure(int reason) {
                        //failure logic
                        Log.d(getLogStart(),"encounteredDevices: connect called not successfully");
                    }
                }
            ); // end connect
        }

        // done: remove list
        this.devices2Connect = null;
    }
    
    private String getLogStart() {
        return "ASAPWifiP2PEngine";
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

        // create an ASAPSession with connection parameters
        ASAPConnectionLauncher ASAPConnectionLauncher = new ASAPConnectionLauncher(channelCreator,
                this.getAsapService().getMultiASAPEngine());

        ASAPConnectionLauncher.start();
    }
}
