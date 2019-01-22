package net.sharksystem.aasp.android.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import net.sharksystem.aasp.android.AASPService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Looper.getMainLooper;

public class AASPWifiP2PEngine implements WifiP2pManager.PeerListListener {
    private static AASPWifiP2PEngine wifiP2PEngine = null;

    // https://developer.android.com/guide/topics/connectivity/wifip2p#java

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private AASPWiFiDirectBroadcastReceiver mReceiver;

    private final AASPService aaspService;
    private final Context context;


    AASPWifiP2PEngine(AASPService aaspService, Context context) {
        this.aaspService = aaspService;
        this.context = context;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 factory / singleton                              //
    //////////////////////////////////////////////////////////////////////////////////////

    public static AASPWifiP2PEngine getAASPWifiP2PEngine(AASPService aaspService, Context context) {
        if(AASPWifiP2PEngine.wifiP2PEngine == null) {
            AASPWifiP2PEngine.wifiP2PEngine = new AASPWifiP2PEngine(aaspService, context);
        }

        return AASPWifiP2PEngine.wifiP2PEngine;
    }

    public static AASPWifiP2PEngine getAASPWifiP2PEngine() {
        return AASPWifiP2PEngine.wifiP2PEngine;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                                 live cycle methods                               //
    //////////////////////////////////////////////////////////////////////////////////////

    public void start() {
        // get P2P service on this device
        this.mManager = (WifiP2pManager) this.context.getSystemService(Context.WIFI_P2P_SERVICE);

        // get access to p2p framework
        this.mChannel = mManager.initialize(this.context, getMainLooper(), null);

        // create broadcast listener to get publications regarding wifi (p2p)
        this.mReceiver = new AASPWiFiDirectBroadcastReceiver(mManager,
                mChannel, this.context, this);

        // define what broadcasts we are interested in
        IntentFilter mIntentFilter = new IntentFilter();

        // see broadcast receiver for details of those events
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // register (subscribe) to broadcasts
        this.context.registerReceiver(this.mReceiver, mIntentFilter);
    }

    public void stop() {
        // stop / remove channel in some way: TODO

        // remove broadcast listener
        this.context.unregisterReceiver(this.mReceiver);
    }

    void discover() {
        mManager.discoverPeers(this.mChannel,
            new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // TODO remove that after debugging
                    Toast.makeText(context, "peer discovery start", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    // TODO remove that after debugging
                    Toast.makeText(context, "peer discovery failed", Toast.LENGTH_SHORT).show();
                }
            });
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                WifiP2pManager.PeerListListener interface support                 //
    //////////////////////////////////////////////////////////////////////////////////////

    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000*60; // a minute
    private long waitBeforeReconnect = DEFAULT_WAIT_BEFORE_RECONNECT_TIME;

    /** last encounter with new peers - last time pnPeersAvaiable called */
    private Date lastEncounter = new Date();

    /** keeps info abbout device recently met (mac address, last encounter*/
    private Map<String, Date> knownDevices = new HashMap<>();

    /** list of devices which should be connected to */
    private List<WifiP2pDevice> devices2Connect = null;

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        // got a list of peers  - check it out
        Toast.makeText(context, "peers available", Toast.LENGTH_SHORT).show();

        // get current time, in its incarnation as date
        long nowInMillis = System.currentTimeMillis();
        long reconnectedBeforeInMillis = nowInMillis - this.waitBeforeReconnect;

        Date now = new Date(nowInMillis);
        Date reconnectBefore = new Date(reconnectedBeforeInMillis);

        /* if we last encountered other peers *before* the reconnect
        waiting periode remove whole knownDevices list - any peer should be
        tried to reconnected.
         */
        if(this.lastEncounter.before(reconnectBefore)) {
            this.knownDevices = new HashMap<>();
        }

        if(this.devices2Connect == null) {
            this.devices2Connect = new ArrayList<>();
        }

        // walk trough list of available peers
        for(WifiP2pDevice device : peers.getDeviceList()) {
            boolean connect = false;

            // device in known device list?
            Date lastEncounter = this.knownDevices.get(device.deviceAddress);

            if(lastEncounter != null) {
                // it was in the list
                if(lastEncounter.before(reconnectBefore)) {
                    // that encounter longer ago than waiting periode - remember that peer
                    devices2Connect.add(device);
                }
            } else {
                // no entry at all - remember that device
                devices2Connect.add(device);
            }
        }

        // remember that encounter
        this.lastEncounter = now;

        // are there devices to connect to?
        if(!devices2Connect.isEmpty()) {
            this.connectDevices();
        }
    }

    private void connectDevices() {
        if(this.devices2Connect == null || this.devices2Connect.isEmpty()) return;

        // not null, not empty, go ahead
        for(WifiP2pDevice device : this.devices2Connect) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            this.mManager.connect(this.mChannel, config,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            //success logic TODO: maybe remove that stuff
                        }

                        @Override
                        public void onFailure(int reason) {
                            //failure logic
                        }
                    }
            ); // end connect
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                        Broadcast receiver for wifi support                       //
    //////////////////////////////////////////////////////////////////////////////////////

    private class AASPWiFiDirectBroadcastReceiver extends BroadcastReceiver {
        // https://developer.android.com/guide/topics/connectivity/wifip2p#java

        private final WifiP2pManager mManager;
        private final WifiP2pManager.Channel mChannel;
        private final Context context;
        private final WifiP2pManager.PeerListListener peerListListener;

        public AASPWiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                               WifiP2pManager.Channel channel,
                                               Context context,
                                               WifiP2pManager.PeerListListener peerListListener
                                                ) {
            super();
            this.mManager = manager;
            this.mChannel = channel;
            this.context = context;
            this.peerListListener = peerListListener;
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
                    Toast.makeText(this.context, "wifi p2p enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // Wi-Fi P2P is not enabled
                    Toast.makeText(this.context, "wifi p2p not enabled", Toast.LENGTH_SHORT).show();
                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                // that event is a result of a previous discover peers

                Toast.makeText(this.context, "p2p peers changed", Toast.LENGTH_SHORT).show();

                // call for a list of peers
                if (mManager != null) {
                    mManager.requestPeers(mChannel, this.peerListListener);
                }


            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            }
        }
    }
}
