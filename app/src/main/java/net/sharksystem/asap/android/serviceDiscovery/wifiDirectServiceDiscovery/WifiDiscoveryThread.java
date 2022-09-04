package net.sharksystem.asap.android.serviceDiscovery.wifiDirectServiceDiscovery;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Discovers nearby services periodically as long as {@link #retries} are
 * left.
 * <p>
 * Retries<br>
 * ------------------------------------------------------------<br>
 * Retries are per default set to two, which increases the chance
 * of discovering a service (as my finding are).
 * A different amount of retries amy be set using the constructor
 * {@link #WifiDiscoveryThread(WifiP2pManager, WifiP2pManager.Channel, WifiDirectDiscoveryEngine, int)}
 * or {@link #setTries(int)}
 *
 * As longs as there are retries left discovery will be restarted with
 * a 7 second gap in between, to ensure the discovery of nearby services.
 * <p>
 * Whats returned?<br>
 * ------------------------------------------------------------<br>
 * There will be no filter applied to discovered services, this
 * thread does not care what it discovers or how often, depending
 * on the amount of retries set -s service can be discovered several times.
 *
 * <p>
 * SdpWifiDiscoveryEngine<br>
 * ------------------------------------------------------------<br>
 * Every discovered service will be passed to
 * {@link WifiDirectDiscoveryEngine#onServiceDiscovered(WifiP2pDevice, Map, String, String)}
 * only there the service records will be evaluated.
 *
 * @author WilliBoelke
 */
@SuppressLint("MissingPermission")
class WifiDiscoveryThread extends Thread
{
    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();

    private final int WAIT_BEFORE_RETRY = 10000;

    private final int TRIES = 2;

    private int retries;

    private int runningTries = 0;

    private final WifiDirectDiscoveryEngine engine;

    private boolean isDiscovering;

    private final WifiP2pManager manager;

    private final WifiP2pManager.Channel channel;

    private Thread thread;

    private final HashMap<String, Map<String, String>> tmpRecordCache = new HashMap<>();
    //
    //  ---------- constructor and initialisation ----------
    //

    /**
     * Constructor
     *
     * @param manager
     * The WifiP2P manager
     * @param channel
     *  The Channel
     * @param engine
     * The WifiDirectDiscoveryEngine to callback
     */
    public WifiDiscoveryThread(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectDiscoveryEngine engine)
    {
        this.manager = manager;
        this.channel = channel;
        this.engine = engine;
        this.retries = TRIES;
    }

    /**
     * Constructor
     *
     * @param manager
     * The WifiP2P manager
     * @param channel
     *  The Channel
     * @param engine
     * The WifiDirectDiscoveryEngine to callback
     * @param retries
     * number of retries
     */
    public WifiDiscoveryThread(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectDiscoveryEngine engine, int retries)
    {
        this.manager = manager;
        this.channel = channel;
        this.engine = engine;
        this.retries = retries;
    }


    //
    //  ----------  discovery ----------
    //


    @Override
    public void run()
    {
        Log.d(TAG, "run: starting discovery thread");
        //--- setting to running ---//

        isDiscovering = true;
        this.thread = currentThread();

        //--- setting up callbacks ---//

        setupDiscoveryCallbacks();

        //--- discovery loop ---//

        while (isDiscovering && runningTries < retries)
        {
            try
            {
                runningTries++;
                startDiscovery();
                // give it some time
                synchronized (this)
                {
                    this.wait(WAIT_BEFORE_RETRY);
                }
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "run:discovery thread was interrupted (maybe cancelled)");
            }
        }

        //--- end ---//

        engine.onDiscoveryFinished();
        this.cancel();
        Log.d(TAG, "run: discovery thread finished");
    }

    //
    //  ---------- discovery ----------
    //

    /**
     * Setting up the callbacks which wil be called when
     * a service was discovered, proving the TXT records and other
     * service information.
     *
     * This only needs to be set up once, at the Thread start.
     * It shouldn't called while lopping (re-starting service discovery).
     *
     */
    private void setupDiscoveryCallbacks()
    {
        tmpRecordCache.clear();
        Log.d(TAG, "setupDiscoveryCallbacks: setting up callbacks");

        //----------------------------------
        // NOTE : Well its a little bit weird i think
        // that the TXT record and the DnsService response
        // come in separate callbacks.
        // It seems so that the always come in teh same order
        // To i can match both using the device address.
        // https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct
        // does it similar, though in their example
        // there just is one single service.
        // I am sure there is some reason for it.. which i
        // cant quite understand. I think both should come in teh same callback
        // because i am sure google could make a more reliable matching between the two
        // then i can do here.
        //
        // Lets test it out
        //----------------------------------

        //--- TXT Record listener ---//

        WifiP2pManager.DnsSdTxtRecordListener txtListener = (fullDomain, txtRecord, device) ->
        {
            Log.d(TAG, "run: found service record: on  " + device + " record: " + txtRecord);

            tmpRecordCache.put(device.deviceAddress, txtRecord);
        };

        //--- Service response listener - gives additional service info ---//

        WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, device) ->
        {
            Map<String, String> record = tmpRecordCache.get(device.deviceAddress);
            engine.onServiceDiscovered(device, record, registrationType, instanceName);

        };

        //--- setting the listeners ---//

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
    }

    private void startDiscovery()
    {
        Log.d(TAG, "startDiscovery: started discovery");

        //--- clearing already running service requests ---//
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener()
        {
            @Override
            public void onSuccess()
            {
                Log.d(TAG, "Cleared local service requests");

                //--- adding service requests (again) ---//

                //----------------------------------
                // NOTE : Bonjour services are used,
                // so WifiP2pDnsSdServiceRequests are
                // used here.
                //----------------------------------
                manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(), new WifiP2pManager.ActionListener()
                {
                    @Override
                    public void onSuccess()
                    {
                        //--- starting the service discovery (again) ---//
                        manager.discoverServices(channel, new WifiP2pManager.ActionListener()
                        {
                            @Override
                            public void onSuccess()
                            {
                                Log.d(TAG, "Started service discovery");
                            }

                            @Override
                            public void onFailure(int code)
                            {
                                Log.d(TAG, "failed to start service discovery");
                                onServiceDiscoveryFailure();
                            }
                        });
                    }

                    @Override
                    public void onFailure(int code)
                    {
                        Log.d(TAG, "failed to add service discovery request");
                        onServiceDiscoveryFailure();
                    }
                });
            }
            @Override
            public void onFailure(int code)
            {
                Log.d(TAG, "Failed to clear local service requests");
                onServiceDiscoveryFailure();
            }
        });
    }


    //
    //  ---------- others ----------
    //

    protected void cancel(){
        Log.d(TAG, "cancel: canceling service discovery");
        this.thread.interrupt();
        this.isDiscovering = false;
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener()
        {
            @Override
            public void onSuccess()
            {
                // nothing to do here
            }

            @Override
            public void onFailure(int reason)
            {
                WifiDirectDiscoveryEngine.logReason(TAG,"DiscoveryThread: cancel: could not clear service requests ", reason);
            }
        });
        Log.d(TAG, "cancel: canceled service discovery");
    }

    protected boolean isDiscovering(){
        return this.isDiscovering;
    }

    private void onServiceDiscoveryFailure(){
        //----------------------------------
        // NOTE : There doesn't seem to be
        // much i can do here, wifi could be restarted
        // (of / on) but that's all
        //----------------------------------
    }

    /**
     * Sets the amount retries for the service discovery
     * the default is 1 try.
     * A higher value will restart the discovery every 10 seconds
     * as many times as specified trough the tries
     *
     * @param tries
     * the number of tries
     */
    protected void setTries(int tries){
        this.retries = tries;
    }
}